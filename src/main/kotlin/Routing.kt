package com.navfer

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
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.bson.types.ObjectId

fun Application.configureRouting() {

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

        /**
         * Registra nuevo usuario
         */
        post("/user") {
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


                val nuevoUsuario = User(
                    id = ObjectId(),  // Generar un nuevo ID
                    username = usuarioJson.username,
                    password = usuarioJson.password,
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
                        public = it.public,
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
                        public = post.public,
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
            val posts = mp.getPostsByUser(objectId)
            if (posts != null) {
                posts.map{
                    PostSerializable(
                        id = it.id.toString(),
                        userId = it.userId.toString(),
                        image = it.image,
                        public = it.public,
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
            }else{
                call.respond(HttpStatusCode.NotFound, "No encontrado.")
            }
        }

        /**
         * Crea un nuevo Post
         */
        post("/posts"){
            try{
                val postJson = call.receive<PostSerializable>()

                val nuevo = Post(
                    id = ObjectId(),
                    userId = ObjectId(postJson.userId),
                    image = postJson.image,
                    public = postJson.public,
                    timestamp = System.currentTimeMillis(),
                    comments = emptyList(),
                    likes = emptyList()
                )

                val resultado = mp.createPost(nuevo)
                if(resultado){
                    call.respond(HttpStatusCode.OK, "Post Creado correctamente.")
                }else{
                    call.respond(HttpStatusCode.InternalServerError, "No se pudo crear el post.")
                }
            }catch(e:Exception){
                call.respond(HttpStatusCode.BadRequest, "Error: ${e.message}")
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
                val comentarioJson = call.receive<CommentSerializable>()
                val comentario = Comment(
                  userId = ObjectId(userId),
                    text = comentarioJson.text,
                    timestamp = System.currentTimeMillis()
                )

                val resultado = mp.addComment(ObjectId(postId), comentario)

                if(resultado){
                    call.respond(HttpStatusCode.OK, "Comentario Creado correctamente.")
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
    }

}
