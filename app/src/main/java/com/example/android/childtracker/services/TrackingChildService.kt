package com.example.android.childtracker.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getService
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.android.childtracker.R
import com.example.android.childtracker.data.entities.Child
import com.example.android.childtracker.data.entities.Parent
import com.example.android.childtracker.ui.ParentActivity
import com.example.android.childtracker.utils.Constants.ACTION_CALL_SERVICE
import com.example.android.childtracker.utils.Constants.ACTION_PAUSE_SERVICE
import com.example.android.childtracker.utils.Constants.ACTION_SMS_SERVICE
import com.example.android.childtracker.utils.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.android.childtracker.utils.Constants.ACTION_STOP_SERVICE
import com.example.android.childtracker.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.example.android.childtracker.utils.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.android.childtracker.utils.Constants.NOTIFICATION_CHILD_ID
import com.example.android.childtracker.utils.Extension.toShortUID
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
import timber.log.Timber
import java.lang.StringBuilder
import javax.inject.Inject

@AndroidEntryPoint
class TrackingChildService : LifecycleService() {

    private var firebaseAuth = FirebaseAuth.getInstance()

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    private lateinit var curNotificationBuilder: NotificationCompat.Builder

    private var parentCollectionRef: CollectionReference = Firebase.firestore.collection("parents")
    private var childCollectionRef: CollectionReference = Firebase.firestore.collection("children")

    private var subscribeFlag = false
    private var listenerChild: ListenerRegistration? = null
    private var listenerParent: ListenerRegistration? = null

    private lateinit var searchPointFeatureCollection: FeatureCollection
    private var drawnPolygon: Polygon? = null
    private var freehandTouchPointListForPolygon: ArrayList<Point> = ArrayList()
    private val inZone = MutableLiveData<Boolean>()

    companion object {
        val isTracking = MutableLiveData<Boolean>()
        val locationChild = MutableLiveData<Child?>()
    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        locationChild.postValue(null)
    }

    override fun onCreate() {
        super.onCreate()
        curNotificationBuilder = baseNotificationBuilder
        postInitialValues()
    }

//    override fun onTaskRemoved(rootIntent: Intent?) {
//        super.onTaskRemoved(rootIntent)
//        listenerChild?.remove()
//        listenerParent?.remove()
//        drawnPolygon = null
//        this.stopSelf()
//    }

    private var isFirstRun = true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                        isTracking.value = true
                        setLocationSubscriber()
                        isFirstRun = false
                    }
                }
                ACTION_STOP_SERVICE -> {
                    listenerChild?.remove()
                    listenerParent?.remove()
                    drawnPolygon = null
                    this.stopSelf()
                }

            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun setLocationSubscriber() {
        listenerParent = parentCollectionRef.document(firebaseAuth.currentUser!!.uid.toShortUID())
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                firebaseFirestoreException?.let {
                    return@addSnapshotListener
                }
                querySnapshot?.let {
                    val parent = it.toObject<Parent>()
                    if (!subscribeFlag) {
                        parent?.childId?.let { id ->
                            setChildListener(id)
                        }
                    }
                    parent?.polygon?.let { arrayList ->
                        freehandTouchPointListForPolygon = arrayListOf()
                        arrayList.forEach { geopoint ->
                            geopoint?.let { point ->
                                freehandTouchPointListForPolygon.add(
                                    Point.fromLngLat(
                                        point.longitude,
                                        point.latitude
                                    )
                                )
                                val polygonList: MutableList<List<Point>> =
                                    ArrayList()
                                polygonList.add(freehandTouchPointListForPolygon)
                                if (polygonList.isNotEmpty()){
                                    drawnPolygon = Polygon.fromLngLats(polygonList)
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun setChildListener(childId: String) {
        if (listenerChild == null) {
            listenerChild = childCollectionRef.document(childId)
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    firebaseFirestoreException?.let {
                        return@addSnapshotListener
                    }
                    querySnapshot?.let {
                        val child = it.toObject<Child>()
                        child?.let { children ->
                            locationChild.postValue(child)
                            drawnPolygon?.let {
                                checkChildInArea(children.location)
                            }
                        }
                    }
                }
        }
    }

    private fun checkChildInArea(geoPoint: GeoPoint) {
        if (drawnPolygon!=null){
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
                inZone.postValue(false)
            } else {
                inZone.postValue(true)
            }
        }
    }


    private fun startForegroundService() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotification(notificationManager)
        }
        startForeground(NOTIFICATION_CHILD_ID, baseNotificationBuilder
            .setContentText("Ожидаем ответ от устройства ребенка...").build())

        inZone.observe(this, {
            updateNotificationTrackingState(it)
        })
    }

    private fun updateNotificationTrackingState(inZone: Boolean) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationContentText = if (inZone) "Ваш ребенок находиться в разрешенной зоне" else "Ваш ребенок покинул разрешенную зону"
        curNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(curNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }

        if (inZone){
            val stopIntent = Intent(this,TrackingChildService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            val pendingIntent = getService(this,1,stopIntent, FLAG_UPDATE_CURRENT)
            curNotificationBuilder = baseNotificationBuilder
                .addAction(R.drawable.ic_baseline_stop_24,"Stop",pendingIntent)
        }else {
            val callIntent = Intent("ParentService")
            callIntent.putExtra("Action", ACTION_CALL_SERVICE)
            val smsIntent = Intent("ParentService")
            smsIntent.putExtra("Action", ACTION_SMS_SERVICE)
            val background = BitmapFactory.decodeResource(this.resources,R.drawable.background_not)
            val callPendingIntent = PendingIntent.getBroadcast(this,2,callIntent,
                FLAG_UPDATE_CURRENT)
            val smsPendingIntent = PendingIntent.getBroadcast(this,3,smsIntent,
                FLAG_UPDATE_CURRENT)
            curNotificationBuilder = baseNotificationBuilder
                .addAction(R.drawable.ic_baseline_local_phone_24,"Call",callPendingIntent)
                .addAction(R.drawable.ic_baseline_email_24,"SMS",smsPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setDefaults(Notification.DEFAULT_VIBRATE)
        }
        curNotificationBuilder.setContentText(notificationContentText)
        notificationManager.notify(NOTIFICATION_CHILD_ID,curNotificationBuilder.build())
    }


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