package com.example.android.childtracker.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.childtracker.data.entities.Parent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

class ChildViewModel :ViewModel(){

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _currentUserLogged = MutableLiveData<Boolean>()
    val currentUserLogged: LiveData<Boolean>
        get() = _currentUserLogged

    private val personCollectionRef = Firebase.firestore.collection("parents")
    private val childCollectionRef = Firebase.firestore.collection("children")

    lateinit var currentUser: FirebaseUser

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

}