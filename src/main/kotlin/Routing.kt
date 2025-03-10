package com.navfer

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.navfer.Mongo.MongoPost
import com.navfer.Mongo.MongoUser
import com.navfer.clases.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.bson.types.ObjectId
import java.util.*

fun Application.configureRouting() {
    //JWT
    val myRealm = environment.config.property("jwt.realm").getString()
    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()

    install(Authentication) {
        jwt("jwt-auth") {
            realm = myRealm

            // Verifica que el token sea un token válido así como la signatura
            verifier(JWT
                .require(Algorithm.HMAC256(secret))
                .withAudience(audience)
                .withIssuer(issuer)
                .build())

            // Valida el payload
            validate { credential ->
                if (credential.payload.getClaim("username").asString().isNotEmpty()) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            // Configura una respuesta cuando la autenticación falle
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }



    //Mongo DB
    val pojoCodecRegistry = fromRegistries(
        MongoClientSettings.getDefaultCodecRegistry(),
        fromProviders(PojoCodecProvider.builder().automatic(true).build())
    )
    val mongoConnString = "mongodb://localhost:27017"
    val connectionString = ConnectionString(mongoConnString)

    val settings = MongoClientSettings.builder()
        .applyConnectionString(connectionString)
        .codecRegistry(pojoCodecRegistry)
        .build()

    val client = MongoClient.create(settings)
    val database = client.getDatabase("DamGram")

    val mu= MongoUser(database)
    val mp = MongoPost(database)

    routing {

        /**
         * Devuelve la lista de todos los usuarios de la base de datos
         */
        get("/") {
            try {
                //val users = mu.getAllUsers()
                val users = mu.getAllUsers().map {
                    UserSerializable(it.id.toString(), it.username, it.password, it.avatar)
                }
                call.respond(HttpStatusCode.OK, users)
            } catch (e: Exception) {
               call.respond(HttpStatusCode.InternalServerError, "Error al obtener usuarios: ${e.message}")
            }
        }

        /**
         * Devuelve el usuario con la ID dada.
         */
        get("/users/{id}"){
            try{
                //Coge el parámetro y lo transforma a ObjectId
                val id = call.parameters["id"]
                val objectId = ObjectId(id)

                val user = mu.getUserById(objectId)
                if (user != null){
                    val userJson = UserSerializable(objectId.toString(), user.username, user.password, user.avatar)
                    call.respond(HttpStatusCode.OK, userJson)
                }else{
                    call.respond(HttpStatusCode.NotFound, "No se ha encontrado el usuario")
                }

            }catch(e:Exception){
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }

        get("/users/username/{username}"){
            val username = call.parameters["username"]
            if (username.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Username is required")
                return@get
            }

            val user = mu.getUserByUsername(username)
            if (user != null) {
                val usuarioJson = UserSerializable(
                    id = user.id.toString(),
                    username = user.username,
                    password = user.password,
                    avatar = user.avatar
                )
                call.respond(HttpStatusCode.OK, usuarioJson)
            } else {
                call.respond(HttpStatusCode.NotFound, "User not found")
            }
        }

        /**
         * Obtiene usuario por su nombre de usuario
         */
        get("/users/username/{username}"){
            try{
                //Coge el parámetro y lo transforma a ObjectId
                val username = call.parameters["username"].toString()
                val user = mu.getUserByUsername(username)
                if (user != null){
                    //println("Usuario encontrado: id=${user.id}, username=${user.username}")
                    val userJson = UserSerializable(
                        id = user.id.toHexString(),
                        username = user.username,
                        password = user.password,
                        avatar = user.avatar
                    )
                    call.respond(HttpStatusCode.OK, userJson)
                }else{
                    call.respond(HttpStatusCode.NotFound, "No se ha encontrado el usuario")
                }

            }catch(e:Exception){
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }


        /**
         * Registra nuevo usuario
         */
        post("/users") {
            try {

                val usuarioJson = call.receive<UserSerializable>()

                //valida usuario
                val usuarioExistente = mu.getUserByUsername(usuarioJson.username)
                if (usuarioExistente != null) {
                    call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("message" to "El usuario ya existe")
                    )
                    return@post
                }

                //Encriptamos la contraseña del usuario para guardarla en la base de datos
                val bcryptHashPassword = BCrypt.withDefaults().hashToString(12, usuarioJson.password.toCharArray());
                val nuevoUsuario = User(
                    id = ObjectId(),  // Generar un nuevo ID
                    username = usuarioJson.username,
                    password = bcryptHashPassword,
                    avatar = usuarioJson.avatar
                )

                //insertar base de datos
                val resultado = mu.createUser(nuevoUsuario)
                if (resultado) {
                    call.respond(
                        HttpStatusCode.Created,
                        mapOf("message" to "Usuario creado exitosamente", "id" to nuevoUsuario.id.toString())
                    )
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("message" to "No se pudo crear el usuario")
                    )
                }
            } catch (e: IllegalStateException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Error en la solicitud: ${e.message}")
                )
            } catch (e: JsonConvertException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Error en el formato JSON: ${e.message}")
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("message" to "Error desconocido: ${e.message}")
                )
            }
        }

        /**
         * Devuelve lista de todos los post
         */
        get("/posts"){
            try{
                val posts = mp.getAllPosts().map {
                    PostSerializable(
                        id = it.id.toString(),
                        userId = it.userId.toString(),
                        image = it.image,
                        info = it.info,
                        timestamp = it.timestamp,
                        comments = it.comments.map { comment ->
                            CommentSerializable(
                                userId = comment.userId.toString(),
                                text = comment.text,
                                timestamp = comment.timestamp
                            )
                        },
                        likes = it.likes.map { like ->
                            LikeSerializable(userId = like.userId.toString())
                        }
                    )
                }

                call.respond(HttpStatusCode.OK, posts)

            }catch (e:Exception){
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }

        /**
         * Devuelve un post por su ID.
         */
        get("/posts/{id}"){
            try{
                val id = call.parameters["id"]
                val objectId = ObjectId(id)
                val post = mp.getPostById(objectId)
                if (post != null) {
                    //Serializamos el post para mostrarlo
                    val postJson = PostSerializable(
                        id = post.id.toString(),
                        userId = post.userId.toString(),
                        image = post.image,
                        info = post.info,
                        timestamp = post.timestamp,
                        comments = post.comments.map { comment ->
                            CommentSerializable(
                                userId = comment.userId.toString(),
                                text = comment.text,
                                timestamp = comment.timestamp
                            )
                        },
                        likes = post.likes.map { like ->
                            LikeSerializable(userId = like.userId.toString())
                        }
                    )
                    call.respond(HttpStatusCode.OK, postJson)
                }else{
                    call.respond(HttpStatusCode.NotFound, "No se ha encontrado el post.")
                }
            }catch(e:Exception){
                call.respond(HttpStatusCode.BadRequest, "Error: ${e.message}")
            }
        }

        /**
         * Devuelve la lista de todos los post de un usuario pasando la ID de este.
         */

        get("/posts/user/{id}"){
            val id = call.parameters["id"]
            val objectId = ObjectId(id)
            val posts: List<Post> = mp.getPostsByUser(objectId)
            if (posts != null) {
                val postJson = posts.map{
                    PostSerializable(
                        id = it.id.toString(),
                        userId = it.userId.toString(),
                        image = it.image,
                        info = it.info,
                        timestamp = it.timestamp,
                        comments = it.comments.map { comment ->
                            CommentSerializable(
                                userId = comment.userId.toString(),
                                text = comment.text,
                                timestamp = comment.timestamp
                            )
                        },
                        likes = it.likes.map { like ->
                            LikeSerializable(userId = like.userId.toString())
                        }
                    )
                }

                call.respond(HttpStatusCode.OK, postJson)
            }else{
                call.respond(HttpStatusCode.NotFound, "No encontrado.")
            }
        }

        /**
         * Crea un nuevo Post
         */
        post("/posts"){
                val postJson = call.receive<PostSerializable>()

                val nuevo = Post(
                    id = ObjectId(),  // Se genera un nuevo ID
                    userId = ObjectId(postJson.userId),
                    image = postJson.image,
                    info = postJson.info,
                    timestamp = postJson.timestamp,
                    comments = emptyList(),
                    likes = emptyList()
                )

                val resultado = mp.createPost(nuevo)
                if(resultado != null){
                    call.respond(HttpStatusCode.Created, "Post Creado correctamente.")
                }else{
                    call.respond(HttpStatusCode.InternalServerError, "No se pudo crear el post.")
                }

        }


        /**
         * Añade comentario al un Post (PostID) por parte de un usuario (UserID)
         */
        post("/posts/{postId}/comment/{userId}"){
            try{
                //Cogemos los parámetros dados
                val postId = call.parameters["postId"]
                val userId = call.parameters["userId"]

                //Recibimos el objeto comentario
                val comentarioJson = call.receive<String>()
                val comentario = Comentario(
                    userId = ObjectId(userId),
                    text = comentarioJson,
                    timestamp = System.currentTimeMillis()
                )
                val resultado = mp.addComment(ObjectId(postId),comentario)

                if(resultado){
                    call.respond(HttpStatusCode.OK, "Comentario añadido.")
                }else{
                    call.respond(HttpStatusCode.NotFound, "Post no encontrado")
                }
            }catch(e:Exception){
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }

        /**
         * Añade un “me gusta” al post <postId> por parte del usuario <userId>
         */
        post("/posts/{postId}/like/{userId}"){
            try{
                val postId = call.parameters["postId"]
                val userId = call.parameters["userId"]

                val resultado = mp.addLike(ObjectId(postId),ObjectId(userId))
                if(resultado){
                    call.respond(HttpStatusCode.OK, "Like correcto.")
                }else{
                    call.respond(HttpStatusCode.NotFound, "No encontrado.")
                }
            }catch(e:Exception){
                call.respond(HttpStatusCode.BadRequest, "Error: ${e.message}")
            }
        }

        @Serializable
        data class LoginRequest(val username: String, val password: String)
        /**
         * Permite al usuario registrado iniciar sesión
         */
        post("/auth/login"){
            try {
                //Credenciales enviado por el cliente
                val credentials = call.receive<LoginRequest>()
                val user = mu.getUserByUsername(credentials.username)

                //si no es nulo verificamos que la contraseña sea la misma que la encriptada de la base de datos
                if (user != null) {
                    val result = BCrypt.verifyer().verify(credentials.password.toCharArray(), user.password)
                    if(result.verified){
                        val token = JWT.create()
                            .withAudience(audience)
                            .withIssuer(issuer)
                            .withClaim("username", user.username)
                            .withExpiresAt(Date(System.currentTimeMillis() + 60000*60*24))
                            .sign(Algorithm.HMAC256(secret))

                        call.respond(mapOf("token" to token))
                        //call.respond(HttpStatusCode.OK)
                    }else{
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Credenciales incorrectas"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Error: ${e.message}")
            }
        }
    }

}
