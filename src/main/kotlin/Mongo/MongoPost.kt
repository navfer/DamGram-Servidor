package com.navfer.Mongo

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.navfer.Repository.PostRepository
import com.navfer.clases.Comment
import com.navfer.clases.Like
import com.navfer.clases.Post
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

class MongoPost(private val db: MongoDatabase): PostRepository {

    //obtiene los post de la base de datos
    private val posts = db.getCollection<Post>("post")

    override suspend fun getAllPosts(): List<Post> {
        return posts.find().toList()
    }

    override suspend fun getPostById(id: ObjectId): Post? {
        return posts.find(Filters.eq("_id", id)).firstOrNull()
    }

    override suspend fun getPostsByUser(userId: ObjectId): List<Post> {
        return posts.find(Filters.eq("userId", userId)).toList()
    }

    override suspend fun createPost(post: Post): Boolean {
        val result = posts.insertOne(post)
        return  result.wasAcknowledged()
    }

    override suspend fun addComment(postId: ObjectId, comment: Comment): Boolean {
         try {
            val result = posts.updateOne(
                Filters.eq("_id", postId),  //Busca el post con esa ID
                Updates.push("comments", comment)   //Push comentario en el post
            )
            return result.wasAcknowledged()
        } catch (e: Exception) {
            return false
        }
    }

    override suspend fun addLike(postId: ObjectId, userId: ObjectId): Boolean {
        val newLike = Like(userId)
        try {
            val result = posts.updateOne(
                Filters.eq("_id", postId),
                Updates.push("likes", newLike)
            )
            return result.wasAcknowledged()
        } catch (e: Exception) {
            return false
        }
    }
}
