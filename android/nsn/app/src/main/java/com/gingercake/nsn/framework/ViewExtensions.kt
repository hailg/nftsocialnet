package com.gingercake.nsn.framework

import android.app.Activity
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

fun Activity.displayToast(
    @StringRes message:Int
){
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Activity.displayToast(
    message:String
){
    Toast.makeText(this,message, Toast.LENGTH_LONG).show()
}


interface AreYouSureCallback {

    fun proceed()

    fun cancel()
}