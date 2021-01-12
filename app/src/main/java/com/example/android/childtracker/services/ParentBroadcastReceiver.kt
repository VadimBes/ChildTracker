package com.example.android.childtracker.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.android.childtracker.utils.Constants.ACTION_SMS_SERVICE

class ParentBroadcastReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action ==  ACTION_SMS_SERVICE ){
            Log.d("MuTag","In")
            val smsIntent = Intent(Intent.ACTION_SENDTO)
            smsIntent.setData(Uri.parse("89185333853"))
            smsIntent.putExtra("sms_body","Salam")
            if(smsIntent.resolveActivity(context?.packageManager!!)!=null){
                Log.d("MuTag","Salam")
                context.startActivity(smsIntent)
            }
        }
//        val myintent = Intent("ParentService").apply {
//            action = intent?.action
//        }
//        context?.sendBroadcast(myintent)
    }
}