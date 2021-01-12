package com.example.android.childtracker.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.android.childtracker.R
import com.example.android.childtracker.databinding.FragmentSettingBinding
import com.example.android.childtracker.databinding.FragmentTrackerBinding
import com.example.android.childtracker.ui.MainActivity
import com.example.android.childtracker.ui.viewmodel.SettingViewModel
import com.example.android.childtracker.utils.Constants.REQUEST_IMAGE_PICK
import com.example.android.childtracker.utils.NoChildException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_setting.*
import javax.inject.Inject

@AndroidEntryPoint
class SettingFragment : Fragment(R.layout.fragment_setting) {

    private lateinit var binding: FragmentSettingBinding
    private var currentFile: Uri? = null

    @Inject
    lateinit var auth:FirebaseAuth


    private val viewModel:SettingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingBinding.inflate(inflater,null,false)
        binding.settingViewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
        setListeners()
    }

    private fun setListeners(){
        binding.addButton.setOnClickListener {
            binding.settingViewModel?.addChild(addChild_et.text.toString())
        }

        binding.circleImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).also {
                it.type = "image/*"
            }
            Intent.createChooser(intent,"Выберите фото профиля").also {
                startActivityForResult(it,REQUEST_IMAGE_PICK)
            }
        }

        binding.saveButton.setOnClickListener {
            currentFile?.let {
                binding.settingViewModel?.uploadImageToStorage(auth.currentUser!!.uid,it)
            }
            if (binding.nameEt.text.toString().isNotEmpty()){
                binding.settingViewModel?.saveName(binding.nameEt.text.toString())
            }
        }

        binding.exitButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(),MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun setObservers(){
        binding.settingViewModel?.currentParent!!.observe(requireActivity(), Observer {
            binding.nameEt.setText(it.name)
        })

        binding.settingViewModel?.currentImage!!.observe(requireActivity(), Observer {
            it?.let {
                binding.circleImageView.setImageBitmap(it)
            }
        })

        binding.settingViewModel?.error!!.observe(requireActivity(),{
            if (it!=null) {
                when (it) {
                    is NoChildException -> Toast.makeText(
                        requireContext(),
                        "Введенный id неверный",
                        Toast.LENGTH_LONG
                    ).show()
                    is FirebaseFirestoreException -> Toast.makeText(
                        requireContext(),
                        "Проверьте подключение к интернету",
                        Toast.LENGTH_LONG
                    ).show()
                    else -> Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                }
            }
            binding.settingViewModel?.errorDone()
        })

        binding.settingViewModel?.currentName!!.observe(requireActivity(), Observer {
            it?.let {
                var text = resources.getString(R.string.name_child)
                text += it
                binding.childNameTv.setText(text)
            }
        })
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode==Activity.RESULT_OK && requestCode == REQUEST_IMAGE_PICK){
            data?.data.let {
                binding.circleImageView.setImageURI(it)
            }
        }
    }
}