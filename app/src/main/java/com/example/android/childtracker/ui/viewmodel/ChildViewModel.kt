package com.example.android.childtracker.ui.viewmodel

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


class ChildViewModel :ViewModel(){


    private var auth: FirebaseAuth =FirebaseAuth.getInstance()

    private val _currentUserLogged = MutableLiveData<Boolean>()
    val currentUserLogged: LiveData<Boolean>
        get() = _currentUserLogged

    private val _polygonPoints = MutableLiveData<ArrayList<GeoPoint>>()
    val polygonPoints: LiveData<ArrayList<GeoPoint>>
        get() = _polygonPoints


    private var personCollectionRef : CollectionReference = Firebase.firestore.collection("parents")


    private var childCollectionRef : CollectionReference = Firebase.firestore.collection("children")

    lateinit var currentUser: FirebaseUser

    init {
        subscribeToPolygonParent()
    }

    private fun registerUser() = CoroutineScope(Dispatchers.IO).launch {
        try {
            auth.createUserWithEmailAndPassword("test@mail.ru", "Baramba").await()
            auth.currentUser?.let {
                createChildDocument(it)
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

    private suspend fun createChildDocument(currentUser: FirebaseUser) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                childCollectionRef.document(currentUser.uid).set(Parent())
                withContext(Dispatchers.Main) {
                    checkLoggedInState()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Timber.d(e)
                }
            }
        }

    private fun addParent(parentUID:String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            childCollectionRef.document(currentUser.uid).update("parentId",parentUID).await()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Timber.d(e)
            }
        }
    }

    @Suppress("UNREACHABLE_CODE")
    private fun subscribeToPolygonParent(){
        personCollectionRef.document(auth.currentUser!!.uid).addSnapshotListener{querySnapshot,firebaseFirestoreException->
            firebaseFirestoreException?.let {
                Log.d("MyTag",it!!.message)
                return@addSnapshotListener
            }
            querySnapshot?.let {
                val parent = it.toObject<Parent>()
                parent?.polygon?.let {arrayList->
                    _polygonPoints.value = arrayList
                }
                Log.d("MyTag",parent!!.name)
            }

        }
    }



}