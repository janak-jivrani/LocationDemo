package com.zw.template.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.uppereastcare.mednurse.R
import com.uppereastcare.mednurse.activities.BaseActivity
import com.uppereastcare.mednurse.core.Constant.REQUEST_CHECK_LOCATION_SETTINGS
import com.uppereastcare.mednurse.core.ViewModelFactory
import com.uppereastcare.mednurse.core.getViewModelFromFactory
import com.uppereastcare.mednurse.databinding.ActivityChooseWorkLocationBinding
import com.zw.template.R
import com.zw.template.core.ViewModelFactory
import com.zw.template.core.getViewModelFromFactory
import com.zw.template.databinding.ActivityChooseWorkLocationBinding
import javax.inject.Inject

class ChooseWorkLocationActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityChooseWorkLocationBinding
    private lateinit var mMap: GoogleMap

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChooseLocationViewModel>
    private var chooseLocationViewModel: ChooseLocationViewModel =
        getViewModelFromFactory(viewModelFactory)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseWorkLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        chooseLocationViewModel.initLocationClient(mActivity!!)
        setUpClicks(binding.rlCurrentLocation, binding.rlLocateOnMap)
    }

    private fun handleLocateOnMap() {
        binding.groupCurrentLocation.visibility = View.VISIBLE
        val options = GoogleMapOptions()
        options.mapType(GoogleMap.MAP_TYPE_NORMAL)
            .compassEnabled(false)
            .rotateGesturesEnabled(false)
            .tiltGesturesEnabled(false).scrollGesturesEnabled(true)
        val mapFragment = SupportMapFragment.newInstance(options)
        mapFragment.getMapAsync(this)
        supportFragmentManager.beginTransaction().replace(R.id.flLocationContainer, mapFragment)
            .commitAllowingStateLoss()
    }

    @SuppressLint("MissingPermission")
    override fun onClick(v: View) {
        super.onClick(v)
        when (v.id) {
            R.id.rlCurrentLocation -> {
                mActivity?.let { chooseLocationViewModel.checkAndGetCurrentLocation(it) }
            }
            R.id.rlLocateOnMap -> {
                //TODO: show map view to pin location
                //switch to map
                handleLocateOnMap()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_LOCATION_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                chooseLocationViewModel.getCurrentLocation()
            } else {

            }
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        //TODO: handle this method
        mMap = p0
        // Add a marker for current location
        /*val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions()
            .position(sydney)
            .title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))*/
    }

    /*var cameraActivityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        if (result.getResultCode() === RESULT_OK) {
            *//*image result from camera*//*
            try {
                val path: String = PathUtils.getPath(mActivity, fileUri)
                cropImage(path)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }*/
}