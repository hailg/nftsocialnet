package com.gingercake.nsn.main.newpost

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gingercake.nsn.R
import com.gingercake.nsn.databinding.FragmentNewPostSaleSettingBinding
import com.gingercake.nsn.framework.BaseChildFragment
import com.gingercake.nsn.framework.hideKeyboard
import com.gingercake.nsn.framework.showKeyboard
import com.gingercake.nsn.main.CreatePostProgress
import com.gingercake.nsn.main.MainActivity
import com.gingercake.nsn.model.post.Post
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import javax.inject.Inject

class NewPostSaleSettingFragment : BaseChildFragment() {
    private lateinit var binding: FragmentNewPostSaleSettingBinding
    private lateinit var df: DecimalFormat

    private val args: NewPostSaleSettingFragmentArgs by navArgs()
    private var creatingPostId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        df = DecimalFormat("0.0000")
        df.roundingMode = RoundingMode.CEILING
        Log.d(TAG, "onCreate: ${args.postTitle}")
        Log.d(TAG, "onCreate: ${args.postContent}")
        Log.d(TAG, "onCreate: ${args.resourcePath}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNewPostSaleSettingBinding.inflate(inflater, container, false)
        binding.salePrice.isVisible = false
        binding.password.isVisible = false
        binding.saleSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            binding.salePrice.isVisible = isChecked
            binding.password.isVisible = isChecked
            if (isChecked) {
                binding.salePrice.requestFocus()
                activity?.showKeyboard(binding.salePrice.editText)
            } else {
                activity?.hideKeyboard(binding.salePrice)
            }
        }
        binding.postBtn.setOnClickListener {
            activity?.hideKeyboard(binding.salePrice)
            binding.postForm.isVisible = false
            binding.status.isVisible = true
            binding.salePrice.error = null
            binding.password.error = null
            val price = if (binding.saleSwitch.isChecked) {
                val userPriceText = binding.salePrice.editText?.text.toString()
                val userPrice = userPriceText?.toDoubleOrNull()
                if (userPrice == null) {
                    binding.salePrice.error = "Invalid sale price"
                    return@setOnClickListener
                }
                df.format(userPrice)
            } else "-1"
            val password = if (binding.saleSwitch.isChecked) {
                val userPassword = binding.password.editText?.text.toString()
                if (userPassword.isEmpty()) {
                    binding.password.error = getString(R.string.please_enter_password_to_sell)
                    return@setOnClickListener
                }
                userPassword
            } else ""
            creatingPostId =  UUID.randomUUID().toString().replace("-", "")
            (activity as MainActivity).createPost(
                creatingPostId,
                args.postTitle,
                args.postContent,
                args.resourcePath,
                if (args.resourcePath.isBlank()) Post.TEXT_ONLY_TYPE else Post.IMAGE_TYPE,
                price, password
            )
        }

        (activity as MainActivity).mainViewModel.postCreationLiveData.observe(viewLifecycleOwner, {
            Log.d(TAG, "onPostCreation change: $it")
            if (it.post.id != creatingPostId) {
                return@observe
            }
            when (it.state) {
                CreatePostProgress.SUCCESS -> {
                    creatingPostId = ""
                    findNavController().popBackStack()
                    findNavController().popBackStack()
                }
                CreatePostProgress.FAIL -> {
                    binding.postForm.isVisible = true
                    binding.status.isVisible = false
                    activity?.let { activity ->
//                        (activity as MainActivity).deletePost(creatingPostId)
                        MaterialAlertDialogBuilder(activity)
                        .setTitle(resources.getString(R.string.app_name))
                            .setMessage(it.errorMessage)
                            .setPositiveButton(resources.getString(R.string.retry)) { _, _ ->
                            }
                            .show()
                    }
                    creatingPostId = ""
                }
            }
        })
        return binding.root
    }

    companion object {
        private const val TAG = "NewPostSaleSettingFragm"
    }
}