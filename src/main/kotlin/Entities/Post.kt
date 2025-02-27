package com.navfer.clases

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Post (
    @BsonId val id: ObjectId,
    val userId: ObjectId,
    val image: String,
    val public: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val comments: List<Comment> = emptyList(),
    val likes: List<Like> = emptyList()
)

data class Comment (
    val userId: ObjectId,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Like (
    val userId: ObjectId,
)


//CLASES SERIALIZABLES

@Serializable
data class PostSerializable (
    val id: String,
    val userId: String,
    val image: String,
    val public: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val comments: List<CommentSerializable> = emptyList(),
    val likes: List<LikeSerializable> = emptyList()
)

@Serializable
data class CommentSerializable (
    val userId: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class LikeSerializable (
    val userId: String,
)
