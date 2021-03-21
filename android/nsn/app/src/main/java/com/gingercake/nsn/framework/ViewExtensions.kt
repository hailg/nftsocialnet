package com.gingercake.nsn.framework

import android.app.Activity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat


fun Activity.displayToast(
    @StringRes message: Int
){
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Activity.displayToast(
    message: String
){
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Activity.showKeyboard(view: View?) {
    view?.let {
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm!!.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun Activity.hideKeyboard(view: View) {
    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}
interface AreYouSureCallback {

    fun proceed()

    fun cancel()
}