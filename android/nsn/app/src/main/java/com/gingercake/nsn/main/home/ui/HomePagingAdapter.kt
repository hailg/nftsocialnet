package com.gingercake.nsn.main.home.ui

import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.firebase.ui.firestore.paging.LoadingState
import com.gingercake.nsn.SessionManager
import com.gingercake.nsn.databinding.LayoutPostOnlyListItemBinding
import com.gingercake.nsn.main.MainViewModel
import com.gingercake.nsn.model.post.Post
import com.google.firebase.storage.FirebaseStorage

class HomePagingAdapter constructor(
    private val options: FirestorePagingOptions<Post>,
    private val mainViewModel: MainViewModel,
    private val listener: HomePagingAdapter.Listener,
    private val imageLoader: RequestManager
) : FirestorePagingAdapter<Post, HomePagingAdapter.PostViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = LayoutPostOnlyListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(mainViewModel, binding, listener, imageLoader)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int, model: Post) {
        holder.bind(model)
    }

    override fun onLoadingStateChanged(state: LoadingState) {
        super.onLoadingStateChanged(state)
        listener.onLoadingStateChanged(state)
    }

    interface Listener {
        fun onItemSelected(position: Int, item: Post)
        fun onLoadingStateChanged(state: LoadingState)
    }

    class PostViewHolder
    constructor(
        private val mainViewModel: MainViewModel,
        private val binding: LayoutPostOnlyListItemBinding,
        private val interaction: Listener?,
        private val requestManager: RequestManager
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Post?) = with(binding) {
            item?.let {
                itemView.setOnClickListener {
                    interaction?.onItemSelected(bindingAdapterPosition, item)
                }
                requestManager
                    .load(item.owner.photoUrl)
                    .dontAnimate()
                    .into(binding.authorImage)
                if (item.resourceType == Post.IMAGE_TYPE && item.resource.isNotEmpty()) {
                    if (item.resource.startsWith("http")) {
                        requestManager
                            .load(item.resource)
                            .into(binding.postImage)
                    } else {
                        val resourceRef = FirebaseStorage.getInstance().reference.child(item.resource)
                        requestManager
                            .load(resourceRef)
                            .into(binding.postImage)
                    }

                }
                binding.postCardView.isVisible = true
                binding.postTitle.text = item.title
                if (item.content.isEmpty()) {
                    binding.postContent.isVisible = false
                } else {
                    binding.postContent.isVisible = true
                    binding.postContent.text = item.content
                }
                binding.authorName.text = item.owner.name
                binding.updateDate.text = DateUtils.getRelativeDateTimeString(
                    itemView.context,
                    item.timestamp,
                    DateUtils.SECOND_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    DateUtils.FORMAT_SHOW_TIME
                )
                binding.likeCount.text = item.likes.size.toString()
                if (item.commentCount > 0) {
                    binding.commentCount.isVisible = true
                    binding.commentCount.text = item.commentCount.toString()
                } else {
                    binding.commentCount.isVisible = false
                }
                if (item.price != "-1") {
                    binding.price.isVisible = true
                    binding.price.text = "${item.price} NSN"
                } else {
                    binding.price.isVisible = false
                }
                val isLiked = item.likes.contains(SessionManager.currentUser.uid)
                if (isLiked) {
                    binding.likeText.text = "Unlike"
                } else {
                    binding.likeText.text = "Like"
                }
                val likes = item.likes.toMutableSet()
                binding.likeBtn.setOnClickListener {
                    if (binding.likeText.text == "Like") {
                        likes.add(SessionManager.currentUser.uid)
                        binding.likeText.text = "Unlike"
                        mainViewModel.likePost(SessionManager.currentUser, item.id)
                    } else {
                        likes.remove(SessionManager.currentUser.uid)
                        binding.likeText.text = "Like"
                        mainViewModel.unlikePost(SessionManager.currentUser, item.id)
                    }
                    binding.likeCount.text = likes.size.toString()
                }
                binding.commentBtn.setOnClickListener {
                    Log.d(TAG, "onClick: Comment Button")
                    interaction?.onItemSelected(bindingAdapterPosition, item)
                }

                binding.buyBtn.setOnClickListener {
                    if (item.owner.uid == SessionManager.currentUser.uid) {
                        Toast
                            .makeText(binding.buyBtn.context, "Cannot buy this post. You are already the owner.", Toast.LENGTH_SHORT)
                            .show()
                        return@setOnClickListener
                    }
                }
            }
        }
    }
    
    companion object {
        private const val TAG = "HomePagingAdapter"
    }
}