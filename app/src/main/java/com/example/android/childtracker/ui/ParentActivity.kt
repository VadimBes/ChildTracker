package com.example.android.childtracker.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.android.childtracker.R
import com.example.android.childtracker.databinding.ActivityParentBinding
import com.example.android.childtracker.services.TrackingChildService
import com.example.android.childtracker.ui.viewmodel.ParentViewModel
import com.example.android.childtracker.utils.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.android.childtracker.utils.Constants.FILL_OPACITY
import com.example.android.childtracker.utils.Constants.FREEHAND_DRAW_FILL_LAYER_ID
import com.example.android.childtracker.utils.Constants.FREEHAND_DRAW_FILL_LAYER_SOURCE_ID
import com.example.android.childtracker.utils.Constants.FREEHAND_DRAW_LINE_LAYER_ID
import com.example.android.childtracker.utils.Constants.FREEHAND_DRAW_LINE_LAYER_SOURCE_ID
import com.example.android.childtracker.utils.Constants.MARKER_SYMBOL_LAYER_SOURCE_ID
import com.example.android.childtracker.utils.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.example.android.childtracker.utils.PermissionUtility
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_parent.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

@AndroidEntryPoint
class ParentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParentBinding
    private lateinit var viewModel :ParentViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        Log.d("MuTag","Здесь")
        binding = DataBindingUtil.setContentView(this, R.layout.activity_parent)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        binding.bottomNavigationView.setupWithNavController(navHostFragment.findNavController())
       // requestPermissions()
        viewModel = ViewModelProvider(this).get(ParentViewModel::class.java)
    }



//    private fun requestPermissions() {
//        if (PermissionUtility.hasPermission(this)) {
//            return
//        }
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
//            EasyPermissions.requestPermissions(
//                this,
//                "Вы должны принять разрешения для отслеживания местоположения, что-бы использовать это приложение",
//                REQUEST_CODE_LOCATION_PERMISSION,
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.ACCESS_COARSE_LOCATION,
//            )
//
//        } else {
//            EasyPermissions.requestPermissions(
//                this,
//                "Вы должны принять разрешения для отслеживания местоположения, что-бы использовать это приложение",
//                REQUEST_CODE_LOCATION_PERMISSION,
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.ACCESS_COARSE_LOCATION,
//                Manifest.permission.ACCESS_BACKGROUND_LOCATION
//            )
//        }
//
//    }
//
//
//    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
//        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
//            AppSettingsDialog.Builder(this).build().show()
//        } else {
//            requestPermissions()
//        }
//    }
//
//    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {}
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
//    }
}