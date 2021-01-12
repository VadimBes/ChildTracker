package com.example.android.childtracker.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.example.android.childtracker.data.entities.Child
import com.example.android.childtracker.data.entities.Parent
import com.example.android.childtracker.utils.NoChildException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.net.URI

class SettingViewModel @ViewModelInject constructor(val auth: FirebaseAuth) : ViewModel() {

    private val parentCollectionRef = Firebase.firestore.collection("parents")
    private val childCollectionRef = Firebase.firestore.collection("children")
    private val imageRef = Firebase.storage.reference

    private val _currentParent = MutableLiveData<Parent>()
    val currentParent: LiveData<Parent>
        get() = _currentParent

    private val _currentImage = MutableLiveData<Bitmap?>()
    val currentImage: LiveData<Bitmap?>
        get() = _currentImage

    private val _currentName = MutableLiveData<String?>()
    val currentName: LiveData<String?>
        get() = _currentName

    private val _error = MutableLiveData<Exception?>()
    val error: LiveData<Exception?>
        get() = _error

    companion object{
        private val _needRestartServer = MutableLiveData<Boolean>()
        val needRestartServer: LiveData<Boolean>
            get() = _needRestartServer
    }



    init {
        getInitialValues()
        downloadImage(auth.currentUser!!.uid)
    }

    private fun getInitialValues() = CoroutineScope(Dispatchers.IO).launch {
        try {
            parentCollectionRef.document(auth.currentUser?.uid!!.substring(0..8))
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    firebaseFirestoreException?.let {
                        Timber.d(it)
                        return@addSnapshotListener
                    }
                    val parent = querySnapshot?.toObject(Parent::class.java)
                    parent?.let {
                        _currentParent.value = it
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val query =
                                    childCollectionRef.document("${it.childId}").get().await()
                                val child = query.toObject(Child::class.java)
                                child?.let {
                                    withContext(Dispatchers.Main) {
                                        _currentName.value = it.name
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.d(e)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            if (e is IllegalArgumentException) {
                Timber.d("This")
            }
            Timber.d(e)
        }
    }

    fun addChild(childId: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            parentCollectionRef.document(auth.currentUser?.uid!!.substring(0..8))
                .update("childId", childId)
            val query = childCollectionRef.document(childId).get().await()
            val child = query.toObject(Child::class.java)
            if (child != null) {
                withContext(Dispatchers.Main) {
                    _currentName.value = child.name
                    _needRestartServer.value = true
                    delay(3000)
                    _needRestartServer.value = false
                }
            } else {
                withContext(Dispatchers.Main){
                    _error.value = NoChildException()
                }
            }
        } catch (e: Exception) {
            Timber.d(e)
            withContext(Dispatchers.Main){
                _error.value = e
            }
        }
    }

    fun saveName(name: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            parentCollectionRef.document(auth.currentUser!!.uid.substring(0..8))
                .update("name", name).await()
        } catch (e: Exception) {
            Timber.d(e)
        }
    }

    private fun downloadImage(filename: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val maxDownloadSize = 5L * 1024 * 1024
            val bytes = imageRef.child("imagesProfile/$filename").getBytes(maxDownloadSize).await()
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            withContext(Dispatchers.Main) {
                _currentImage.value = bmp
            }
        } catch (e: Exception) {
            Timber.d(e)
        }
    }

    fun uploadImageToStorage(filename: String, uri: Uri) = CoroutineScope(Dispatchers.IO).launch {
        try {
            imageRef.child("imagesProfile/$filename").putFile(uri).await()

        } catch (e: Exception) {
            Timber.d(e)
            _error.value = e
        }
    }

    fun errorDone(){
        _error.value = null
    }
}