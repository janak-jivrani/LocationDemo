package com.zw.template.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson
import com.zw.template.core.Constant
import com.zw.template.core.Constant.REQUEST_CHECK_LOCATION_SETTINGS
import com.zw.template.models.MapData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class HomeViewModel @Inject constructor() : ViewModel() {

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    val currentLocationLiveData = MutableLiveData<Location>()
    val routeLiveData = MutableLiveData<Triple<List<LatLng>, Location, String>>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionResultLauncher: ActivityResultLauncher<Array<String>>

    @SuppressLint("MissingPermission")
    fun initLocationClient(mActivity: AppCompatActivity) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mActivity)
        locationPermissionResultLauncher =
            mActivity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result: Map<String, Boolean> ->
                var allGranted = result.isNotEmpty()
                for ((_, value) in result) {
                    if (!value) {
                        allGranted = false
                        break
                    }
                }
                if (allGranted) {
                    locationSettingsRequest(mActivity)
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            mActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    ) {
                        //permissionDialog()
                    }
                }
            }
        locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(10))
                .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                currentLocationLiveData.postValue(locationResult.lastLocation)
            }
        }
    }

    private fun checkLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionResultLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    fun getLiveLocation() {
        val cts = CancellationTokenSource()
        val token = cts.token
        token.onCanceledRequested {
            // Some other operations to cancel this Task, such as free resources...
        }

        fusedLocationClient.removeLocationUpdates(locationCallback)
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    override fun onCleared() {
        super.onCleared()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    fun locationSettingsRequest(mActivity: AppCompatActivity) {
        val mLocationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, (30 * 1000).toLong())
        mLocationRequest.setMinUpdateIntervalMillis((5 * 1000).toLong())
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest.build())
        builder.setAlwaysShow(true) //this is the key ingredient
        val client = LocationServices.getSettingsClient(mActivity)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            //TODO: handle location is already on
            getLiveLocation()
        }
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialogLocationPermission.
                try {
                    // Show the dialogLocationPermission by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(mActivity, REQUEST_CHECK_LOCATION_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                    sendEx.printStackTrace()
                }
            } else {
                Toast.makeText(mActivity, "Error: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun checkAndGetCurrentLocation(mActivity: AppCompatActivity) {
        if (checkLocationPermission(mActivity)) {
            locationSettingsRequest(mActivity)
        } else {
            requestLocationPermission()
        }
    }

    // Draw route between home and current location
    fun findDirection(location: Location, origin: LatLng, dest: LatLng) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            val request = Request.Builder().url(getDirectionURL(origin, dest, Constant.GOOGLE_PLACES)).build()
            val response = client.newCall(request).execute()
            val data = response.body!!.string()

            var distance = ""
            val result = ArrayList<LatLng>()
            try {
                val respObj = Gson().fromJson(data, MapData::class.java)
                for (i in 0 until respObj.routes[0].legs[0].steps.size) {
                    result.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }

                distance = respObj.routes[0].legs[0].distance.text
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (distance.isBlank()) {
                distance = distance(origin.latitude, origin.longitude, dest.latitude, dest.longitude)
            }

            withContext(Dispatchers.Main) {
                routeLiveData.postValue(Triple(result, location, distance))
            }
        }
    }

    private fun getDirectionURL(origin: LatLng, dest: LatLng, secret: String): String {
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&sensor=false" +
                "&mode=driving" +
                "&key=$secret"
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val latLng = LatLng((lat.toDouble() / 1E5), (lng.toDouble() / 1E5))
            poly.add(latLng)
        }
        return poly
    }

    //Calculate distance
    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val startPoint = Location("locationA")
        startPoint.latitude = lat1
        startPoint.longitude = lon1

        val endPoint = Location("locationA")
        endPoint.latitude = lat2
        endPoint.longitude = lon2

        return "${startPoint.distanceTo(endPoint)/1000} Km"
    }
}