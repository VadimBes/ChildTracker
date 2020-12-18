package com.example.android.childtracker.ui.viewmodel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.childtracker.data.entities.Child
import com.example.android.childtracker.data.entities.Parent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

class MainViewModel @ViewModelInject constructor(
    val auth: FirebaseAuth
):ViewModel() {

    private var personCollectionRef: CollectionReference = Firebase.firestore.collection("parents")
    private var childCollectionRef: CollectionReference = Firebase.firestore.collection("children")

    private val _currentUserLogged = MutableLiveData<Boolean>()
    val currentUserLogged: LiveData<Boolean>
        get() = _currentUserLogged

    private val _exceptionLog = MutableLiveData<Exception?>()
    val exceptionLog: LiveData<Exception?>
        get() = _exceptionLog

    private val _movingToActivity = MutableLiveData<Boolean>()
    val movingToActivity:LiveData<Boolean>
        get() = _movingToActivity


    fun checkEmail(email:String): Boolean {
        Timber.d(email)
        return email.contains('@') && email.contains('.')
    }

    fun checkPassword(password:String): Boolean {
        Timber.d(password)
        return password.length > 8
    }

    fun registerUser(email: String,password: String,itChild: Boolean) = CoroutineScope(Dispatchers.IO).launch {
        try {
            auth.createUserWithEmailAndPassword(email, password).await()
            auth.currentUser?.let {
                createDocument(it, itChild)
            }
            _movingToActivity.value = true

        }catch (e:Exception){
            Timber.d(e)
            _exceptionLog.value = e
        }
    }



    fun finishMoving(){
        _movingToActivity.value = false
    }

    private suspend fun createDocument(currentUser: FirebaseUser,itChild:Boolean) =
        CoroutineScope(Dispatchers.IO).launch {
                val nameDocument = currentUser.uid.substring(0..8)
                if (itChild){
                    childCollectionRef.document(nameDocument).set(Child())
                }else{
                    personCollectionRef.document(nameDocument).set(Parent())
                }
                withContext(Dispatchers.Main) {
                    checkLoggedInState()
                }
        }

    private fun checkLoggedInState() {
        _currentUserLogged.value = auth.currentUser != null
    }

    fun finishCheckException(){
        _exceptionLog.value = null
    }



}