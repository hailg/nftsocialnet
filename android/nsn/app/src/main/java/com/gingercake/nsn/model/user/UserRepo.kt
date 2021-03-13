package com.gingercake.nsn.model.user

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserRepo @Inject constructor(private val userDao: UserDao) {
    fun loadUserById(uid: String): Flow<User> {
        return userDao.loadUserById(uid)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun saveUser(user: User) {
        userDao.insertUsers(user)
    }
}