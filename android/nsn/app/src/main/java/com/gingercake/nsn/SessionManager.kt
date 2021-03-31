package com.gingercake.nsn

import com.gingercake.nsn.model.user.User

object SessionManager {
    lateinit var currentUser: User
    var currentPostTitle: String = ""
    var currentPostContent: String = ""
    var currentPostResourceFile: String = ""
}