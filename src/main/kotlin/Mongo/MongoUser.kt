package com.navfer.Mongo

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.navfer.Repository.UserRepository
import com.navfer.clases.User
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

class MongoUser(private val db: MongoDatabase): UserRepository {

    //Obtiene a todos los usuarios de la base de datos
    private val users = db.getCollection<User>("users")

    override suspend fun getAllUsers(): List<User> {
        return users.find().toList()
    }

    override suspend fun getUserById(id: ObjectId): User? {
        return users.find(Filters.eq("_id", id)).firstOrNull()
    }

    override suspend fun getUserByUsername(username: String): User? {
        return users.find(Filters.eq("username", username)).firstOrNull()
    }

    override suspend fun createUser(user: User): Boolean {
        val result = users.insertOne(user)
        return result.wasAcknowledged() //.wasAcknowledged() devuelve true o false de la insercion
    }
}
