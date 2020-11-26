package com.example.android.childtracker.data.entities

import com.google.firebase.firestore.GeoPoint
import com.google.type.LatLng
import com.mapbox.geojson.Point

data class Parent (
    val name:String = "",
    val polygon: MutableList<GeoPoint> = mutableListOf(),
    val childId: String = ""
)