package com.gingercake.nsn.framework

import android.content.Context
import com.gingercake.nsn.main.MainActivity
import dagger.android.support.DaggerFragment

open class BaseChildFragment : DaggerFragment() {

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onDetach() {
        (activity as MainActivity).showBottomNavigation()
        super.onDetach()
    }

    fun createPost(title: String, content: String,
                   resourcePath: String, resourceType: Int, price: String) {
        activity?.let {
            (activity as MainActivity).createPost(title, content, resourcePath, resourceType, price)
        }
    }
}