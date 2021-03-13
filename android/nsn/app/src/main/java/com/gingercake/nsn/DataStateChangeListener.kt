package com.gingercake.nsn

interface DataStateChangeListener {
    fun onDataStateChange(dataState: DataState<*>?)
}