package com.example.android.childtracker.ui.viewmodel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.childtracker.data.entities.Child
import com.example.android.childtracker.data.entities.Parent
import com.example.android.childtracker.data.entities.Person
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
) : ViewModel() {

    private var parentCollectionRef: CollectionReference = Firebase.firestore.collection("parents")
    private var childCollectionRef: CollectionReference = Firebase.firestore.collection("children")
    private var personCollectionRef: CollectionReference = Firebase.firestore.collection("person")

    private val _currentUserLogged = MutableLiveData<Boolean>()
    val currentUserLogged: LiveData<Boolean>
        get() = _currentUserLogged

    private val _exceptionLog = MutableLiveData<Exception?>()
    val exceptionLog: LiveData<Exception?>
        get() = _exceptionLog

    private val _movingToActivity = MutableLiveData<Int>()
    val movingToActivity: LiveData<Int>
        get() = _movingToActivity

    private val _movingToActivityLogin = MutableLiveData<Boolean>()
    val movingToActivityLogin: LiveData<Boolean>
        get() = _movingToActivityLogin

    init {
        _movingToActivity.value = -1
    }


    fun checkEmail(email: String): Boolean {
        Timber.d(email)
        return email.contains('@') && email.contains('.')
    }

    fun checkPassword(password: String): Boolean {
        Timber.d(password)
        return password.length > 8
    }

    fun loginUser(email: String,password: String) = CoroutineScope(Dispatchers.IO).launch{
        try {
            auth.signInWithEmailAndPassword(email,password).await()
            val document = personCollectionRef.document(auth.currentUser!!.uid).get().await()
            val person = document.toObject(Person::class.java)
            withContext(Dispatchers.Main){
                person?.let {
                    if (person.isChild) _movingToActivity.value = 0
                    else _movingToActivity.value = 1
                }
            }
        }catch (e:Exception){
            Timber.d(e)
            withContext(Dispatchers.Main){
                _exceptionLog.value = e
            }
        }

    }

    fun registerUser(email: String, password: String,phone:String,name:String, itChild: Boolean) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                auth.currentUser?.let {
                    createDocument(it, itChild,name,phone)
                }
                auth.signOut()
                withContext(Dispatchers.Main){
                    _movingToActivityLogin.value = true
                }
            } catch (e: Exception) {
                Timber.d(e)
                _exceptionLog.value = e
            }
        }


    fun finishMoving() {
        _movingToActivity.value = -1
    }

    private suspend fun createDocument(currentUser: FirebaseUser, itChild: Boolean,phone:String,name:String,) =
        CoroutineScope(Dispatchers.IO).launch {
            val nameDocument = currentUser.uid.substring(0..8)
            if (itChild) {
                childCollectionRef.document(nameDocument).set(Child(name = name,phone = phone))
                personCollectionRef.document(currentUser.uid).set(Person(true))
            } else {
                parentCollectionRef.document(nameDocument).set(Parent(name = name))
                personCollectionRef.document(currentUser.uid).set(Person())
            }
            withContext(Dispatchers.Main) {
                checkLoggedInState()
            }
        }

    private fun checkLoggedInState() {
        _currentUserLogged.value = auth.currentUser != null
    }

    fun finishCheckException() {
        _exceptionLog.value = null
    }


}