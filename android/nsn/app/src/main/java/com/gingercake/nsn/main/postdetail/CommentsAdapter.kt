package com.gingercake.nsn.main.postdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.gingercake.nsn.databinding.LayoutCommentListItemBinding
import com.gingercake.nsn.model.post.Comment
import com.google.firebase.firestore.FirebaseFirestoreException
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class CommentsAdapter constructor(
    private val listener: CommentsAdapter.Listener,
    private val imageLoader: RequestManager,
    options: FirestoreRecyclerOptions<Comment>,
) : FirestoreRecyclerAdapter<Comment, CommentsAdapter.CommentViewHolder>(options) {

    private val prettyTime = PrettyTime()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = LayoutCommentListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding, prettyTime, imageLoader, listener)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int, model: Comment) {
        holder.bind(model)
    }

    override fun onDataChanged() {
        super.onDataChanged()
        listener.onDataChanged()
    }

    override fun onError(e: FirebaseFirestoreException) {
        super.onError(e)
        listener.onError(e)
    }

    interface Listener {
        fun onDataChanged()
        fun onError(e: FirebaseFirestoreException)
        fun onItemSelected(position: Int, comment: Comment)
    }

    class CommentViewHolder constructor(
        private val binding: LayoutCommentListItemBinding,
        private val prettyTime: PrettyTime,
        private val imageLoader: RequestManager,
        private val listener: Listener
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: Comment) = with(binding) {
            itemView.setOnClickListener {
                listener?.onItemSelected(bindingAdapterPosition, comment)
            }
            imageLoader
                .load(comment.author.photoUrl)
                .dontAnimate()
                .into(binding.authorImage)
            binding.authorName.text = comment.author.name
            binding.message.text = comment.content
            binding.timestamp.text = prettyTime.format(Date(comment.timestamp))
        }
    }
}

