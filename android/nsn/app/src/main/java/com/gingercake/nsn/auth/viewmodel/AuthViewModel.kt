package com.gingercake.nsn.auth.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.gingercake.nsn.model.user.User
import com.gingercake.nsn.model.user.UserRepo
import kotlinx.coroutines.launch
import javax.inject.Inject


class AuthViewModel @Inject constructor(private val userRepo: UserRepo) : ViewModel() {
    fun observeUser(uid: String): LiveData<User> = userRepo.loadUserById(uid).asLiveData()

    fun saveUser(user: User) = viewModelScope.launch {
        userRepo.saveUser(user)
    }
}