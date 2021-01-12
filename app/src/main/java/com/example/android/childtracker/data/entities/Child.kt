package com.example.android.childtracker.data.entities

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.local.ReferenceSet

data class Child (
    val name:String = "",
    val location:GeoPoint = GeoPoint(45.54,45.54),
    val parentId:String = "",
    val phone:String =""
)