package com.navfer.clases

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class User(
    @BsonId val id: ObjectId,   //_id
    val username: String,
    val password: String,
    val avatar: String?
)

@Serializable
data class UserSerializable(
    val id: String = ObjectId().toString(),
    val username: String,
    val password: String,
    val avatar: String?
)
