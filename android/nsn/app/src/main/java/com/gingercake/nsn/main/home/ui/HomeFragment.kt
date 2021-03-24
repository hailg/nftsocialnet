package com.gingercake.nsn.main.home.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.RequestManager
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.firebase.ui.firestore.paging.LoadingState
import com.gingercake.nsn.SessionManager
import com.gingercake.nsn.databinding.FragmentHomeBinding
import com.gingercake.nsn.framework.Constants
import com.gingercake.nsn.framework.displayToast
import com.gingercake.nsn.main.CreatePostProgress
import com.gingercake.nsn.main.MainViewModel
import com.gingercake.nsn.main.MainViewModelFactory
import com.gingercake.nsn.model.post.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class HomeFragment : DaggerFragment(), HomePagingAdapter.Listener {

    @Inject
    lateinit var imageLoader: RequestManager

    @Inject
    lateinit var db: FirebaseFirestore

    @Inject
    lateinit var mainViewModelFactory: MainViewModelFactory

    private lateinit var mainViewModel: MainViewModel
    private lateinit var binding: FragmentHomeBinding
    private lateinit var postAdapter: HomePagingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProvider(this, mainViewModelFactory).get(MainViewModel::class.java)
        val query = db
            .collection(Constants.POSTS_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
        val pagingConfig = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPrefetchDistance(10)
            .setPageSize(10).build()
        val options = FirestorePagingOptions
            .Builder<Post>()
            .setQuery(query, pagingConfig, Post::class.java)
            .build()
        postAdapter = HomePagingAdapter(options, mainViewModel, this, imageLoader)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.postRecyclerview.layoutManager = LinearLayoutManager(context)
        binding.postRecyclerview.adapter = postAdapter
        binding.fab.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToNewPostFragment()
            findNavController().navigate(action)
        }
        binding.swipeRefresh.setOnRefreshListener {
            refresh()
        }
        mainViewModel.postCreationLiveData.observe(viewLifecycleOwner, {
            Log.d(TAG, "onPostCreation change: $it")
            when (it.state) {
                CreatePostProgress.SUCCESS -> refresh()
                CreatePostProgress.FAIL -> activity?.displayToast("Failed to create post. Please try again!")
            }
        })
    }

    override fun onStart() {
        super.onStart()
        postAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        postAdapter.stopListening()
    }

    override fun onItemSelected(position: Int, item: Post) {
        val action = HomeFragmentDirections.actionHomeFragmentToPostDetailFragment(item.id)
        findNavController().navigate(action)
        mainViewModel.viewPost(SessionManager.currentUser, item)
    }

    override fun onLoadingStateChanged(state: LoadingState) {
        when (state) {
            LoadingState.ERROR -> {
                activity?.displayToast("Failed to load posts. Please try again!")
                binding.swipeRefresh.isRefreshing = false
                binding.progressBarLoadMore.isVisible = false
            }
            LoadingState.LOADED -> {
                binding.swipeRefresh.isRefreshing = false
                binding.progressBarLoadMore.isVisible = false
            }
            LoadingState.FINISHED -> {
//                activity?.displayToast("No more posts to see. You're amazing!")
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

    override fun onItemBuying(position: Int, item: Post) {

    }

    private fun refresh() {
        postAdapter.refresh()
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}