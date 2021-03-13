package com.gingercake.nsn

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gingercake.nsn.model.user.User
import com.gingercake.nsn.model.user.UserDao

@Database(entities = [User::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun getUserDao(): UserDao

    companion object{
        val DATABASE_NAME: String = "app_db"
    }
}