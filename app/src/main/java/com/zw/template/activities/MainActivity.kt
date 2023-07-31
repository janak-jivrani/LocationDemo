package com.zw.template.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.zw.template.R
import com.zw.template.core.*
import com.zw.template.databinding.ActivityMainBinding
import com.zw.template.di.ZwApplication
import com.zw.template.viewmodels.HomeViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity : BaseActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMainBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<HomeViewModel>
    private lateinit var chooseLocationViewModel: HomeViewModel

    private var mMap: GoogleMap? = null
    private var homeMarker: Marker? = null
    private var currentMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ZwApplication.component.inject(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chooseLocationViewModel = getViewModelFromFactory(viewModelFactory)
        setUpClicks(binding.fabHome)
        setUpObserver()
        setUpMap()
    }

    private fun setUpMap() {
        chooseLocationViewModel.initLocationClient(this)
        chooseLocationViewModel.checkAndGetCurrentLocation(this)
        val options = GoogleMapOptions()
        options.mapType(GoogleMap.MAP_TYPE_NORMAL).compassEnabled(false)
            .rotateGesturesEnabled(false).tiltGesturesEnabled(false).scrollGesturesEnabled(true)
        val mapFragment = SupportMapFragment.newInstance(options)
        mapFragment.getMapAsync(this)
        supportFragmentManager.beginTransaction().replace(R.id.flLocationContainer, mapFragment)
            .commitAllowingStateLoss()
    }

    private fun setUpObserver() {
        chooseLocationViewModel.currentLocationLiveData.observe(this) {
            lifecycleScope.launch {
                val homeAddress = PreferenceUtil.getHomeLocation(this@MainActivity)
                if (homeAddress?.latitude != null && homeAddress.longitude != null) {
                    chooseLocationViewModel.findDirection(
                        it,
                        LatLng(it.latitude, it.longitude),
                        LatLng(homeAddress.latitude!!, homeAddress.longitude!!),
                        getString(R.string.api_key)
                    )
                } else addLocationIntoMap(null, it, null)
            }
        }
        chooseLocationViewModel.routeLiveData.observe(this) {
            lifecycleScope.launch {
                addLocationIntoMap(it.first, it.second, it.third)
            }
        }
    }

    private fun addLocationIntoMap(
        listRoutes: List<LatLng>?,
        location: Location?,
        distance: String?
    ) {

        mMap?.clear()
        mMap?.setOnCameraIdleListener(null)
        mMap?.setOnCameraMoveListener(null)
        homeMarker?.remove()
        currentMarker?.remove()

        PreferenceUtil.getHomeLocation(this)?.let {
            val marker = CommonUtils.bitmapDescriptorFromVector(
                this, R.drawable.ic_marker_home
            )
            homeMarker =
                mMap?.addMarker(
                    MarkerOptions().position(
                        LatLng(it.latitude!!, it.longitude!!),
                    ).title("Home").icon(marker)
                )
        }

        location?.let {
            val marker = CommonUtils.bitmapDescriptorFromVector(
                this, R.drawable.ic_marker_current
            )
            currentMarker = mMap?.addMarker(
                MarkerOptions().position(LatLng(it.latitude, it.longitude))
                    .title("${it.latitude},${it.longitude}").icon(marker)
            )

            if (distance.isNullOrBlank().not()) {
                binding.tvDistance.text =
                    "Location: ${location.latitude}, ${location.longitude}\nDistance: $distance"
            } else
                binding.tvDistance.text = "Location: ${location.latitude}, ${location.longitude}"

            binding.cvLocationDetails.isVisible = true
        }

        val builder = LatLngBounds.Builder()
        listRoutes?.let {
            val lineOption = PolylineOptions()
            lineOption.addAll(it)
            lineOption.width(10f)
            lineOption.color(Color.GREEN)
            lineOption.geodesic(true)
            mMap?.addPolyline(lineOption)
            it.forEach {
                builder.include(it)
            }
        }
        homeMarker?.position?.let { it1 -> builder.include(it1) }
        currentMarker?.position?.let { it1 -> builder.include(it1) }
        mMap!!.setOnMapLoadedCallback { //animate camera here
            mMap!!.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                    builder.build(), 300
                )
            )
        }
    }

    override fun onClick(v: View) {
        super.onClick(v)
        when (v.id) {
            R.id.fabHome -> {
                activityLauncher.launch(
                    Intent(this, ChooseLocationActivity::class.java),
                    object : BetterActivityResult.OnActivityResult<ActivityResult> {
                        override fun onActivityResult(result: ActivityResult) {
                            if (result.resultCode == Activity.RESULT_OK) {
                                PreferenceUtil.getHomeLocation(this@MainActivity)?.let {
                                    homeMarker?.remove()
                                    val marker = CommonUtils.bitmapDescriptorFromVector(
                                        this@MainActivity, R.drawable.ic_marker_home
                                    )
                                    homeMarker = mMap?.addMarker(
                                        MarkerOptions().position(
                                            LatLng(
                                                it.latitude!!,
                                                it.longitude!!
                                            )
                                        ).title("Home")
                                            .icon(marker)
                                    )

                                    val builder = LatLngBounds.Builder()
                                    homeMarker?.position?.let { it1 -> builder.include(it1) }
                                    currentMarker?.position?.let { it1 -> builder.include(it1) }
                                    mMap!!.setOnMapLoadedCallback { //animate camera here
                                        mMap!!.animateCamera(
                                            CameraUpdateFactory.newLatLngBounds(
                                                builder.build(), 300
                                            )
                                        )
                                    }
                                }
                                if (currentMarker == null) chooseLocationViewModel.checkAndGetCurrentLocation(
                                    this@MainActivity
                                )
                            }
                        }
                    })
            }
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
    }

    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constant.REQUEST_CHECK_LOCATION_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                chooseLocationViewModel.getLiveLocation()
            }
        }
    }
}