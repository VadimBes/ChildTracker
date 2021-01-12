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
import com.example.android.childtracker.databinding.ActivityRegisterBinding
import com.example.android.childtracker.ui.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.email_edit_text
import kotlinx.android.synthetic.main.activity_main.password_edit_text
import kotlinx.android.synthetic.main.activity_register.*
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.slots.PredefinedSlots
import ru.tinkoff.decoro.watchers.MaskFormatWatcher
import timber.log.Timber
import java.lang.IllegalStateException

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding:ActivityRegisterBinding
    private val viewModel: MainViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_register)
        binding.viewModel = viewModel
        val watcher = MaskFormatWatcher(MaskImpl.createTerminated(PredefinedSlots.RUS_PHONE_NUMBER))
        watcher.installOn(phone_edit_text)
        binding.registerButton.setOnClickListener {
            registerOnClick()
        }
        binding.viewModel!!.exceptionLog.observe(this, Observer {
            it?.let {
                checkException(it)
            }
            binding.viewModel!!.finishCheckException()
        })
        binding.viewModel!!.movingToActivityLogin.observe(this,{
            if (it){
                val intent = Intent(this,MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        })

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
            is FirebaseAuthInvalidUserException -> Toast.makeText(
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

    private fun checkPhone(str:String):Boolean{
        val phone = Regex(pattern = """((8|\+7)[\- ]?)?(\(?\d{3}\)?[\- ]?)?[\d\- ]{7,10}$""").find(str)?.value
        if (phone!=null) {
            phone_edit_text.error = null
            return true
        }
        else {
            phone_edit_text.error = "Invalid phone"
            return false
        }
    }

    private fun registerOnClick() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        if (activeNetworkInfo == null) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show()
            return
        }
        Timber.d(binding.phoneEditText.text.toString())
        if (checkEmail() and checkPassword() and checkPhone(binding.phoneEditText.text.toString())) {
            try {
                val itChild = binding.radioChild.isChecked
                binding.viewModel!!.registerUser(
                    email_edit_text.text.toString(),
                    password_edit_text.text.toString(),
                    name_edit_text.text.toString(),
                    phone_edit_text.text.toString(),
                    itChild
                )
            } catch (e: Exception) {
                Timber.d(e)
            }
        }

    }


}