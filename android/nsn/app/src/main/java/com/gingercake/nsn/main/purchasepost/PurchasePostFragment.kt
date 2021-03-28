package com.gingercake.nsn.main.purchasepost

import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.RequestManager
import com.gingercake.nsn.R
import com.gingercake.nsn.databinding.FragmentPurchasePostBinding
import com.gingercake.nsn.framework.BaseChildFragment
import com.gingercake.nsn.framework.Constants
import com.gingercake.nsn.framework.hideKeyboard
import com.gingercake.nsn.main.CreatePostProgress
import com.gingercake.nsn.main.MainActivity
import com.gingercake.nsn.model.post.Post
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.math.RoundingMode
import java.text.DecimalFormat
import javax.inject.Inject

class PurchasePostFragment : BaseChildFragment() {

    @Inject
    lateinit var db: FirebaseFirestore

    @Inject
    lateinit var imageLoader: RequestManager

    private lateinit var binding: FragmentPurchasePostBinding
    private lateinit var df: DecimalFormat
    private val args: PurchasePostFragmentArgs by navArgs()

    private var purchasingPostId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        df = DecimalFormat("0.0000")
        df.roundingMode = RoundingMode.CEILING
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentPurchasePostBinding.inflate(inflater, container, false)
        lifecycleScope.launchWhenStarted {
            db.collection(Constants.POSTS_COLLECTION).document(args.postId).get().await().toObject(Post::class.java)?.let { post ->
                val price = post.price.toDouble()
                val newPrice = price * 1.1
                binding.salePrice.editText?.setText(df.format(newPrice).toString())
                imageLoader
                        .load(post.owner.photoUrl)
                        .dontAnimate()
                        .into(binding.authorImage)
                if (post.resourceType == Post.IMAGE_TYPE && post.resource.isNotEmpty()) {
                    if (post.resource.startsWith("http")) {
                        imageLoader
                                .load(post.resource)
                                .into(binding.postImage)
                    } else {
                        val resourceRef = FirebaseStorage.getInstance().reference.child(post.resource)
                        imageLoader
                                .load(resourceRef)
                                .into(binding.postImage)
                    }
                }
                binding.postTitle.text = post.title
                binding.postContent.text = post.content
                binding.authorName.text = post.owner.name
                binding.updateDate.text = DateUtils.getRelativeDateTimeString(
                        binding.updateDate.context,
                        post.timestamp,
                        DateUtils.SECOND_IN_MILLIS,
                        DateUtils.WEEK_IN_MILLIS,
                        DateUtils.FORMAT_SHOW_TIME
                )
                binding.likeCount.text = post.likes.size.toString()
                binding.commentCount.isVisible = true
                binding.commentCount.text = post.commentCount.toString()
                binding.price.text = "${post.price} EOS"
                binding.buyBtn.setOnClickListener {
                    activity?.hideKeyboard(binding.salePrice)
                    activity?.hideKeyboard(binding.password)
                    binding.salePrice.error = null
                    binding.password.error = null
                    val priceTxt = binding.salePrice.editText?.text.toString()
                    val userPrice = priceTxt.toDoubleOrNull()
                    if (userPrice == null) {
                        binding.salePrice.error = "Invalid sale price"
                        return@setOnClickListener
                    }
                    val password = binding.password.editText?.text.toString()
                    if (password.isEmpty()) {
                        binding.password.error = getString(R.string.please_enter_password_to_buy)
                        return@setOnClickListener
                    }
                    binding.purchaseForm.isVisible = false
                    binding.status.isVisible = true
                    purchasingPostId = post.id
                    (activity as MainActivity).mainViewModel.purchasePost(post, df.format(userPrice), password)
                }
            }
        }

        (activity as MainActivity).mainViewModel.purchaseLiveData.observe(viewLifecycleOwner, {
            Log.d(TAG, "onPostPurchasing change: $it")
            if (it.post.id != purchasingPostId) {
                return@observe
            }
            when (it.state) {
                CreatePostProgress.SUCCESS -> {
                    purchasingPostId = ""
                    findNavController().popBackStack()
                }
                CreatePostProgress.FAIL -> {
                    binding.purchaseForm.isVisible = true
                    binding.status.isVisible = false
                    activity?.let { activity ->
                        MaterialAlertDialogBuilder(activity)
                            .setTitle(resources.getString(R.string.app_name))
                            .setMessage(it.errorMessage)
                            .setPositiveButton(resources.getString(R.string.retry)) { _, _ ->
                            }
                            .show()
                    }
                    purchasingPostId = ""
                }
            }
        })
        return binding.root
    }

    companion object {
        private const val TAG = "PurchasePostFragment"
    }
}