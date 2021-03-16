package com.gingercake.nsn.model.user

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Post(
    val id: String,
    val author: String,
    val owner: String,
    val title: String,
    val content: String,
    val resource: String,
    val resourceType: Int,
    val timestamp: Long,
    val views: Long,
    val commentCount: Long,
    val thumbUps: Long,
    val thumbDowns: Long,
    val rank: Long,
    val price: Double
)  : Parcelable {
    constructor() : this("", "", "","", "", "", 0, 0, 0, 0, 0, 0, 0, 0.0)

    companion object {
        const val IMAGE_TYPE = 0
        const val PROFILE_HEADER_TYPE = 1
        const val NO_POST_TYPE = 2
    }
}
