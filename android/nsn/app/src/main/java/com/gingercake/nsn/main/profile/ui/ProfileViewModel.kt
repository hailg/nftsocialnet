package com.gingercake.nsn.main.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.gingercake.nsn.framework.Constants
import com.gingercake.nsn.main.profile.model.ProfilePostFirestorePagingSource
import com.gingercake.nsn.model.user.User
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject

class ProfileViewModel @Inject constructor(
    private val firebaseFirestore: FirebaseFirestore
): ViewModel() {
    fun profileFlow(user: User) = Pager(PagingConfig(pageSize = Constants.QUERY_LIMIT.toInt())) {
        val query = FirebaseFirestore
            .getInstance()
            .collection(Constants.POSTS_COLLECTION)
        ProfilePostFirestorePagingSource(user, query)
    }.flow.cachedIn(viewModelScope)
}