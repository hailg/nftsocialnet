package com.gingercake.nsn.main.postdetail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.bumptech.glide.RequestManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.gingercake.nsn.SessionManager
import com.gingercake.nsn.databinding.FragmentPostDetailBinding
import com.gingercake.nsn.framework.BaseChildFragment
import com.gingercake.nsn.framework.Constants
import com.gingercake.nsn.framework.hideKeyboard
import com.gingercake.nsn.model.post.Comment
import com.gingercake.nsn.model.post.PostRepo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import javax.inject.Inject

class PostDetailFragment : BaseChildFragment(), CommentsAdapter.Listener {

    @Inject
    lateinit var imageLoader: RequestManager
    @Inject
    lateinit var db: FirebaseFirestore
    @Inject
    lateinit var postRepo: PostRepo

    private val safeArgs: PostDetailFragmentArgs by navArgs()
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var options: FirestoreRecyclerOptions<Comment>
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var postId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = safeArgs.postId
        Log.d(TAG, "onCreate: ${safeArgs.postId}")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        linearLayoutManager = LinearLayoutManager(context)
        val query = db
            .collection(Constants.POSTS_COLLECTION).document(postId)
            .collection(Constants.COMMENTS_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
        options = FirestoreRecyclerOptions.Builder<Comment>()
            .setQuery(query, Comment::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        val binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        binding.commentRecycleView.layoutManager = linearLayoutManager
        commentsAdapter = CommentsAdapter(this, imageLoader, options)
        commentsAdapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (commentsAdapter.itemCount > 0) {
                    binding.commentRecycleView.scrollToPosition(commentsAdapter.itemCount - 1)
                }
            }
        })
        binding.commentRecycleView.adapter = commentsAdapter
        binding.send.setOnClickListener {
            val content = binding.textSend.text?.toString()
            if (!content.isNullOrBlank()) {
                lifecycleScope.launch {
                    postRepo.commentPost(SessionManager.currentUser, postId, content)
                }
            }
            binding.textSend.setText("")
            activity?.hideKeyboard(binding.textSend)
        }
        return binding.root
    }

    companion object {
        private const val TAG = "PostDetailFragment"
    }

    override fun onDataChanged() {

    }

    override fun onError(e: FirebaseFirestoreException) {

    }

    override fun onItemSelected(position: Int, comment: Comment) {

    }
}