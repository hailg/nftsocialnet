package com.gingercake.nsn.main.profile.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.gingercake.nsn.framework.Constants
import com.gingercake.nsn.main.profile.model.ProfilePostFirestorePagingSource
import com.gingercake.nsn.model.post.PostRepo
import com.gingercake.nsn.model.user.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import javax.inject.Inject

class ProfileViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val postRepo: PostRepo,
): ViewModel() {

    fun profileFlow(user: User) = Pager(PagingConfig(pageSize = Constants.QUERY_LIMIT.toInt())) {
        ProfilePostFirestorePagingSource(db, user)
    }.flow.cachedIn(viewModelScope)

    fun likePost(user: User, postId: String) {
        viewModelScope.launch {
            postRepo.likePost(user, postId)
        }
    }

    fun unlikePost(user: User, postId: String) {
        viewModelScope.launch {
            postRepo.unlikePost(user, postId)
        }
    }
}