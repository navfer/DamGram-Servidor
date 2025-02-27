package com.navfer.Repository

import com.navfer.clases.User
import org.bson.types.ObjectId

interface UserRepository {
    suspend fun getAllUsers(): List<User>
    suspend fun getUserById(id: ObjectId): User?
    suspend fun getUserByUsername(username: String): User?
    suspend fun createUser(user: User): Boolean
}