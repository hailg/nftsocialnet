package com.gingercake.nsn.main.profile.ui

import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.gingercake.nsn.SessionManager
import com.gingercake.nsn.databinding.LayoutPostListItemBinding
import com.gingercake.nsn.main.MainViewModel
import com.gingercake.nsn.model.post.Post
import com.google.firebase.storage.FirebaseStorage

class ProfilePagingAdapter (
        private val profileViewModel: ProfileViewModel,
        private val interaction: Interaction? = null,
        private val requestManager: RequestManager) : PagingDataAdapter<Post, ProfilePagingAdapter.PostViewHolder>(Companion) {

    companion object : DiffUtil.ItemCallback<Post>() {
        private const val TAG = "ProfilePagingAdapter"

        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PostViewHolder {
        val binding = LayoutPostListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(profileViewModel, binding, interaction, requestManager)
    }

    interface Interaction {
        fun onItemSelected(position: Int, item: Post)
    }

    class PostViewHolder
    constructor(
        private val profileViewModel: ProfileViewModel,
        private val binding: LayoutPostListItemBinding,
        private val interaction: Interaction?,
        private val requestManager: RequestManager
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Post?) = with(binding) {
            item?.let {
                itemView.setOnClickListener {
                    interaction?.onItemSelected(bindingAdapterPosition, item)
                }
                when (item.resourceType) {
                    Post.PROFILE_HEADER_TYPE -> {
                        binding.postCardView.isVisible = false
                        binding.profileContainer.isVisible = true

                        binding.nameField.text = "${item.owner.name} (@${SessionManager.currentUser.username})"
                        if (!SessionManager.currentUser.eosAmount.isEmpty()) {
                            binding.balanceField.text = "${SessionManager.currentUser.eosAmount} EOS"
                        }
                        requestManager
                            .load(item.owner.photoUrl)
                            .dontAnimate()
                            .into(binding.profileImage)
                    }
                    Post.NO_POST_TYPE -> {
                        binding.postCardView.isVisible = false
                        binding.profileContainer.isVisible = false
                        binding.noPostPlaceHolder.isVisible = true
                    }
                    else -> {
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
                        binding.profileContainer.isVisible = false
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
                            binding.price.text = "${item.price} EOS"
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
                                profileViewModel.likePost(SessionManager.currentUser, item.id)
                            } else {
                                likes.remove(SessionManager.currentUser.uid)
                                binding.likeText.text = "Like"
                                profileViewModel.unlikePost(SessionManager.currentUser, item.id)
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
        }
    }

}