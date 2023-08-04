package com.zw.template.activities

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.internal.service.Common
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.zw.template.R
import com.zw.template.core.*
import com.zw.template.databinding.ActivityMainBinding
import com.zw.template.di.ZwApplication
import com.zw.template.viewmodels.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

class MainActivity : BaseActivity(), OnMapReadyCallback, SensorEventListener {

    private lateinit var binding: ActivityMainBinding

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<HomeViewModel>
    private lateinit var chooseLocationViewModel: HomeViewModel

    private var mMap: GoogleMap? = null
    private var homeMarker: Marker? = null
    private var currentMarker: Marker? = null

    private var currentLocation: LatLng? = null
    private var destinationLocation: LatLng? = null

    private var homeLocation: LatLng? = null
    private var userMarker: Marker? = null

    private var isZoomed = false

    private lateinit var sensorManager: SensorManager
    private lateinit var rotationSensor: Sensor

    private var currentRotation = 0f
    private var prevAzimuthDegrees = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        ZwApplication.component.inject(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chooseLocationViewModel = getViewModelFromFactory(viewModelFactory)
        setUpClicks(binding.fabHome)
        setUpObserver()
        setUpMap()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    }

    private fun setUpMap() {
        chooseLocationViewModel.initLocationClient(this)
        chooseLocationViewModel.checkAndGetCurrentLocation(this)
        val options = GoogleMapOptions()
        options.mapType(GoogleMap.MAP_TYPE_NORMAL).compassEnabled(false)
            .rotateGesturesEnabled(true).tiltGesturesEnabled(false)
            .scrollGesturesEnabled(true)
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

                    currentLocation = LatLng(it.latitude, it.longitude)
                    destinationLocation = homeLocation

                    homeLocation = LatLng(homeAddress.latitude!!, homeAddress.longitude!!)

                    chooseLocationViewModel.findDirection(
                        it,
                        LatLng(it.latitude, it.longitude),
                        LatLng(homeAddress.latitude!!, homeAddress.longitude!!)
                    )
                } else {
                    addLocationIntoMap(null, it, null)
                }
            }
        }
        chooseLocationViewModel.routeLiveData.observe(this) {
            mDialog.dismiss()
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
            homeMarker = mMap?.addMarker(
                CommonUtils.getHomeMarker(
                    this,
                    LatLng(it.latitude!!, it.longitude!!)
                )
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

        userMarker?.remove()
        val option = MarkerOptions()
            .position(LatLng(location?.latitude!!, location.longitude))
            .rotation(currentRotation)
            .anchor(0.5f, 0.5f)
            .icon(CommonUtils.bitmapDescriptorFromVector(this, R.drawable.ic_direction_arrow))
        userMarker = mMap?.addMarker(option)

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
            if (!isZoomed) {
                mMap!!.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        builder.build(), 300
                    )
                )
                lifecycleScope.launch {
                    delay(3000)
                    isZoomed = true
                }
            }
        }

    }

    override fun onClick(v: View) {
        super.onClick(v)
        when (v.id) {
            R.id.fabHome -> {
                activityLauncher.launch(
                    Intent(this, DestinationsListActivity::class.java),
                    object : BetterActivityResult.OnActivityResult<ActivityResult> {
                        override fun onActivityResult(result: ActivityResult) {
                            if (result.resultCode == Activity.RESULT_OK) {
                                PreferenceUtil.getHomeLocation(this@MainActivity)?.let {
                                    mDialog.show()
                                    destinationLocation = LatLng(it.latitude!!, it.longitude!!)
                                    mMap?.clear()
                                    homeMarker = mMap?.addMarker(
                                        CommonUtils.getHomeMarker(
                                            this@MainActivity,
                                            destinationLocation
                                        )
                                    )
                                    currentMarker = mMap?.addMarker(
                                        CommonUtils.getCurrentMarker(
                                            this@MainActivity,
                                            currentLocation
                                        )
                                    )

                                    val builder = LatLngBounds.Builder()
                                    builder.include(currentLocation!!)
                                    builder.include(destinationLocation!!)
                                    mMap?.setOnMapLoadedCallback {
                                        mMap?.animateCamera(
                                            CameraUpdateFactory.newLatLngBounds(
                                                builder.build(), 300
                                            )
                                        )
                                    }

                                    chooseLocationViewModel.findDirection(Location("").apply {
                                        latitude = currentLocation?.latitude!!
                                        longitude = currentLocation?.longitude!!
                                    }, currentLocation!!, destinationLocation!!)

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

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientationValues = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationValues)

            val azimuthDegrees = (Math.toDegrees(orientationValues[0].toDouble()) + 360) % 360

            val animator = ValueAnimator.ofFloat(currentRotation, azimuthDegrees.toFloat())
            animator.duration = 100 // Adjust the duration as needed for smoother or faster rotation
            animator.addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Float
                userMarker?.rotation = value
                currentRotation = value
            }
            animator.start()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

}