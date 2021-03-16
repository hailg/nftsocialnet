package com.gingercake.nsn.main.profile.ui

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.gingercake.nsn.SessionManager
import com.gingercake.nsn.databinding.LayoutPostListItemBinding
import com.gingercake.nsn.model.user.Post

class ProfilePagingAdapter (
        private val interaction: Interaction? = null,
        private val requestManager: RequestManager) : PagingDataAdapter<Post, ProfilePagingAdapter.PostViewHolder>(Companion) {

    companion object : DiffUtil.ItemCallback<Post>() {
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
        return PostViewHolder(binding, interaction, requestManager)
    }

    interface Interaction {
        fun onItemSelected(position: Int, item: Post)
    }

    class PostViewHolder
    constructor(
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

                        binding.nameField.text = item.title
                        if (item.resource.isNotEmpty()) {
                            requestManager
                                .load(item.resource)
                                .dontAnimate()
                                .into(binding.profileImage)
                        }
                    }
                    else -> {
                        if (item.owner == SessionManager.currentUser.uid) {
                            binding.authorImage.isVisible = true
                            requestManager
                                .load(SessionManager.currentUser.photoUrl)
                                .dontAnimate()
                                .into(binding.authorImage)
                        } else {
                            binding.authorImage.isVisible = false
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
                        binding.authorName.text = item.owner
                        binding.updateDate.text = DateUtils.getRelativeDateTimeString(
                            itemView.context,
                            item.timestamp,
                            DateUtils.SECOND_IN_MILLIS,
                            DateUtils.WEEK_IN_MILLIS,
                            DateUtils.FORMAT_SHOW_TIME
                        )
                        if (item.resourceType == Post.IMAGE_TYPE && item.resource.isNotEmpty()) {
                            requestManager
                                .load(item.resource)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(binding.postImage)
                        }
                    }
                }
            }
        }
    }
}