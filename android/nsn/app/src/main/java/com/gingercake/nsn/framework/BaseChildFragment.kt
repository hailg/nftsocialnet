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
}