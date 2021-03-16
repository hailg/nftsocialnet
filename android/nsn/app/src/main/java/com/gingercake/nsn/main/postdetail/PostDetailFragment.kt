package com.gingercake.nsn.main.postdetail

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.RequestManager
import com.gingercake.nsn.R
import com.gingercake.nsn.framework.BaseChildFragment
import javax.inject.Inject

class PostDetailFragment : BaseChildFragment() {

    @Inject
    lateinit var requestManager: RequestManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val safeArgs: PostDetailFragmentArgs by navArgs()
        Log.d(TAG, "onCreate: ${safeArgs.postId}")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        Log.d(TAG, "onOptionsItemSelected: ")
        return true
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_post_detail, container, false)
    }

    companion object {
        private const val TAG = "PostDetailFragment"
    }
}