package com.gingercake.nsn.main.newpost

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.aminography.choosephotohelper.ChoosePhotoHelper
import com.bumptech.glide.RequestManager
import com.gingercake.nsn.R
import com.gingercake.nsn.databinding.FragmentNewPostBinding
import com.gingercake.nsn.framework.BaseChildFragment
import javax.inject.Inject

class NewPostFragment : BaseChildFragment() {

    @Inject
    lateinit var requestManager: RequestManager

    private lateinit var binding: FragmentNewPostBinding
    private lateinit var choosePhotoHelper: ChoosePhotoHelper
    private var photoFile: String? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        choosePhotoHelper = ChoosePhotoHelper.with(this)
            .asFilePath()
            .alwaysShowRemoveOption(false)
            .build  {
                Log.d(TAG, "onPhotoPicked: $it")
                it?.let {
                    photoFile = it
                    requestManager
                        .load(it)
                        .into(binding.postImage)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNewPostBinding.inflate(inflater, container, false)
        binding.attachBtn.setOnClickListener {
            choosePhotoHelper.showChooser()
        }
        binding.nextBtn.setOnClickListener {
            binding.title.error = null
            if (binding.title.editText?.text.isNullOrBlank()) {
                binding.title.error = getString(R.string.title_is_required)
                return@setOnClickListener
            }
            val action = NewPostFragmentDirections.actionNewPostFragmentToNewPostSaleSettingFragment(
                binding.title.editText?.text.toString(),
                binding.content.editText?.text.toString(),
                photoFile ?: ""
            )
            findNavController().navigate(action)
        }
        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        choosePhotoHelper.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        choosePhotoHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        choosePhotoHelper.onSaveInstanceState(outState)
    }

    companion object {
        private const val TAG = "NewPostFragment"
    }
}