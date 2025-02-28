package com.navfer.clases

import kotlinx.serialization.Serializable
import org.bson.BsonType
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonRepresentation
import org.bson.types.ObjectId


data class Post (
    @BsonId val id: ObjectId,
    val userId: ObjectId,
    val image: String? = null,
    val info: String,
    val timestamp: Long = System.currentTimeMillis(),
    val comments: List<CommentSerializable>,
    val likes: List<LikeSerializable>
)



//CLASES SERIALIZABLES

@Serializable
data class PostSerializable (
    val id: String? = null,
    val userId: String,
    val image: String? = null,
    val info: String,
    val timestamp: Long = System.currentTimeMillis(),
    val comments: List<CommentSerializable>,
    val likes: List<LikeSerializable>
)

@Serializable
data class CommentSerializable (
    @BsonRepresentation(BsonType.OBJECT_ID)
    val userId: String?,
    val text: String?,
    val timestamp: Long = System.currentTimeMillis()
)

data class Comentario(
    val userId: ObjectId,
    val text: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class LikeSerializable (
    @BsonRepresentation(BsonType.OBJECT_ID)
    val userId: String?,
)