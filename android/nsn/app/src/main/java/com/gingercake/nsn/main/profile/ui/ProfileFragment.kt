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
import com.gingercake.nsn.main.MainViewModelFactory
import com.gingercake.nsn.model.user.Post
import com.gingercake.nsn.model.user.User
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject


class ProfileFragment : DaggerFragment(), ProfilePagingAdapter.Interaction {
    @Inject
    lateinit var requestManager: RequestManager
    @Inject
    lateinit var mainViewModelFactory: MainViewModelFactory

    lateinit var owner: User
    private lateinit var viewModel: ProfileViewModel
    private lateinit var pagingAdapter: ProfilePagingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, mainViewModelFactory).get(ProfileViewModel::class.java)
        owner = SessionManager.currentUser
        Log.d(TAG, "onCreate: viewModel ${viewModel}")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentProfileBinding.bind(view)
        pagingAdapter = ProfilePagingAdapter(this, requestManager)
        binding.postRecyclerview.adapter = pagingAdapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profileFlow(owner).collectLatest { pagingData ->
                pagingAdapter.submitData(pagingData)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            pagingAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.progressBarLoadMore.isVisible = loadStates.append is LoadState.Loading
                binding.progressBar.isVisible = loadStates.refresh  is  LoadState.Loading
            }
        }

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        private const val TAG = "PostsFragment"
    }

    override fun onItemSelected(position: Int, item: Post) {
        Log.d(TAG, "onItemSelected: ${item.title}")
        if (item.resourceType == Post.PROFILE_HEADER_TYPE || item.resourceType == Post.NO_POST_TYPE) {
            return
        }
        val action = ProfileFragmentDirections.actionPostsFragmentToPostDetailFragment(item.id)
        findNavController().navigate(action)
    }
}