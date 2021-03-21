package com.gingercake.nsn.model.post

import android.os.Parcelable
import com.gingercake.nsn.model.user.User
import kotlinx.parcelize.Parcelize

@Parcelize
data class PostUser(
    val uid: String,
    val name: String,
    val photoUrl: String
) : Parcelable {
    constructor() : this("", "", "")
}

@Parcelize
data class Like(
    val uid: String,
    val name: String,
    val photoUrl: String
) : Parcelable {
    constructor() : this("", "", "")
}

@Parcelize
data class Comment(
    val id: String,
    val author: PostUser,
    val content: String,
    val timestamp: Long,
    val likes: List<String> = emptyList()
) : Parcelable {
    constructor() : this("", PostUser(), "", 0)

    companion object {
        val NO_COMMENT_MAKER = Comment()
    }
}

@Parcelize
data class Post(
    val id: String,
    val title: String,
    val content: String,
    val resource: String,
    val resourceType: Int,
    val timestamp: Long,
    val views: Long = 0,
    val commentCount: Long = 0,
    val rank: Long = 0,
    val price: String = "0.0",
    val owner: PostUser,
    val author: PostUser,
    val likes: List<String> = emptyList(),
)  : Parcelable {
    constructor() : this("", "", "", "", 0, 0, 0, 0, 0, "0.0", PostUser(), PostUser(), emptyList())

    companion object {
        const val IMAGE_TYPE = 0
        const val PROFILE_HEADER_TYPE = 1
        const val NO_POST_TYPE = 2
        const val LINK_TYPE = 3
        const val TEXT_ONLY_TYPE = 4

        val NO_POST_MARKER = Post()

        fun profilePostForUser(user: User) = Post("", "",
            "", "", Post.PROFILE_HEADER_TYPE,
            0, 0, 0,0, "0.0", PostUser(user.uid, user.name, user.photoUrl), PostUser(user.uid, user.name, user.photoUrl), emptyList()
        )

        fun newInstance(id: String,
                        title: String, content: String,
                        resource: String, resourceType: Int,
                        price: String, user: User) =
            Post(id = id, title = title, content = content,
                resource = resource, resourceType = resourceType,
                timestamp = System.currentTimeMillis(),
                price = price,
                owner = PostUser(user.uid, user.name, user.photoUrl),
                author = PostUser(user.uid, user.name, user.photoUrl))
    }
}
