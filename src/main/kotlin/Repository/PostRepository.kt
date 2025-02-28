package com.navfer.Repository

import com.navfer.clases.Comentario
import com.navfer.clases.CommentSerializable
import com.navfer.clases.Post
import org.bson.types.ObjectId

interface PostRepository {
    suspend fun getAllPosts(): List<Post>
    suspend fun getPostById(id: ObjectId): Post?
    suspend fun getPostsByUser(userId: ObjectId): List<Post>
    suspend fun createPost(post: Post): String?
    suspend fun addComment(postId: ObjectId, comment: Comentario): Boolean
    suspend fun addLike(postId: ObjectId, userId: ObjectId): Boolean
}
