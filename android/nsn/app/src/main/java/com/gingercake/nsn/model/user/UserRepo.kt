package com.gingercake.nsn.model.user

import androidx.annotation.WorkerThread
import com.gingercake.nsn.framework.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepo @Inject constructor(private val db: FirebaseFirestore) {

    suspend fun loadUsersByIds(uid: Set<String>): Map<String, User> {
        return try {
            val result = db
                .collection(Constants.USERS_COLLECTION)
                .whereIn("id", uid.toList())
                .get().await().toObjects(User::class.java)
            result.map { it.uid to it }.toMap()
        } finally {
            emptyMap<String, User>()
        }

    }

    suspend fun loadUserById(uid: String): User? {
        return try {
            db
                .collection(Constants.USERS_COLLECTION)
                .document(uid)
                .get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun saveUser(user: User) {
        db
            .collection(Constants.USERS_COLLECTION)
            .document(user.uid)
            .set(mapOf("uid" to user.uid,
                "name" to user.name,
                "email" to user.email,
                "photoUrl" to user.photoUrl), SetOptions.merge()).await()
    }
}