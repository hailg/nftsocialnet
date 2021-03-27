package com.gingercake.nsn.model.user

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val uid: String,
    val email: String,
    val name: String,
    val photoUrl: String,
    val eosAmount: String = "0",
    val publicKey: String = "",
    val username: String = "",
) : Parcelable {
    constructor() : this("", "", "", "")

    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false
        other as User
        if (uid != other.uid) return false
        return true
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }
}
