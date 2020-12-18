package com.example.android.childtracker.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.android.childtracker.R
import com.example.android.childtracker.ui.ChildActivity
import com.example.android.childtracker.ui.ParentActivity
import com.example.android.childtracker.utils.Constants
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.android.core.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.ref.WeakReference

class ChildLocationService: LifecycleService() {
    var isFirstRun = true
    private var locationEngine: LocationEngine? = null
    private val callback = LocationChangeListeningActivityLocationCallback()
    private var childCollectionRef: CollectionReference = Firebase.firestore.collection("children")

    companion object{
        val childLocation = MutableLiveData<Location>()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                Constants.ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                        initLocationEngine()
                        isFirstRun = false
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("MissingPermission")
    private fun initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        val request = LocationEngineRequest.Builder(Constants.DEFAULT_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(Constants.DEFAULT_MAX_WAIT_TIME).build()
        locationEngine!!.requestLocationUpdates(request, callback, mainLooper)
        locationEngine!!.getLastLocation(callback)
    }

    private fun startForegroundService() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotification(notificationManager)
        }

        val notificationBuilder = NotificationCompat.Builder(this,
            Constants.NOTIFICATION_CHILD_CHANNEL_ID
        )
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_baseline_child_care_24)
            .setContentTitle("Child Tracker")
            .setContentText("Вы находитесь в разрешенной зоне")
            .setContentIntent(getMainActivityPendingIntent())

        startForeground(Constants.NOTIFICATION_CHILD_SERVICE_ID, notificationBuilder.build())
    }

    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, ChildActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHILD_CHANNEL_ID,
            Constants.NOTIFICATION_CHILD_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    fun changeLocation(location: Location) = CoroutineScope(Dispatchers.IO).launch {
        try {
            childCollectionRef.document("child1")
                .update("location", GeoPoint(location.latitude, location.longitude))
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Timber.d(e)
            }
        }
    }

    private inner class LocationChangeListeningActivityLocationCallback() :
        LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult) {

            val location = result.lastLocation ?: return

            location.let {
                childLocation.postValue(it)
                changeLocation(it)
            }

        }

        override fun onFailure(exception: Exception) {
            Timber.d("Failure")
        }

    }
}

