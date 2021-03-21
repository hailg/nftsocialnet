package com.gingercake.nsn.model.post

import android.util.Log
import com.gingercake.nsn.framework.Constants
import com.gingercake.nsn.model.user.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import java.util.*
import javax.inject.Inject

class PostRepo @Inject constructor(private val db: FirebaseFirestore) {

    private fun getId() = UUID.randomUUID().toString().replace("-", "")

    suspend fun commentPost(user: User, postId: String, content: String) {
        val commentId = getId()
        val comment = Comment(commentId, PostUser(user.uid, user.name, user.photoUrl), content, System.currentTimeMillis())
        val postRef = db
            .collection(Constants.POSTS_COLLECTION).document(postId)
        val commentRef = db
            .collection(Constants.POSTS_COLLECTION).document(postId)
            .collection(Constants.COMMENTS_COLLECTION).document(commentId)
        db.runTransaction { transaction ->
            transaction.update(postRef, "commentCount", FieldValue.increment(1))
            transaction.set(commentRef, comment)
        }.await()
    }

    suspend fun likePost(user: User, postId: String) {
        val postRef = db
            .collection(Constants.POSTS_COLLECTION).document(postId)
        val likeRef = postRef
            .collection(Constants.LIKES_COLLECTION).document(user.uid)

        db.runTransaction{ transaction ->
            transaction.update(postRef, "likes", FieldValue.arrayUnion(user.uid))
            transaction.set(likeRef, Like(user.uid, user.name, user.photoUrl))
        }.await()
    }

    suspend fun unlikePost(user: User, postId: String) {
        val postRef = db
            .collection(Constants.POSTS_COLLECTION).document(postId)
        val likeRef = postRef
            .collection(Constants.LIKES_COLLECTION).document(user.uid)

        db.runTransaction{ transaction ->
            transaction.delete(likeRef)
            transaction.update(postRef, "likes", FieldValue.arrayRemove(user.uid))
        }.await()
    }

    suspend fun getPostComments(postId: String): List<Comment>? {
        return try {
            db
                .collection(Constants.POSTS_COLLECTION).document(postId)
                .collection(Constants.COMMENTS_COLLECTION)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get().await().toObjects(Comment::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "failed getPostComments: ", e)
            null
        }
    }

    suspend fun likeComment(user: User, postId: String, commentId: String) {
        val commentRef = db
            .collection(Constants.POSTS_COLLECTION).document(postId)
            .collection(Constants.COMMENTS_COLLECTION).document(commentId)
        val likeRef = commentRef
            .collection(Constants.LIKES_COLLECTION).document(user.uid)
        db.runTransaction{ transaction ->
            transaction.update(commentRef, "likes", FieldValue.arrayUnion(user.uid))
            transaction.set(likeRef, Like(user.uid, user.name, user.photoUrl))
        }.await()
    }

    suspend fun unlikeComment(user: User, postId: String, commentId: String) {
        val commentRef = db
            .collection(Constants.POSTS_COLLECTION).document(postId)
            .collection(Constants.COMMENTS_COLLECTION).document(commentId)
        val likeRef = commentRef
            .collection(Constants.LIKES_COLLECTION).document(user.uid)

        db.runTransaction{ transaction ->
            transaction.delete(likeRef)
            transaction.update(commentRef, "likes", FieldValue.arrayRemove(user.uid))
        }.await()
    }

    companion object {
        private const val TAG = "PostRepo"
    }
}