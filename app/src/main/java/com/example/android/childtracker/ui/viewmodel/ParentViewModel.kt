package com.example.android.childtracker.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.childtracker.data.entities.Parent
import com.example.android.childtracker.utils.Extension.toGeoPointList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber


class ParentViewModel : ViewModel() {


    private var auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _currentUserLogged = MutableLiveData<Boolean>()
    val currentUserLogged: LiveData<Boolean>
        get() = _currentUserLogged



    companion object {
        private val _touchPointListForService = MutableLiveData<ArrayList<Point>?>()
        val touchPointListForService: LiveData<ArrayList<Point>?>
            get() = _touchPointListForService
    }


    private var personCollectionRef: CollectionReference = Firebase.firestore.collection("parents")
    private var childCollectionRef: CollectionReference = Firebase.firestore.collection("children")

    lateinit var currentUser: FirebaseUser

    private fun registerUser() = CoroutineScope(Dispatchers.IO).launch {
        try {
            auth.createUserWithEmailAndPassword("test@mail.ru", "Baramba").await()
            auth.currentUser?.let {
                createParentDocument(it)
                checkLoggedInState()
                currentUser = it
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Timber.d(e)
            }
        }
    }

    private fun checkLoggedInState() {
        _currentUserLogged.value = auth.currentUser != null
    }

    private suspend fun createParentDocument(currentUser: FirebaseUser) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                personCollectionRef.document(currentUser.uid).set(Parent())
                withContext(Dispatchers.Main) {
                    checkLoggedInState()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Timber.d(e)
                }
            }
        }

    private fun addChild(childUID: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            personCollectionRef.document(currentUser.uid).update("childId", childUID).await()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Timber.d(e)
            }
        }
    }

    fun saveGeoPoint(arrayPoints: ArrayList<Point>) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withContext(Dispatchers.Main) {
                Log.d("MyTag", auth.currentUser?.uid)
            }
            personCollectionRef.document(auth.currentUser!!.uid)
                .update("polygon", arrayPoints.toGeoPointList()).await()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Log.d("MyTag", e.message)
            }
        }
    }


}