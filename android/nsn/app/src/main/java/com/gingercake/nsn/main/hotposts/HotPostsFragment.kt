package com.gingercake.nsn.main.hotposts

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.RequestManager
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.firebase.ui.firestore.paging.LoadingState
import com.gingercake.nsn.R
import com.gingercake.nsn.SessionManager
import com.gingercake.nsn.databinding.FragmentHotPostsBinding
import com.gingercake.nsn.framework.Constants
import com.gingercake.nsn.framework.displayToast
import com.gingercake.nsn.main.CreatePostProgress
import com.gingercake.nsn.main.MainActivity
import com.gingercake.nsn.main.MainViewModel
import com.gingercake.nsn.main.home.ui.HomeFragmentDirections
import com.gingercake.nsn.model.post.Post
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class HotPostsFragment : DaggerFragment(), HotPostsPagingAdapter.Listener {
    @Inject
    lateinit var imageLoader: RequestManager

    @Inject
    lateinit var db: FirebaseFirestore
    private lateinit var mainViewModel: MainViewModel
    private lateinit var binding: FragmentHotPostsBinding
    private lateinit var postAdapter: HotPostsPagingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = (activity as MainActivity).mainViewModel
        val query = db
            .collection(Constants.POSTS_COLLECTION)
            .orderBy("rank", Query.Direction.DESCENDING)
        val pagingConfig = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPrefetchDistance(10)
            .setPageSize(10).build()
        val options = FirestorePagingOptions
            .Builder<Post>()
            .setQuery(query, pagingConfig, Post::class.java)
            .build()
        postAdapter = HotPostsPagingAdapter(options, mainViewModel, this, imageLoader)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHotPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.postRecyclerview.layoutManager = LinearLayoutManager(context)
        binding.postRecyclerview.adapter = postAdapter
        binding.fab.setOnClickListener {
            val action = HotPostsFragmentDirections.actionHotPostsFragmentToNewPostFragment()
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
        val action = HotPostsFragmentDirections.actionHotPostsFragmentToPostDetailFragment(item.id)
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

    override fun onItemBuying(position: Int, post: Post) {
        if (post.price == "-1") {
            activity?.let { activity ->
                MaterialAlertDialogBuilder(activity)
                    .setTitle(resources.getString(R.string.app_name))
                    .setMessage("Sorry, this item is not on sale!")
                    .setPositiveButton(resources.getString(R.string.okay)) { _, _ ->
                    }
                    .show()
            }
            return
        }
        if (post.price.toDouble() > SessionManager.currentUser.eosAmount.toDouble()) {
            activity?.let { activity ->
                MaterialAlertDialogBuilder(activity)
                    .setTitle(resources.getString(R.string.app_name))
                    .setMessage("Sorry, you don't have enough EOS to buy this NSN post.")
                    .setPositiveButton(resources.getString(R.string.okay)) { _, _ ->
                    }
                    .show()
            }
            return
        }
        val action = HotPostsFragmentDirections.actionHotPostsFragmentToPurchasePostFragment(post.id)
        findNavController().navigate(action)
    }

    private fun refresh() {
        postAdapter.refresh()
    }

    companion object {
        private const val TAG = "HotPostsFragment"
    }
}