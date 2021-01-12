package com.example.android.childtracker.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.android.childtracker.R
import com.example.android.childtracker.databinding.ActivityParentBinding
import com.example.android.childtracker.services.TrackingChildService
import com.example.android.childtracker.ui.viewmodel.ParentViewModel
import com.example.android.childtracker.ui.viewmodel.SettingViewModel
import com.example.android.childtracker.utils.Constants.ACTION_CALL_SERVICE
import com.example.android.childtracker.utils.Constants.ACTION_SMS_SERVICE
import com.example.android.childtracker.utils.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.android.childtracker.utils.Constants.ACTION_STOP_SERVICE
import com.mapbox.mapboxsdk.Mapbox
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ParentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParentBinding
    private lateinit var viewModel: ParentViewModel

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.getStringExtra("Action")
            when (action) {
                ACTION_CALL_SERVICE -> {
                    Toast.makeText(this@ParentActivity, "Вы позвонили", Toast.LENGTH_LONG).show()
                    val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                    sendBroadcast(it)
                    val callIntent = Intent(Intent.ACTION_DIAL)
                    callIntent.setData(Uri.fromParts("tel", "+7(918) 533-38-53", null))
                    if (callIntent.resolveActivity(packageManager!!) != null) {
                        startActivity(callIntent)
                    }

                }
                ACTION_SMS_SERVICE -> {
                    Toast.makeText(this@ParentActivity, "Вы написали смс", Toast.LENGTH_LONG).show()
                    val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                    sendBroadcast(it)
                    val smsIntent = Intent(Intent.ACTION_VIEW)
                    smsIntent.setType("vnd.android-dir/mms-sms")
                    // smsIntent.putExtra("address","89185384777")
                    smsIntent.putExtra("sms_body", "Salam")
                    if (smsIntent.resolveActivity(packageManager!!) != null) {
                        startActivity(smsIntent)
                    }
                }
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        binding = DataBindingUtil.setContentView(this, R.layout.activity_parent)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        binding.bottomNavigationView.setupWithNavController(navHostFragment.findNavController())
        registerReceiver(broadcastReceiver, IntentFilter("ParentService"))
        viewModel = ViewModelProvider(this).get(ParentViewModel::class.java)

        SettingViewModel.needRestartServer.observe(this, {
            if (it) {
                CoroutineScope(Dispatchers.Main).launch {
                    sendCommandToService(ACTION_STOP_SERVICE)
                    delay(2000)
                    sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
                }
            }
        })
    }

    private fun sendCommandToService(action: String) {
        Intent(this, TrackingChildService::class.java).also {
            it.action = action
            this.startService(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this,TrackingChildService::class.java))
        //sendCommandToService(ACTION_STOP_SERVICE)
    }
}