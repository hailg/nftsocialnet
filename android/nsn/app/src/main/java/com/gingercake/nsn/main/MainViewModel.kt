package com.gingercake.nsn.main

import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gingercake.nsn.SessionManager
import com.gingercake.nsn.auth.AuthActivity
import com.gingercake.nsn.framework.Constants
import com.gingercake.nsn.model.post.Post
import com.gingercake.nsn.model.post.PostRepo
import com.gingercake.nsn.model.user.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.parcelize.Parcelize
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

@Parcelize
data class CreatePostProgress(val state: Int,
                              val bytesTransferred: Long = 0,
                              val totalByteCount: Long = 0,
                              val post: Post = Post(),
                              val errorMessage: String = "Main"): Parcelable {
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
    private val functions: FirebaseFunctions,
): ViewModel() {
    private val _postCreationLiveData = MutableLiveData<CreatePostProgress>()
    private val _accountBalanceLiveData = MutableLiveData<String>()
    private val _purchaseLiveData = MutableLiveData<CreatePostProgress>()

    val postCreationLiveData: LiveData<CreatePostProgress> = _postCreationLiveData
    val accountBalanceLiveData: LiveData<String> = _accountBalanceLiveData
    val purchaseLiveData: LiveData<CreatePostProgress> = _purchaseLiveData

    fun getAccountBalance() = viewModelScope.launch {
        try {
            val balanceResponse = functions
                    .getHttpsCallable("blockchain_account-getAccountBalance")
                    .call(hashMapOf("username" to SessionManager.currentUser.username))
                    .continueWith { task ->
                        task.result?.data as HashMap<String, Object>
                    }
                    .await()
            var balance = (balanceResponse["data"] as HashMap<String, Object>)["balance"] as String
            if (balance.endsWith("EOS")) {
               balance = balance.replace("EOS", "").trim()
            }
            SessionManager.currentUser.eosAmount = balance
            _accountBalanceLiveData.postValue(balance)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query account balance", e)
        }
    }

    fun createPost(postId: String, title: String, content: String,
                   resourcePath: String, resourceType: Int, price: String, password: String) = viewModelScope.launch {
        val resourceName = if (!resourcePath.isBlank()) {
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
        } else ""

        val post = Post.newInstance(postId, title, content, resourceName, resourceType, price, SessionManager.currentUser)
        db.collection(Constants.POSTS_COLLECTION).document(postId).set(post, SetOptions.merge()).await()
        val data = hashMapOf(
                "postId" to postId,
                "password" to password,
        )
        try {
            functions
                    .getHttpsCallable("blockchain_post-createPost")
                    .call(data)
                    .continueWith { task ->
                        Log.d(AuthActivity.TAG, "" + task.result)
                        task.result?.data
                    }
                    .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create post", e)
            val msg = if (e is FirebaseFunctionsException) {
                e.details.toString()
            } else {
                "Failed to create your post. Please try again!"
            }
            _postCreationLiveData.postValue(CreatePostProgress(CreatePostProgress.FAIL, post = post, errorMessage = msg))
            return@launch
        }
        _postCreationLiveData.postValue(CreatePostProgress(CreatePostProgress.SUCCESS, post = post))

    }

    fun viewPost(user: User, post: Post) {
        viewModelScope.launch {
            if (post.owner.uid != user.uid && post.author.uid != user.uid) {
                postRepo.viewPost(user, post.id)
            }
        }
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

    fun purchasePost(post: Post, newPrice: String, password: String) = viewModelScope.launch {
        try {
            functions
                    .getHttpsCallable("blockchain_post-purchasePost")
                    .call(hashMapOf(
                            "postId" to post.id,
                            "newPrice" to newPrice,
                            "password" to password
                    ))
                    .continueWith { task ->
                        task.result?.data                    }
                    .await()
            _purchaseLiveData.postValue(CreatePostProgress(CreatePostProgress.SUCCESS, post = post))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to purchase post", e)
            val msg = if (e is FirebaseFunctionsException) {
                e.details.toString()
            } else {
                "Failed to purchase this post. Please try again!"
            }
            _purchaseLiveData.postValue(CreatePostProgress(CreatePostProgress.FAIL, post = post, errorMessage = msg))
        }
    }
    companion object {
        private const val TAG = "NewPostViewModel"
    }
}