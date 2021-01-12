package com.example.android.childtracker.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.example.android.childtracker.R
import com.example.android.childtracker.data.entities.Person
import com.example.android.childtracker.databinding.ActivityMainBinding
import com.example.android.childtracker.ui.viewmodel.MainViewModel
import com.example.android.childtracker.ui.viewmodel.SettingViewModel
import com.example.android.childtracker.utils.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.example.android.childtracker.utils.Constants.SHARED_PREFERENCE_FILE_NAME
import com.example.android.childtracker.utils.Constants.SHARED_PREFERENCE_KEY_NAME
import com.example.android.childtracker.utils.PermissionUtility
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import java.lang.IllegalStateException

import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        Timber.d(auth.currentUser?.uid)
        requestPermissions()
        binding.viewModel = viewModel
        binding.registerButton.setOnClickListener {
            val intent = Intent(this,RegisterActivity::class.java)
            startActivity(intent)
        }
        binding.viewModel!!.exceptionLog.observe(this, Observer {
            it?.let {
                checkException(it)
            }
            binding.viewModel!!.finishCheckException()
        })
        binding.viewModel!!.movingToActivity.observe(this, Observer {
            if (it != -1) {
                var intent: Intent? = null
                if (it == 0) {
                    intent = Intent(this, ChildActivity::class.java)
                } else {
                    if (it == 1) intent = Intent(this, ParentActivity::class.java)
                }
                binding.viewModel?.finishMoving()
                intent?.let {
                    startActivity(it)
                    finish()
                }
            }
        })
        binding.loginButton.setOnClickListener {
            loginOnClick()
        }


    }



    private fun checkException(e: Exception) {
        when (e) {
            is FirebaseAuthInvalidCredentialsException -> email_edit_text.error =
                "Invalid E-mail. Please try again"
            is FirebaseAuthUserCollisionException -> Toast.makeText(
                this,
                "The email address is already in use by another account.",
                Toast.LENGTH_LONG
            ).show()
            is IllegalStateException -> Toast.makeText(
                this,
                "No internet connection",
                Toast.LENGTH_LONG
            ).show()
            is FirebaseAuthInvalidUserException->Toast.makeText(
                this,
                "Такой пользователь не зарегистрирован",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun checkEmail(): Boolean {
        return if (binding.viewModel!!.checkEmail(email_edit_text.text.toString())) {
            email_edit_text.error = null
            true
        } else {
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

    private fun loginOnClick() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        if (activeNetworkInfo == null) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show()
            return
        }
        if (checkEmail() and checkPassword()) {
            try {
                binding.viewModel!!.loginUser(
                    email_edit_text.text.toString(),
                    password_edit_text.text.toString()
                )
            } catch (e: Exception) {
                Timber.d(e)
            }
        }
    }



        private fun requestPermissions() {
        if (PermissionUtility.hasPermission(this)) {
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "Вы должны принять разрешения для отслеживания местоположения, что-бы использовать это приложение",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )

        } else {
            EasyPermissions.requestPermissions(
                this,
                "Вы должны принять разрешения для отслеживания местоположения, что-бы использовать это приложение",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }

    }


    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermissions()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

}