package com.example.android.childtracker.services

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.android.childtracker.R
import com.example.android.childtracker.data.entities.Child
import com.example.android.childtracker.data.entities.Parent
import com.example.android.childtracker.ui.ParentActivity
import com.example.android.childtracker.utils.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.android.childtracker.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.example.android.childtracker.utils.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.android.childtracker.utils.Constants.NOTIFICATION_CHILD_ID
import com.example.android.childtracker.utils.Extension.toPointList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.turf.TurfJoins
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


class TrackingChildService : LifecycleService() {

    private var firebaseAuth = FirebaseAuth.getInstance()

    private var personCollectionRef: CollectionReference = Firebase.firestore.collection("parents")
    private var childCollectionRef: CollectionReference = Firebase.firestore.collection("children")

    private var subscribeFlag = false
    var listener: ListenerRegistration? = null

    lateinit var searchPointFeatureCollection: FeatureCollection
    private var drawnPolygon: Polygon? = null
    private var freehandTouchPointListForPolygon: ArrayList<Point> = ArrayList()

    companion object {
        val isTracking = MutableLiveData<Boolean>()
        val locationChild = MutableLiveData<Child?>()
        val polygonPoint = MutableLiveData<ArrayList<GeoPoint?>?>()
    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        locationChild.postValue(null)
        polygonPoint.postValue(arrayListOf())
    }

    var isFirstRun = true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                        isFirstRun = false
                    }
                    setLocationSubscriber()
                }
            }
        }
        postInitialValues()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun setLocationSubscriber() {
        personCollectionRef.document(firebaseAuth.currentUser!!.uid)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                firebaseFirestoreException?.let {
                    Log.d("MyTag", it!!.message)
                    return@addSnapshotListener
                }
                querySnapshot?.let {
                    val parent = it.toObject<Parent>()
                    if (!subscribeFlag) {
                        parent?.childId?.let {
                            setChildListener(it)
                        }
                    }
                    parent?.polygon?.let { arrayList ->
                        polygonPoint.postValue(arrayList)
                        freehandTouchPointListForPolygon = arrayListOf()
                        arrayList.forEach { geopoint ->
                            geopoint?.let {
                                freehandTouchPointListForPolygon.add(
                                    Point.fromLngLat(
                                        it.longitude,
                                        it.latitude
                                    )
                                )
                                val polygonList: MutableList<List<Point>> =
                                    ArrayList()
                                polygonList.add(freehandTouchPointListForPolygon)
                                drawnPolygon = Polygon.fromLngLats(polygonList)
                            }
                        }
                    }
                    Log.d("MyTag", parent!!.name)
                }
            }
    }

    private fun setChildListener(childId: String) {
        if (listener == null) {
            listener = childCollectionRef.document("child1")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    firebaseFirestoreException?.let {
                        Log.d("MyTag", it!!.message)
                        return@addSnapshotListener
                    }
                    querySnapshot?.let {
                        val child = it.toObject<Child>()
                        child?.let { children ->
                            locationChild.postValue(child)
                            drawnPolygon?.let {
                                Log.d("MuTag", "в snapchot")
                                checkChildInArea(children.location)
                            }
                        }
                    }
                }
        }
    }

    private fun checkChildInArea(geoPoint: GeoPoint) {
        Log.d("MuTag", "Вызов check")
        val feature = Feature.fromGeometry(Point.fromLngLat(geoPoint.longitude, geoPoint.latitude))
        searchPointFeatureCollection = FeatureCollection.fromFeature(feature)

        val collection = TurfJoins.pointsWithinPolygon(
            searchPointFeatureCollection,
            FeatureCollection.fromFeature(
                Feature.fromGeometry(
                    drawnPolygon
                )
            )
        )
        if (collection.features()?.size!! < 1) {
            Log.d("MuTag", "Не в зоне")
        } else {
            Log.d("MuTag", "В зоне")
        }

    }

    private fun startForegroundService() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotification(notificationManager)
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_baseline_child_care_24)
            .setContentTitle("Child Tracker")
            .setContentText("Ваш ребенок находиться в разрешенной зоне")
            .setContentIntent(getMainActivityPendingIntent())

        startForeground(NOTIFICATION_CHILD_ID, notificationBuilder.build())
    }

    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, ParentActivity::class.java),
        FLAG_UPDATE_CURRENT
    )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}