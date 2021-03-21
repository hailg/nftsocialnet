package com.gingercake.nsn.main.newpost

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gingercake.nsn.R
import com.gingercake.nsn.databinding.FragmentNewPostSaleSettingBinding
import com.gingercake.nsn.framework.BaseChildFragment
import com.gingercake.nsn.framework.displayToast
import com.gingercake.nsn.framework.hideKeyboard
import com.gingercake.nsn.framework.showKeyboard
import com.gingercake.nsn.main.MainActivity
import com.gingercake.nsn.model.post.Post
import java.math.RoundingMode
import java.text.DecimalFormat

class NewPostSaleSettingFragment : BaseChildFragment() {

    private lateinit var binding: FragmentNewPostSaleSettingBinding
    private lateinit var df: DecimalFormat

    private val args: NewPostSaleSettingFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        df = DecimalFormat("#.####")
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
        binding.saleSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            binding.salePrice.isVisible = isChecked
            if (isChecked) {
                binding.salePrice.requestFocus()
                activity?.showKeyboard(binding.salePrice.editText)
            } else {
                activity?.hideKeyboard(binding.salePrice)
            }
        }
        binding.postBtn.setOnClickListener {
            activity?.hideKeyboard(binding.salePrice)
            binding.salePrice.error = null
            val price = if (binding.saleSwitch.isChecked) {
                val userPriceText = binding.salePrice.editText?.text.toString()
                val userPrice = userPriceText?.toDoubleOrNull()
                if (userPrice == null) {
                    binding.salePrice.error = "Invalid sale price"
                    return@setOnClickListener
                }
                df.format(userPrice)
            } else "-1"

            (activity as MainActivity).createPost(
                args.postTitle,
                args.postContent,
                args.resourcePath,
                if (args.resourcePath.isBlank()) Post.TEXT_ONLY_TYPE else Post.IMAGE_TYPE,
                price
            )
            findNavController().popBackStack()
            findNavController().popBackStack()
        }
        return binding.root
    }

    companion object {
        private const val TAG = "NewPostSaleSettingFragm"
    }
}