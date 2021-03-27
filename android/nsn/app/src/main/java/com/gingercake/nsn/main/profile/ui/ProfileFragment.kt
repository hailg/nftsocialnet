package com.gingercake.nsn.main.profile.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.bumptech.glide.RequestManager
import com.gingercake.nsn.SessionManager
import com.gingercake.nsn.databinding.FragmentProfileBinding
import com.gingercake.nsn.framework.displayToast
import com.gingercake.nsn.main.CreatePostProgress
import com.gingercake.nsn.main.MainActivity
import com.gingercake.nsn.main.MainViewModel
import com.gingercake.nsn.main.MainViewModelFactory
import com.gingercake.nsn.model.post.Post
import com.gingercake.nsn.model.user.User
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject


class ProfileFragment : DaggerFragment(), ProfilePagingAdapter.Interaction {
    companion object {
        private const val TAG = "PostsFragment"
        private const val REQUEST_RESOURCE_CODE = 1001
    }

    @Inject
    lateinit var requestManager: RequestManager

    @Inject
    lateinit var mainViewModelFactory: MainViewModelFactory

    private lateinit var owner: User
    private lateinit var viewModel: ProfileViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var pagingAdapter: ProfilePagingAdapter
    private lateinit var binding: FragmentProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, mainViewModelFactory).get(ProfileViewModel::class.java)
        mainViewModel = (activity as MainActivity).mainViewModel
        owner = SessionManager.currentUser
        Log.d(TAG, "onCreate: viewModel $viewModel")
        Log.d(TAG, "onCreate: mainViewModel $mainViewModel")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileBinding.bind(view)
        binding.swipeRefresh.setOnRefreshListener {
            refresh()
            binding.swipeRefresh.isRefreshing = false
        }
        binding.fab.setOnClickListener {
            val action = ProfileFragmentDirections.actionProfileFragmentToNewPostFragment()
            findNavController().navigate(action)
        }
        mainViewModel.accountBalanceLiveData.observe(viewLifecycleOwner, { balance ->
            SessionManager.currentUser.eosAmount = balance
        })
        mainViewModel.postCreationLiveData.observe(viewLifecycleOwner, {
            Log.d(TAG, "onPostCreation change: $it")
            when (it.state) {
                CreatePostProgress.SUCCESS -> refresh()
                CreatePostProgress.FAIL -> activity?.displayToast("Failed to create post. Please try again!")
            }
        })
        refresh()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onItemSelected(position: Int, item: Post) {
        Log.d(TAG, "onItemSelected: ${item.title}")
        if (item.resourceType == Post.PROFILE_HEADER_TYPE || item.resourceType == Post.NO_POST_TYPE) {
            return
        }
        val action = ProfileFragmentDirections.actionProfileFragmentToPostDetailFragment(item.id)
        findNavController().navigate(action)
    }

    private fun refresh() {
        pagingAdapter = ProfilePagingAdapter(viewModel, this, requestManager)
        binding.postRecyclerview.adapter = pagingAdapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profileFlow(owner).collectLatest { pagingData ->
                pagingAdapter.submitData(pagingData)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            pagingAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.progressBarLoadMore.isVisible = loadStates.append is LoadState.Loading
//                binding.progressBar.isVisible = loadStates.refresh  is  LoadState.Loading
                binding.swipeRefresh.isRefreshing = loadStates.refresh  is  LoadState.Loading
            }
        }
        mainViewModel.getAccountBalance()
    }
}