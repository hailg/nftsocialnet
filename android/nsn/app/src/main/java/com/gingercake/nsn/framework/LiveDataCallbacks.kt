package com.gingercake.nsn.framework

import com.google.firebase.firestore.DocumentSnapshot

interface OnLastVisibleProductCallback {
    fun setLastVisibleProduct(lastVisibleDocument: DocumentSnapshot)
}

interface OnLastProductReachedCallback {
    fun setLastProductReached(isLastProductReached: Boolean)
}
