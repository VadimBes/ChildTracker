package com.example.android.childtracker.ui

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.android.childtracker.R
import com.example.android.childtracker.databinding.ActivityChildBinding
import com.example.android.childtracker.ui.viewmodel.ChildViewModel
import com.example.android.childtracker.utils.Constants
import com.example.android.childtracker.utils.Extension.toPointList
import com.google.firebase.firestore.GeoPoint
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChildActivity : AppCompatActivity() {

    private lateinit var viewModel: ChildViewModel
    private lateinit var binding: ActivityChildBinding

    private var mapboxMap: MapboxMap? = null
    private var drawnPolygon: Polygon? = null
    private var freehandTouchPointListForPolygon: ArrayList<Point> = ArrayList()

    private val polygonObserver = Observer<ArrayList<GeoPoint>> {
        freehandTouchPointListForPolygon = arrayListOf()
        it.forEach {
            mapboxMap?.getStyle { style ->
                freehandTouchPointListForPolygon.add(
                    Point.fromLngLat(
                        it.longitude,
                        it.latitude
                    )
                )
                val fillPolygonSource =
                    style.getSourceAs<GeoJsonSource>(Constants.FREEHAND_DRAW_FILL_LAYER_SOURCE_ID)
                val polygonList: MutableList<List<Point>> =
                    ArrayList()
                polygonList.add(freehandTouchPointListForPolygon)
                drawnPolygon = Polygon.fromLngLats(polygonList)
                fillPolygonSource?.setGeoJson(drawnPolygon)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ChildViewModel::class.java)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_child)
        binding.childViewModel = viewModel
        binding.mapView.onCreate(savedInstanceState)

            binding.mapView.getMapAsync { mapboxMap ->
                mapboxMap.setStyle(
                    Style.LIGHT
                ) {
                    this@ChildActivity.mapboxMap = mapboxMap
                    setUpExample(null)
                    setPolygonObserver()
                }
            }

    }

    private fun setPolygonObserver() = binding.childViewModel?.polygonPoints?.observe(this,polygonObserver)

    private fun setUpExample(searchDataFeatureCollection: FeatureCollection?) {
        mapboxMap!!.getStyle { loadedStyle ->
            loadedStyle.addSource(GeoJsonSource(Constants.FREEHAND_DRAW_LINE_LAYER_SOURCE_ID))
            loadedStyle.addSource(GeoJsonSource(Constants.MARKER_SYMBOL_LAYER_SOURCE_ID))
            loadedStyle.addSource(GeoJsonSource(Constants.FREEHAND_DRAW_FILL_LAYER_SOURCE_ID))
            // Add freehand draw polygon FillLayer to the map
            loadedStyle.addLayerBelow(
                FillLayer(
                    Constants.FREEHAND_DRAW_FILL_LAYER_ID,
                    Constants.FREEHAND_DRAW_FILL_LAYER_SOURCE_ID
                ).withProperties(
                    PropertyFactory.fillColor(Color.RED),
                    PropertyFactory.fillOpacity(Constants.FILL_OPACITY)
                ), Constants.FREEHAND_DRAW_LINE_LAYER_ID
            )
        }
    }

    override fun onStart() {
        super.onStart()
        binding.mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView?.onLowMemory()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        binding.mapView?.onSaveInstanceState(outState)
    }
}