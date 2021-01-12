package com.example.android.childtracker.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.childtracker.data.entities.Parent
import com.example.android.childtracker.utils.Extension.toGeoPointList
import com.example.android.childtracker.utils.Extension.toShortUID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber


class ParentViewModel : ViewModel() {


    private var auth: FirebaseAuth = FirebaseAuth.getInstance()


    companion object {
        private val _touchPointListForService = MutableLiveData<ArrayList<Point>?>()
        val touchPointListForService: LiveData<ArrayList<Point>?>
            get() = _touchPointListForService
    }

    private val _needFirstStartService = MutableLiveData<Boolean>()
    val needFirstStartService:LiveData<Boolean>
    get() = _needFirstStartService


    private var parentCollectionRef: CollectionReference = Firebase.firestore.collection("parents")

    fun clearPolygon() = CoroutineScope(Dispatchers.IO).launch {
        try {
            parentCollectionRef.document(auth.currentUser?.uid!!.toShortUID())
                .update("polygon", ArrayList<GeoPoint>()).await()
        } catch (e: Exception) {
            Timber.d(e)
        }
    }


    fun saveGeoPoint(arrayPoints: ArrayList<Point>) = CoroutineScope(Dispatchers.IO).launch {
        try {
            parentCollectionRef.document(auth.currentUser!!.uid.toShortUID())
                .update("polygon", arrayPoints.toGeoPointList()).await()
        } catch (e: Exception) {
            Timber.d(e)
        }
    }

    fun checkChild() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val query = parentCollectionRef.document(auth.currentUser!!.uid.toShortUID()).get().await()
            val parent = query.toObject(Parent::class.java)
            parent?.let {
                if (it.childId!=""){
                    withContext(Dispatchers.Main){
                        _needFirstStartService.value = true
                    }
                }
            }

        } catch (e: Exception) {
            Timber.d(e)
        }
    }

    fun finishCheck(){
        _needFirstStartService.value = false
    }


}