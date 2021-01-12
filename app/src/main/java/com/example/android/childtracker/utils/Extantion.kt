package com.example.android.childtracker.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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

    fun String.toShortUID():String{
        return this.substring(0..8)
    }

    fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable) {
            return this.bitmap
        }

        val bitmap = Bitmap.createBitmap(this.intrinsicWidth, this.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        this.setBounds(0, 0, canvas.width, canvas.height)
        this.draw(canvas)

        return bitmap
    }
}