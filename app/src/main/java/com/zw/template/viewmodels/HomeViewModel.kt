package com.zw.template.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.zw.template.activities.MainActivity
import com.zw.template.core.Constant.REQUEST_CHECK_LOCATION_SETTINGS
import com.zw.template.models.AddressDataModel
import kotlinx.coroutines.CoroutineExceptionHandler
import java.util.*
import javax.inject.Inject

class LocationViewModel @Inject constructor() : ViewModel() {

    val lastLocationLiveData = MutableLiveData<Location>()
    val currentLocationLiveData = MutableLiveData<Location>()
    val currentLiveLocationLiveData = MutableLiveData<Location>()
    val gecodeResultLiveData = MutableLiveData<AddressDataModel?>()
    val searchResultLiveData = MutableLiveData<ArrayList<AutocompletePrediction>>()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionResultLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var placesClient: PlacesClient
    private lateinit var token: AutocompleteSessionToken

    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "ChooseLocationViewModel"

    @SuppressLint("MissingPermission")
    fun initLocationClient(mActivity: AppCompatActivity) {
        Places.initialize(mActivity, mActivity.getString(com.zw.template.R.string.api_key))
        placesClient = Places.createClient(mActivity)
        token = AutocompleteSessionToken.newInstance()
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
    fun getLastLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener {
            lastLocationLiveData.postValue(it)
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    fun getCurrentLocation() {
        val cts = CancellationTokenSource()
        val token = cts.token
        token.onCanceledRequested {
            // Some other operations to cancel this Task, such as free resources...

        }
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token)
            .addOnSuccessListener {
                currentLocationLiveData.postValue(it)
            }
    }

    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    fun getLiveLocation() {
        val cts = CancellationTokenSource()
        val token = cts.token
        token.onCanceledRequested {
            // Some other operations to cancel this Task, such as free resources...
        }
        fusedLocationClient.requestLocationUpdates(Priority.PRIORITY_HIGH_ACCURACY, token)
            .addOnSuccessListener {
                currentLiveLocationLiveData.postValue(it)
            }
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
            if (mActivity is MainActivity) {
                getLiveLocation()
            } else
                getCurrentLocation()
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

    fun handlePlaceSearch(query: String) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ getPlacePredictions(query) }, 300)
    }

    private fun getPlacePredictions(query: String) {

        val request = FindAutocompletePredictionsRequest.builder().setQuery(query).build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                for (prediction in response.autocompletePredictions) {
                    Log.i(TAG, prediction.placeId)
                    Log.i(TAG, prediction.getPrimaryText(null).toString())
                }
                searchResultLiveData.postValue(
                    response.autocompletePredictions.toCollection(
                        ArrayList()
                    )
                )
            }.addOnFailureListener { exception: Exception? ->
                if (exception is ApiException) {
                    Log.e(TAG, "Place not found: " + exception.statusCode)
                }
            }

    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.w(TAG, "No results from geocoding request." + throwable.message)
    }

    fun getGeocodeResultByPlacePrediction(placePrediction: AutocompletePrediction) {
        val placeId = placePrediction.placeId.toString()
        val placeFields =
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val request = FetchPlaceRequest.builder(placeId, placeFields).build()
        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            val address = AddressDataModel().apply {
                name = place.name
                address = place.address
                latitude = place.latLng?.latitude.toString()
                longitude = place.latLng?.longitude.toString()
                latLng = place.latLng as LatLng
            }
            gecodeResultLiveData.postValue(address)
        }.addOnFailureListener { exception ->
            if (exception is ApiException) {
                // Toast.makeText(mContext, exception.message + "", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getAddress(activity: Activity, currentLatitude: Double, currentLongitude: Double) {
        try {
            val geocoder = Geocoder(activity, Locale.getDefault())
            val addresses: MutableList<Address>? =
                geocoder.getFromLocation(currentLatitude, currentLongitude, 1)
            if (addresses?.isNotEmpty() == true) {
                val current: Address = addresses[0]
                val addressdata = AddressDataModel().apply {
                    name = current.featureName
                    address = current.getAddressLine(0)
                    latitude = currentLatitude.toString()
                    longitude = currentLongitude.toString()
                    latLng = LatLng(currentLatitude, currentLongitude)
                }
                gecodeResultLiveData.postValue(addressdata)
            }
        } catch (e: Exception) {

        }
    }
}