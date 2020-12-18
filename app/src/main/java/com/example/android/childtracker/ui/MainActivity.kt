package com.example.android.childtracker.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.example.android.childtracker.R
import com.example.android.childtracker.databinding.ActivityMainBinding
import com.example.android.childtracker.ui.viewmodel.MainViewModel
import com.example.android.childtracker.utils.Constants.SHARED_PREFERENCE_FILE_NAME
import com.example.android.childtracker.utils.Constants.SHARED_PREFERENCE_KEY_NAME
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.lang.IllegalStateException

import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel:MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private val shared = getSharedPreferences(SHARED_PREFERENCE_FILE_NAME,Context.MODE_PRIVATE)
    private var profession:Int? = null

    @Inject
    lateinit var auth: FirebaseAuth



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)
        auth.signOut()
        binding.viewModel = viewModel
        binding.registerButton.setOnClickListener {
            registerOnClick()
        }
        binding.viewModel.exceptionLog.observe(this, Observer {
            it?.let {
                checkException(it)
            }
            binding.viewModel.finishCheckException()
        })
        binding.viewModel.movingToActivity.observe(this, Observer {
            if (it!=null){
                var intent:Intent? = null
                if (binding.radioChild.isChecked){
                    intent = Intent(this,ChildActivity::class.java)
                }else{
                    intent = Intent(this,ParentActivity::class.java)
                }
                startActivity(intent)
            }
        })
    }


    private fun checkException(e:Exception){
        when (e){
            is FirebaseAuthInvalidCredentialsException -> email_edit_text.error = "Invalid E-mail. Please try again"
            is FirebaseAuthUserCollisionException-> Toast.makeText(this,"The email address is already in use by another account.",Toast.LENGTH_LONG).show()
            is IllegalStateException-> Toast.makeText(this,"No internet connection",Toast.LENGTH_LONG).show()
        }
    }

    private fun checkEmail(): Boolean {
        return if (binding.viewModel!!.checkEmail(email_edit_text.text.toString())){
            email_edit_text.error = null
            true
        }else{
            email_edit_text.error = "Invalid E-mail. Please try again"
            false
        }
    }

    private fun checkPassword(): Boolean {
        return if (binding.viewModel!!.checkPassword(password_edit_text.text.toString())) {
            password_edit_text.error = null
            true
        } else {
            password_edit_text.error = "Password must be at least 8 characters long"
            false
        }
    }

    private fun registerOnClick(){
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        if (activeNetworkInfo==null){
            Toast.makeText(this,"No internet connection",Toast.LENGTH_LONG).show()
            return
        }
        if (checkEmail() and checkPassword()){
            try {
                val itChild = binding.radioChild.isChecked
                binding.viewModel!!.registerUser(email_edit_text.text.toString(),password_edit_text.text.toString(),itChild)
            }catch (e:Exception){
                Timber.d(e)
                when (e){
                    is FirebaseAuthInvalidCredentialsException ->{
                        email_edit_text.error = "Invalid E-mail. Please try again"
                    }
                }
            }
        }
    }

}