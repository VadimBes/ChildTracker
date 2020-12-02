package com.example.android.childtracker.ui

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.android.childtracker.R
import com.example.android.childtracker.databinding.ActivityParentBinding
import com.example.android.childtracker.ui.viewmodel.ParentViewModel
import com.example.android.childtracker.utils.Constants.FILL_OPACITY
import com.example.android.childtracker.utils.Constants.FREEHAND_DRAW_FILL_LAYER_ID
import com.example.android.childtracker.utils.Constants.FREEHAND_DRAW_FILL_LAYER_SOURCE_ID
import com.example.android.childtracker.utils.Constants.FREEHAND_DRAW_LINE_LAYER_ID
import com.example.android.childtracker.utils.Constants.FREEHAND_DRAW_LINE_LAYER_SOURCE_ID
import com.example.android.childtracker.utils.Constants.MARKER_SYMBOL_LAYER_SOURCE_ID
import com.example.android.childtracker.utils.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.example.android.childtracker.utils.PermissionUtility
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions


class ParentActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private lateinit var binding: ActivityParentBinding
    private var mapboxMap: MapboxMap? = null
    private var freehandTouchPointListForPolygon: ArrayList<Point> = ArrayList()
    private var freehandTouchPointListForLine: ArrayList<Point> = ArrayList()
    lateinit var locationEngine: LocationEngine
    private var searchPointFeatureCollection: FeatureCollection? = null
    private var showSearchDataLocations = true
    private val drawSingleLineOnly = false
    private var canRecive = false
    private var drawnPolygon: Polygon? = null
    lateinit var viewModel :ParentViewModel

    @SuppressLint("ClickableViewAccessibility")
    private val customOnTouchListener =
        OnTouchListener { view, motionEvent ->
            val latLngTouchCoordinate = mapboxMap!!.projection.fromScreenLocation(
                PointF(motionEvent.x, motionEvent.y)
            )
            val screenTouchPoint = Point.fromLngLat(
                latLngTouchCoordinate.longitude,
                latLngTouchCoordinate.latitude
            )

            // Draw the line on the map as the finger is dragged along the map
//            freehandTouchPointListForLine.add(screenTouchPoint)
            mapboxMap!!.getStyle { style ->

                // Draw a polygon area if drawSingleLineOnly == false
                if (!drawSingleLineOnly) {
                    if (freehandTouchPointListForPolygon.size < 2) {
                        freehandTouchPointListForPolygon.add(screenTouchPoint)
                    } else if (freehandTouchPointListForPolygon.size == 2) {
                        freehandTouchPointListForPolygon.add(screenTouchPoint)
                        freehandTouchPointListForPolygon.add(freehandTouchPointListForPolygon[0])
                    } else {
                        freehandTouchPointListForPolygon.removeAt(freehandTouchPointListForPolygon.size - 1)
                        freehandTouchPointListForPolygon.add(screenTouchPoint)
                        freehandTouchPointListForPolygon.add(freehandTouchPointListForPolygon[0])
                    }
                }
                // Create and show a FillLayer polygon where the search area is
                val fillPolygonSource =
                    style.getSourceAs<GeoJsonSource>(FREEHAND_DRAW_FILL_LAYER_SOURCE_ID)
                val polygonList: MutableList<List<Point>> =
                    ArrayList()
                polygonList.add(freehandTouchPointListForPolygon)
                drawnPolygon = Polygon.fromLngLats(polygonList)
                fillPolygonSource?.setGeoJson(drawnPolygon)

                // Take certain actions when the drawing is done
                if (motionEvent.action == MotionEvent.ACTION_UP) {

                    // If drawing polygon, add the first screen touch point to the end of
                    // the LineLayer list so that it's
//                    if (!drawSingleLineOnly) {
//                        freehandTouchPointListForLine.add(freehandTouchPointListForPolygon[0])
//                    }
                    Log.d("MyTag", "In this state")
                    viewModel.saveGeoPoint(freehandTouchPointListForPolygon)
                    enableMapMovement()
                    canRecive = true
                }
            }
            true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        binding = DataBindingUtil.setContentView(this, R.layout.activity_parent)
        requestPermissions()
        viewModel = ViewModelProvider(this).get(ParentViewModel::class.java)
        binding.viewModel = viewModel

        binding.mapView.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(
                Style.LIGHT
            ) { style ->
                this.mapboxMap = mapboxMap
                setUpExample(null)

                findViewById<View>(R.id.clear_map_for_new_draw_fab)
                    .setOnClickListener { // Reset ArrayLists
                        freehandTouchPointListForPolygon =
                            ArrayList<Point>()
//                        freehandTouchPointListForLine =
//                            ArrayList<Point>()

                        // Add empty Feature array to the sources
                        val drawLineSource =
                            style.getSourceAs<GeoJsonSource>(FREEHAND_DRAW_LINE_LAYER_SOURCE_ID)
                        drawLineSource?.setGeoJson(FeatureCollection.fromFeatures(arrayOf()))
                        val fillPolygonSource =
                            style.getSourceAs<GeoJsonSource>(FREEHAND_DRAW_FILL_LAYER_SOURCE_ID)
                        fillPolygonSource?.setGeoJson(FeatureCollection.fromFeatures(arrayOf()))
                        enableMapDrawing()
                    }
            }
        }
    }

    private fun setUpExample(searchDataFeatureCollection: FeatureCollection?) {
        searchPointFeatureCollection = searchDataFeatureCollection
        mapboxMap!!.getStyle { loadedStyle ->
            loadedStyle.addSource(GeoJsonSource(FREEHAND_DRAW_LINE_LAYER_SOURCE_ID))
            loadedStyle.addSource(GeoJsonSource(MARKER_SYMBOL_LAYER_SOURCE_ID))
            loadedStyle.addSource(GeoJsonSource(FREEHAND_DRAW_FILL_LAYER_SOURCE_ID))
            // Add freehand draw polygon FillLayer to the map
            loadedStyle.addLayerBelow(
                FillLayer(
                    FREEHAND_DRAW_FILL_LAYER_ID,
                    FREEHAND_DRAW_FILL_LAYER_SOURCE_ID
                ).withProperties(
                    PropertyFactory.fillColor(Color.RED),
                    PropertyFactory.fillOpacity(FILL_OPACITY)
                ), FREEHAND_DRAW_LINE_LAYER_ID
            )
            enableMapDrawing()
            Toast.makeText(
                this,
                getString(R.string.draw_instruction), Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableMapMovement() {
        binding.mapView.setOnTouchListener(null)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableMapDrawing() {
        binding.mapView.setOnTouchListener(customOnTouchListener)
    }

    private fun requestPermissions() {
        if (PermissionUtility.hasPermission(this)) {
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "Вы должны принять разрешения для отслеживания местоположения, что-бы использовать это приложение",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )

        } else {
            EasyPermissions.requestPermissions(
                this,
                "Вы должны принять разрешения для отслеживания местоположения, что-бы использовать это приложение",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }

    }


    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermissions()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}