package com.example.android.childtracker.utils

import com.google.firebase.firestore.GeoPoint
import com.mapbox.geojson.Point

object Extension {

    fun ArrayList<Point>.toGeoPointList(): ArrayList<GeoPoint> {
        val list: ArrayList<GeoPoint> = arrayListOf()
        this.forEach {
            list.add(GeoPoint(it.latitude(),it.longitude()))
        }
        return list
    }

    fun ArrayList<GeoPoint>.toPointList(): ArrayList<Point> {
        val list: ArrayList<Point> = arrayListOf()
        this.forEach {
            list.add(Point.fromLngLat(it.longitude,it.longitude))
        }
        return list
    }
}