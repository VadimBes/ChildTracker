package com.example.android.childtracker.ui.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.childtracker.data.entities.Parent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject


class ChildViewModel : ViewModel() {


    private var auth: FirebaseAuth = FirebaseAuth.getInstance()


    private val _polygonPoints = MutableLiveData<ArrayList<GeoPoint?>>()
    val polygonPoints: LiveData<ArrayList<GeoPoint?>>
        get() = _polygonPoints


    private var personCollectionRef: CollectionReference = Firebase.firestore.collection("parents")


    private var childCollectionRef: CollectionReference = Firebase.firestore.collection("children")

    lateinit var currentUser: FirebaseUser

    init {
        subscribeToPolygonParent()
    }




    private fun addParent(parentUID: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            childCollectionRef.document(currentUser.uid).update("parentId", parentUID).await()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Timber.d(e)
            }
        }
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


    @Suppress("UNREACHABLE_CODE")
    private fun subscribeToPolygonParent() {
        personCollectionRef.document("Pa5dtXnLP8Ujx1snYSQ0VLiviSy1")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                firebaseFirestoreException?.let {
                    Log.d("MyTag", it!!.message)
                    return@addSnapshotListener
                }
                querySnapshot?.let {
                    val parent = it.toObject<Parent>()
                    parent?.polygon?.let { arrayList ->
                        _polygonPoints.value = arrayList
                    }
                    Log.d("MyTag", parent!!.name)
                }
            }
    }


}