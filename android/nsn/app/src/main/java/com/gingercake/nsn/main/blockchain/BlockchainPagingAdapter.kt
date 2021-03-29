package com.gingercake.nsn.main.blockchain

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.firebase.ui.firestore.paging.LoadingState
import com.gingercake.nsn.R
import com.gingercake.nsn.SessionManager
import com.gingercake.nsn.databinding.LayoutTransactionListItemBinding
import com.gingercake.nsn.model.post.Transaction
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class BlockchainPagingAdapter(
    val options: FirestorePagingOptions<Transaction>,
    private val listener: Listener,
    private val imageLoader: RequestManager
): FirestorePagingAdapter<Transaction, BlockchainPagingAdapter.ViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LayoutTransactionListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, listener, imageLoader)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: Transaction) {
        holder.bind(model)
    }

    override fun onLoadingStateChanged(state: LoadingState) {
        super.onLoadingStateChanged(state)
        listener.onLoadingStateChanged(state)
    }

    interface Listener {
        fun onItemSelected(position: Int, item: Transaction)
        fun onLoadingStateChanged(state: LoadingState)
    }

    class ViewHolder constructor(
        private val binding: LayoutTransactionListItemBinding,
        private val listener: Listener,
        private val imageLoader: RequestManager
    ) : RecyclerView.ViewHolder(binding.root) {

        data class TransactionInfo(val actor: String, val actorPhoto: String,
                                   val content: String, val amount: String, val timestamp: String)

        fun bind(item: Transaction) = with(binding) {
            itemView.setOnClickListener {
                listener.onItemSelected(bindingAdapterPosition, item)
            }
            val transactionInfo = getTransactionInfo(item)
            if (transactionInfo.actor == "NSN") {
                imageLoader
                    .load(R.drawable.small_logo)
                    .into(binding.authorImage)
            } else {
                imageLoader
                    .load(transactionInfo.actorPhoto)
                    .into(binding.authorImage)
            }
            binding.authorName.text = transactionInfo.actor
            binding.content.text = transactionInfo.content
            binding.amount.text = transactionInfo.amount
            binding.updateDate.text = transactionInfo.timestamp
        }

        private fun getTransactionInfo(transaction: Transaction): TransactionInfo {
            var postTitle = ""
            var amount = ""
            var postOwnerName = ""
            var postOwnerPhotoUrl = ""
            var buyerName = ""
            var buyerPhotoUrl = ""
            return when (transaction.type) {
                Transaction.TRANSACTION_TYPE_USER_CREATED -> {
                    TransactionInfo(
                        "NSN",
                        "NSN",
                        "Your account is created on EOS blockchain",
                        "Free",
                        df.format(Date(transaction.timestamp))
                    )
                }
                Transaction.TRANSACTION_TYPE_EOS_RECEIVED -> {
                    TransactionInfo(
                        "NSN",
                        "NSN",
                        transaction.data.getOrDefault("memo", "Welcome to NSN! Enjoy this little bonus to get you started.") as String,
                        transaction.data.getOrDefault("quantity", "10.0000 EOS"),
                        df.format(Date(transaction.timestamp))
                    )
                }
                Transaction.TRANSACTION_TYPE_NSN_ISSUED -> {
                    postTitle = transaction.data.getOrDefault("postTitle", "")
                    TransactionInfo(
                    SessionManager.currentUser.name,
                    SessionManager.currentUser.photoUrl,
                    "You created a NSN \"$postTitle\"",
                    "Free",
                    df.format(Date(transaction.timestamp)))
                }
                Transaction.TRANSACTION_TYPE_NSN_LISTED -> {
                    postTitle = transaction.data.getOrDefault("postTitle", "")
                    amount = transaction.data.getOrDefault("price", "0")
                    TransactionInfo(
                        SessionManager.currentUser.name,
                        SessionManager.currentUser.photoUrl,
                        "You listed a NSN post \"$postTitle\" for sale, price $amount EOS.",
                        "Free",
                        df.format(Date(transaction.timestamp)))
                }
                Transaction.TRANSACTION_TYPE_NSN_PURCHASED -> {
                    postTitle = transaction.data.getOrDefault("postTitle", "")
                    postOwnerName = transaction.data.getOrDefault("ownerName", "")
                    postOwnerPhotoUrl = transaction.data.getOrDefault("ownerPhoto", "")
                    amount = transaction.data.getOrDefault("price", "0")
                    TransactionInfo(
                        postOwnerName,
                        postOwnerPhotoUrl,
                        "You purchased the NSN post \"$postTitle\" from $postOwnerName",
                        "-$amount EOS",
                        df.format(Date(transaction.timestamp)))
                }
                Transaction.TRANSACTION_TYPE_NSN_SOLD -> {
                    postTitle = transaction.data.getOrDefault("postTitle", "")
                    buyerName = transaction.data.getOrDefault("buyerName", "")
                    buyerPhotoUrl = transaction.data.getOrDefault("buyerPhoto", "")
                    amount = transaction.data.getOrDefault("price", "0")
                    TransactionInfo(
                        buyerName,
                        buyerPhotoUrl,
                        "$buyerName bought the NSN post \"$postTitle\" from you",
                        "+$amount EOS",
                        df.format(Date(transaction.timestamp)))
                }
                Transaction.TRANSACTION_TYPE_NSN_ROYAL_FEE -> {
                    postTitle = transaction.data.getOrDefault("postTitle", "")
                    buyerName = transaction.data.getOrDefault("buyerName", "")
                    postOwnerName = transaction.data.getOrDefault("ownerName", "")
                    buyerPhotoUrl = transaction.data.getOrDefault("buyerPhoto", "")
                    amount = transaction.data.getOrDefault("amount", "0")
                    TransactionInfo(
                        buyerName,
                        buyerPhotoUrl,
                        "You earned royal free, $buyerName bought the NSN post \"$postTitle\" from $postOwnerName.",
                        "+$amount EOS",
                        df.format(Date(transaction.timestamp)))
                }
                else -> {
                    TransactionInfo("", "", "", "", "")
                }
            }
        }

    }
    companion object {
        private const val TAG = "BlockchainPagingAdapter"
        private val df = SimpleDateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG)
    }
}