package com.gingercake.nsn.main.profile.model

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.gingercake.nsn.framework.Constants
import com.gingercake.nsn.model.user.Post
import com.gingercake.nsn.model.user.User
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import javax.inject.Inject

class ProfilePostFirestorePagingSource @Inject constructor(
        private val owner: User,
        private val query: Query,
) : PagingSource<Long, Post>() {

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Post> {
        return try {
            val currentTime = System.currentTimeMillis()
            val maxTimestamp = params.key ?: currentTime
            val result = query
                    .whereLessThan("timestamp", maxTimestamp)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(Constants.QUERY_LIMIT)
                    .get().await()
            val posts = result.toObjects(Post::class.java)
            if (currentTime == maxTimestamp) { // Add marker for profile header
                posts.add(0, Post("", "", owner.uid, owner.name,
                        owner.email, owner.photoUrl, Post.PROFILE_HEADER_TYPE,
                        0, 0, 0, 0, 0, 0, 0.0))
                if (posts.size == 1) { // Add marker for user has no post
                    posts.add(0, Post("", "", "", "",
                            "", "", Post.NO_POST_TYPE,
                            0, 0, 0, 0, 0, 0, 0.0))
                }
            }
            LoadResult.Page(
                    data = posts,
                    prevKey = null,
                    nextKey = if (posts.size < Constants.QUERY_LIMIT) null else posts[posts.size - 1].timestamp
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, Post>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.minus(1) ?: anchorPage?.nextKey?.plus(1)
        }
    }
}