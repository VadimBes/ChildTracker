package com.example.android.childtracker.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.android.childtracker.R
import com.example.android.childtracker.databinding.ActivityChildBinding
import com.example.android.childtracker.services.ChildLocationService
import com.example.android.childtracker.services.TrackingChildService
import com.example.android.childtracker.ui.viewmodel.ChildViewModel
import com.example.android.childtracker.utils.Constants
import com.example.android.childtracker.utils.Constants.DEFAULT_INTERVAL_IN_MILLISECONDS
import com.example.android.childtracker.utils.Constants.DEFAULT_MAX_WAIT_TIME
import com.google.firebase.firestore.GeoPoint
import com.mapbox.android.core.location.*
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import java.lang.ref.WeakReference

class ChildActivity : AppCompatActivity() {

    lateinit var viewModel: ChildViewModel
    lateinit var binding: ActivityChildBinding

    private var mapboxMap: MapboxMap? = null
    private var drawnPolygon: Polygon? = null
    private var freehandTouchPointListForPolygon: ArrayList<Point> = ArrayList()
    private var locationEngine: LocationEngine? = null
    //   private val callback = LocationChangeListeningActivityLocationCallback(this)

    private val polygonObserver = Observer<ArrayList<GeoPoint?>> {
        freehandTouchPointListForPolygon = arrayListOf()
        it.forEach {geopoint->
            geopoint?.let {
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
                setUpExample()
                setPolygonObserver()
                enableLocationComponent(it)
                sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE)
                setChildLocationObserver()
            }
        }

    }

    private fun setPolygonObserver() = binding.childViewModel?.polygonPoints?.observe(
        this,
        polygonObserver
    )

    private fun sendCommandToService(action:String){
        Intent(this, ChildLocationService::class.java).also {
            it.action = action
            this.startService(it)
        }
    }

    private fun setChildLocationObserver() = ChildLocationService.childLocation.observe(this,
        Observer { location ->

            if (mapboxMap != null) {
                mapboxMap!!.getLocationComponent()
                    .forceLocationUpdate(location)
            }


        })

    private fun setUpExample() {
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

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
// Check if permissions are enabled and if not request

// Get an instance of the component
        val locationComponent = mapboxMap!!.locationComponent

// Set the LocationComponent activation options
        val locationComponentActivationOptions =
            LocationComponentActivationOptions.builder(this, loadedMapStyle)
                .useDefaultLocationEngine(false)
                .build()

// Activate with the LocationComponentActivationOptions object
        locationComponent.activateLocationComponent(locationComponentActivationOptions)

// Enable to make component visible
        locationComponent.isLocationComponentEnabled = true

// Set the component's camera mode
        locationComponent.cameraMode = CameraMode.TRACKING

// Set the component's render mode
        locationComponent.renderMode = RenderMode.COMPASS

    }

//    @SuppressLint("MissingPermission")
//    private fun initLocationEngine() {
//        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
//        val request = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
//            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
//            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build()
//        locationEngine!!.requestLocationUpdates(request, callback, mainLooper)
//        locationEngine!!.getLastLocation(callback)
//    }
//
//    private class LocationChangeListeningActivityLocationCallback(activity: ChildActivity) :
//        LocationEngineCallback<LocationEngineResult> {
//        private val activityWeakReference: WeakReference<ChildActivity> = WeakReference(activity)
//        override fun onSuccess(result: LocationEngineResult) {
//            val activity = activityWeakReference.get()
//            if (activity != null) {
//                val location = result.lastLocation ?: return
//
//                // Create a Toast which displays the new location's coordinates
//                Toast.makeText(
//                    activity,
//                    "Lng: ${result.lastLocation?.longitude} , Lat: ${result.lastLocation?.latitude}",
//                    Toast.LENGTH_SHORT
//                ).show()
//
//                // Pass the new location to the Maps SDK's LocationComponent
//                if (activity.mapboxMap != null && result.lastLocation != null) {
//                    activity.mapboxMap!!.getLocationComponent()
//                        .forceLocationUpdate(result.lastLocation)
//                }
//
//                location?.let {
//                    activity.binding.childViewModel?.changeLocation(it)
//                }
//            }
//        }
//
//        /**
//         * The LocationEngineCallback interface's method which fires when the device's location can't be captured
//         *
//         * @param exception the exception message
//         */
//        override fun onFailure(exception: Exception) {
//            val activity = activityWeakReference.get()
//            if (activity != null) {
//                Toast.makeText(
//                    activity, exception.localizedMessage + "Salam",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//
//    }

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