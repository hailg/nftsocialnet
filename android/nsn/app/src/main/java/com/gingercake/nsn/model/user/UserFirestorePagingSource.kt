package com.gingercake.nsn.model.user

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import javax.inject.Inject

const val USER_COLLECTION = "users"
private const val LOAD_SIZE = 10L

class UserFirestorePagingSource @Inject constructor(
    private val query: Query,
) : PagingSource<Int, User>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, User> {
        return try {
            val nextIndex = params.key ?: 0
            val result = query
                .startAt(nextIndex * LOAD_SIZE)
                .limit(LOAD_SIZE)
                .get().await()
            LoadResult.Page(
                data = result.toObjects(User::class.java),
                prevKey = null,
                nextKey = if (result.isEmpty) null else nextIndex + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, User>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}