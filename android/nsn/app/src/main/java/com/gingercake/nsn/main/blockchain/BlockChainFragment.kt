package com.gingercake.nsn.main.blockchain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.RequestManager
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.firebase.ui.firestore.paging.LoadingState
import com.gingercake.nsn.SessionManager
import com.gingercake.nsn.databinding.FragmentBlockChainBinding
import com.gingercake.nsn.framework.Constants
import com.gingercake.nsn.framework.displayToast
import com.gingercake.nsn.model.post.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class BlockChainFragment : DaggerFragment(), BlockchainPagingAdapter.Listener {

    @Inject
    lateinit var imageLoader: RequestManager
    @Inject
    lateinit var db: FirebaseFirestore

    private lateinit var binding: FragmentBlockChainBinding
    private lateinit var blockchainPagingAdapter: BlockchainPagingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val query = db
            .collection(Constants.BLOCKCHAIN_COLLECTION)
            .whereEqualTo("userId", SessionManager.currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
        val pagingConfig = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPrefetchDistance(20)
            .setPageSize(20).build()
        val options = FirestorePagingOptions
            .Builder<Transaction>()
            .setQuery(query, pagingConfig, Transaction::class.java)
            .build()
        blockchainPagingAdapter = BlockchainPagingAdapter(options, this, imageLoader)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBlockChainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.postRecyclerview.layoutManager = LinearLayoutManager(context)
        binding.postRecyclerview.adapter = blockchainPagingAdapter
        binding.swipeRefresh.setOnRefreshListener {
            refresh()
        }
    }

    override fun onStart() {
        super.onStart()
        blockchainPagingAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        blockchainPagingAdapter.stopListening()
    }

    override fun onItemSelected(position: Int, item: Transaction) {

    }

    override fun onLoadingStateChanged(state: LoadingState) {
        when (state) {
            LoadingState.ERROR -> {
                activity?.displayToast("Failed to load blockchain transactions. Please try again!")
                binding.swipeRefresh.isRefreshing = false
                binding.progressBarLoadMore.isVisible = false
            }
            LoadingState.LOADED -> {
                binding.swipeRefresh.isRefreshing = false
                binding.progressBarLoadMore.isVisible = false
            }
            LoadingState.FINISHED -> {
                binding.swipeRefresh.isRefreshing = false
                binding.progressBarLoadMore.isVisible = false
            }
            LoadingState.LOADING_INITIAL -> {
                binding.swipeRefresh.isRefreshing = true
                binding.progressBarLoadMore.isVisible = false
            }
            LoadingState.LOADING_MORE -> {
                binding.swipeRefresh.isRefreshing = false
                binding.progressBarLoadMore.isVisible = true
            }
        }
    }

    private fun refresh() {
        blockchainPagingAdapter.refresh()
    }

    companion object {
        private const val TAG = "BlockChainFragment"
    }


}