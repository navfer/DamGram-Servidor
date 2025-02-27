package com.navfer.Repository

import com.navfer.clases.Comment
import com.navfer.clases.Post
import org.bson.types.ObjectId

interface PostRepository {
    suspend fun getAllPosts(): List<Post>
    suspend fun getPostById(id: ObjectId): Post?
    suspend fun getPostsByUser(userId: ObjectId): List<Post>
    suspend fun createPost(post: Post): Boolean
    suspend fun addComment(postId: ObjectId, comment: Comment): Boolean
    suspend fun addLike(postId: ObjectId, userId: ObjectId): Boolean
}
