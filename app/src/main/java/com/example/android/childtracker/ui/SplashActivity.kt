package com.example.android.childtracker.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.android.childtracker.R
import com.example.android.childtracker.data.entities.Person
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        checkUserLogged()
    }

    private fun checkUserLogged() = CoroutineScope(Dispatchers.IO).launch {
        var intent: Intent? = null
        if (auth.currentUser != null) {
            try {
                var personCollectionRef: CollectionReference =
                    Firebase.firestore.collection("person")
                val querySnapshot =
                    personCollectionRef.document(auth.currentUser!!.uid).get().await()
                val person = querySnapshot.toObject(Person::class.java)
                person?.let {
                    withContext(Dispatchers.Main) {
                        intent = if (it.isChild) {
                            Intent(this@SplashActivity, ChildActivity::class.java)
                        } else {
                            Intent(this@SplashActivity, ParentActivity::class.java)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.d(e)
            }
        } else{
            intent = Intent(this@SplashActivity,MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}