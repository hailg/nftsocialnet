package com.gingercake.nsn.main

import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gingercake.nsn.SessionManager
import com.gingercake.nsn.framework.Constants
import com.gingercake.nsn.model.post.Post
import com.gingercake.nsn.model.post.PostRepo
import com.gingercake.nsn.model.user.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.parcelize.Parcelize
import java.util.*
import javax.inject.Inject

@Parcelize
data class CreatePostProgress(val state: Int,
                              val bytesTransferred: Long = 0,
                              val totalByteCount: Long = 0,
                              val post: Post = Post()): Parcelable {
    companion object {
        const val UPLOADING = 0
        const val UPLOAD_PAUSED = 1
        const val FAIL = 3
        const val CANCELLED = 4
        const val UPLOADED = 5
        const val SUCCESS = 6
    }
}
class MainViewModel @Inject constructor(
    private val storage: FirebaseStorage,
    private val db: FirebaseFirestore,
    private val postRepo: PostRepo,
): ViewModel() {
    private val _postCreationLiveData = MutableLiveData<CreatePostProgress>()
    val postCreationLiveData: LiveData<CreatePostProgress> = _postCreationLiveData

    fun createPost(title: String, content: String,
                   resourcePath: String?, resourceType: Int, price: String) = viewModelScope.launch {
        val resourceName = resourcePath?.let {
            val file = Uri.fromFile(java.io.File(resourcePath))
            val dotIndex = resourcePath.lastIndexOf('.')
            val fileExt = if (dotIndex > -1) {
                resourcePath.substring(dotIndex)
            } else {
                ""
            }
            val uploadingName = UUID.randomUUID().toString().replace("-", "") + fileExt
            storage
                .reference.child(uploadingName)
                .putFile(file)
                .addOnCanceledListener {
                    _postCreationLiveData.postValue(CreatePostProgress(CreatePostProgress.CANCELLED))
                }
                .addOnFailureListener {
                    Log.e(TAG, "failed to upload", it)
                    _postCreationLiveData.postValue(CreatePostProgress(CreatePostProgress.FAIL))
                }
                .addOnPausedListener {
                    Log.e(TAG, "upload is paused")
                    _postCreationLiveData.postValue(CreatePostProgress(CreatePostProgress.UPLOAD_PAUSED, it.bytesTransferred, it.totalByteCount))
                }
                .addOnProgressListener {
                    _postCreationLiveData.postValue(CreatePostProgress(CreatePostProgress.UPLOADING, it.bytesTransferred, it.totalByteCount))
                }
                .addOnSuccessListener {
                    _postCreationLiveData.postValue(CreatePostProgress(CreatePostProgress.UPLOADED, it.bytesTransferred, it.totalByteCount))
                }.await()
            uploadingName
        } ?: ""

        val postId = UUID.randomUUID().toString().replace("-", "")
        val post = Post.newInstance(postId, title, content, resourceName, resourceType, price, SessionManager.currentUser)
        db.collection(Constants.POSTS_COLLECTION).document(postId).set(post, SetOptions.merge()).await()
        _postCreationLiveData.postValue(CreatePostProgress(CreatePostProgress.SUCCESS, post = post))
    }

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

    companion object {
        private const val TAG = "NewPostViewModel"
    }
}