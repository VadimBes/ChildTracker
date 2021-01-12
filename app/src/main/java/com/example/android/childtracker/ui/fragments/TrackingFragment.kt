package com.example.android.childtracker.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.android.childtracker.R
import com.example.android.childtracker.databinding.ActivityParentBinding
import com.example.android.childtracker.databinding.FragmentTrackerBinding
import com.example.android.childtracker.services.TrackingChildService
import com.example.android.childtracker.ui.viewmodel.ParentViewModel
import com.example.android.childtracker.utils.Constants
import com.example.android.childtracker.utils.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.android.childtracker.utils.Constants.ACTION_STOP_SERVICE
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_parent.*
import pub.devrel.easypermissions.EasyPermissions

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracker) {

    private lateinit var binding: FragmentTrackerBinding
    private var mapboxMap: MapboxMap? = null
    private var freehandTouchPointListForPolygon: ArrayList<Point> = ArrayList()

    private var searchPointFeatureCollection: FeatureCollection? = null
    private var showSearchDataLocations = true
    private val drawSingleLineOnly = false
    private var drawnPolygon: Polygon? = null
    private var isDrawing: Boolean = false
    private var serviceIsRunning = true
    private val viewModel: ParentViewModel by viewModels()


    @SuppressLint("ClickableViewAccessibility")
    private val customOnTouchListener =
        View.OnTouchListener { _, motionEvent ->
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
                    when {
                        freehandTouchPointListForPolygon.size < 2 -> {
                            freehandTouchPointListForPolygon.add(screenTouchPoint)
                        }
                        freehandTouchPointListForPolygon.size == 2 -> {
                            freehandTouchPointListForPolygon.add(screenTouchPoint)
                            freehandTouchPointListForPolygon.add(freehandTouchPointListForPolygon[0])
                        }
                        else -> {
                            freehandTouchPointListForPolygon.removeAt(
                                freehandTouchPointListForPolygon.size - 1
                            )
                            freehandTouchPointListForPolygon.add(screenTouchPoint)
                            freehandTouchPointListForPolygon.add(freehandTouchPointListForPolygon[0])
                        }
                    }
                }
                // Create and show a FillLayer polygon where the search area is
                val fillPolygonSource =
                    style.getSourceAs<GeoJsonSource>(Constants.FREEHAND_DRAW_FILL_LAYER_SOURCE_ID)
                val polygonList: MutableList<List<Point>> =
                    ArrayList()
                polygonList.add(freehandTouchPointListForPolygon)
                drawnPolygon = Polygon.fromLngLats(polygonList)
                fillPolygonSource?.setGeoJson(drawnPolygon)


                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    viewModel.saveGeoPoint(freehandTouchPointListForPolygon)
                    enableMapMovement()
                }
            }
            true
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTrackerBinding.inflate(inflater, null, false)
        val view = binding.root
        binding.viewModel = viewModel
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mapView.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(
                Style.LIGHT
            ) { style ->
                this.mapboxMap = mapboxMap
                setUpExample(null)

                binding.clearMapForNewDrawFab
                    .setOnClickListener { // Reset ArrayLists
                        freehandTouchPointListForPolygon =
                            ArrayList<Point>()
                        // Add empty Feature array to the sources
                        val drawLineSource =
                            style.getSourceAs<GeoJsonSource>(Constants.FREEHAND_DRAW_LINE_LAYER_SOURCE_ID)
                        drawLineSource?.setGeoJson(FeatureCollection.fromFeatures(arrayOf()))
                        val fillPolygonSource =
                            style.getSourceAs<GeoJsonSource>(Constants.FREEHAND_DRAW_FILL_LAYER_SOURCE_ID)
                        fillPolygonSource?.setGeoJson(FeatureCollection.fromFeatures(arrayOf()))
                        enableMapDrawing()
                        binding.viewModel?.clearPolygon()
                    }

                binding.enableMapFab
                    .setOnClickListener { view ->
                        var textForSnack = ""
                        if (isDrawing) {
                            textForSnack = "Рисование выключенно"
                            enableMapMovement()
                        } else {
                            textForSnack = "Рисование включенно"
                            enableMapDrawing()
                        }
                        Snackbar.make(view,textForSnack,Snackbar.LENGTH_LONG).show()
                    }
                binding.stopServiceFab
                    .setOnClickListener {
                        if (serviceIsRunning){
                            sendCommandToService(ACTION_STOP_SERVICE)
                            serviceIsRunning = false
                        }else{
                            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
                            serviceIsRunning = true
                        }
                    }
            }

            setChildLocationObserver()
        }
        binding.viewModel?.checkChild()
        binding.viewModel?.needFirstStartService?.observe(requireActivity(),{
            if (it){
                sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
                binding.viewModel?.finishCheck()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.viewModel?.clearPolygon()
    }

    private fun sendCommandToService(action: String) {
        Intent(requireContext(), TrackingChildService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }
    }

    private fun setUpExample(searchDataFeatureCollection: FeatureCollection?) {
        searchPointFeatureCollection = searchDataFeatureCollection
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
            enableMapDrawing()
            Toast.makeText(
                requireContext(),
                getString(R.string.draw_instruction), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setChildLocationObserver() {
        TrackingChildService.locationChild.observe(requireActivity(), Observer { child ->
            child?.location?.let {
                mapboxMap?.clear()
                mapboxMap?.addMarker(
                    MarkerOptions().position(LatLng(it.latitude, it.longitude)).setTitle(child.name)
                )
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableMapMovement() {
        isDrawing = false
        binding.mapView.setOnTouchListener(null)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableMapDrawing() {
        isDrawing = true
        binding.mapView.setOnTouchListener(customOnTouchListener)
    }
}