package com.gingercake.nsn.main.profile.model

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.gingercake.nsn.framework.Constants
import com.gingercake.nsn.model.post.Post
import com.gingercake.nsn.model.user.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import javax.inject.Inject

class ProfilePostFirestorePagingSource @Inject constructor(
    private val db: FirebaseFirestore,
    private val owner: User
) : PagingSource<Long, Post>() {

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Post> {
        return try {
            val currentTime = System.currentTimeMillis()
            val maxTimestamp = params.key ?: currentTime
            val result = db
                .collection(Constants.POSTS_COLLECTION)
                .whereEqualTo("owner.uid", owner.uid)
                .whereLessThan("timestamp", maxTimestamp)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(Constants.QUERY_LIMIT)
                .get().await()
            val posts = result.toObjects(Post::class.java)
            if (currentTime == maxTimestamp) { // Add marker for profile header
                posts.add(0, Post.profilePostForUser(owner))
                if (posts.size == 1) { // Add marker for user has no post
                    posts.add(0, Post.NO_POST_MARKER)
                }
            }
            LoadResult.Page(
                    data = posts,
                    prevKey = null,
                    nextKey = if (posts.size < Constants.QUERY_LIMIT) null else posts[posts.size - 1].timestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load profile", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, Post>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.minus(1) ?: anchorPage?.nextKey?.plus(1)
        }
    }

    companion object {
        private const val TAG = "ProfilePostFirestorePag"
    }
}