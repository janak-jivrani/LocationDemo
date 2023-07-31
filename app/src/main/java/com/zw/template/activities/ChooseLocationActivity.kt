package com.zw.template.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.zw.template.R
import com.zw.template.adapters.SearchLocationListAdapter
import com.zw.template.core.*
import com.zw.template.core.Constant.PASS_LOCATION_DATA
import com.zw.template.core.Constant.REQUEST_CHECK_LOCATION_SETTINGS
import com.zw.template.databinding.ActivityChooseLocationBinding
import com.zw.template.di.ZwApplication
import com.zw.template.models.AddressDataModel
import com.zw.template.viewmodels.LocationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChooseLocationActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityChooseLocationBinding
    private var mMap: GoogleMap? = null

    private var selectedPlace: AddressDataModel? = null
    private var currentMarker: Marker? = null

    private var locateOnMap = false

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LocationViewModel>
    private lateinit var mViewModel: LocationViewModel

    private val searchList = arrayListOf<AutocompletePrediction>()
    private var isEdit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ZwApplication.component.inject(this)
        binding = ActivityChooseLocationBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        mViewModel = getViewModelFromFactory(viewModelFactory)

        setUpObserver()

        mViewModel.initLocationClient(this)

        setUpClicks(
            binding.ivBack,
            binding.rlCurrentLocation,
            binding.rlLocateOnMap,
            binding.btnConfirm,
            binding.llSearchView
        )

        binding.etSearch.doOnTextChanged { _, _, _, _ ->
            mViewModel.handlePlaceSearch(binding.etSearch.text.toString())
        }

        handleLocateOnMap()

    }

    private fun setIsEditOrNot() {

        PreferenceUtil.getHomeLocation(this)?.let {
            isEdit = true
            binding.btnConfirm.text = getString(R.string.update_location)
            showMapViews()
            val addressModel = it
            locateOnMap = true
            addLocationIntoMap(addressModel)
        }

        if (!isEdit) {
            binding.etSearch.requestFocus()
            binding.etSearch.showKeyboard()
        }

    }

    private fun setUpObserver() {

        mViewModel.gecodeResultLiveData.observe(this) {
            addLocationIntoMap(it)
        }

        mViewModel.currentLocationLiveData.observe(this) {
            lifecycleScope.launch {
                showMapViews()
                delay(1000)
                mViewModel.getAddress(this@ChooseLocationActivity, it.latitude, it.longitude)
            }
        }

        mViewModel.searchResultLiveData.observe(this) {
            searchList.clear()
            searchList.addAll(it)
            binding.rvLocations.adapter?.notifyDataSetChanged() ?: kotlin.run {
                binding.rvLocations.adapter = SearchLocationListAdapter(searchList) {
                    locateOnMap = false
                    selectedPlace = null
                    mViewModel.getGeocodeResultByPlacePrediction(it)
                    binding.etSearch.hideKeyboard()
                }
            }
        }
    }

    private fun showMapViews() {
        if (!binding.cardMap.isVisible) {
            binding.cardMap.isVisible = true
            binding.cardAddressDetails.isVisible = true
            binding.rvLocations.isVisible = false
            binding.btnConfirm.isVisible = true
            binding.rlCurrentLocation.isVisible = false
            binding.rlLocateOnMap.isVisible = false
            binding.view.isVisible = false
            binding.etSearch.clearFocus()
            binding.clSearchBar.isVisible = false
            binding.llSearchView.isVisible = true
        }
    }

    private fun hideMapViews() {
        binding.cardMap.isVisible = false
        binding.cardAddressDetails.isVisible = false
        binding.rvLocations.isVisible = true
        binding.btnConfirm.isVisible = false
        binding.rlCurrentLocation.isVisible = true
        binding.rlLocateOnMap.isVisible = true
        binding.view.isVisible = true
        binding.clSearchBar.isVisible = true
        binding.llSearchView.isVisible = false
    }

    private fun addLocationIntoMap(location: AddressDataModel?) {

        showMapViews()
        mMap?.clear()
        mMap?.setOnCameraIdleListener(null)
        mMap?.setOnCameraMoveListener(null)

        if (!locateOnMap) {
            binding.imgTempMarker.isVisible = false
            location?.let {
                val marker = CommonUtils.bitmapDescriptorFromVector(
                    this,
                    R.drawable.ic_marker_current
                )
                currentMarker = mMap?.addMarker(
                    MarkerOptions().position(LatLng(it.latitude!!, it.longitude!!)).icon(marker)
                )
                currentMarker?.hideInfoWindow()
                binding.addressName.text = location.name
                binding.addressDetails.text = location.address
                selectedPlace?.let {
                    selectedPlace = location
                } ?: kotlin.run {
                    selectedPlace = location
                    val cameraPosition =
                        CameraPosition.Builder().target(LatLng(it.latitude!!, it.longitude!!))
                            .zoom(15f).build()
                    mMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }
            }
        } else {
            selectedPlace?.let {
                selectedPlace = location
            } ?: kotlin.run {
                selectedPlace = location
                val cameraPosition =
                    CameraPosition.Builder()
                        .target(LatLng(location?.latitude!!, location.longitude!!)).zoom(15f)
                        .build()
                mMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
            binding.addressName.text = location?.name
            binding.addressDetails.text = location?.address
            binding.imgTempMarker.isVisible = true
        }

        lifecycleScope.launch {
            delay(2000)
            if (locateOnMap) {
                mMap?.setOnCameraMoveListener(onCameraMoveListener)
                mMap?.setOnCameraIdleListener(onCameraIdleListener)
            }
        }

    }

    private fun handleLocateOnMap() {
        val options = GoogleMapOptions()
        options.mapType(GoogleMap.MAP_TYPE_NORMAL).compassEnabled(false)
            .rotateGesturesEnabled(false).tiltGesturesEnabled(false).scrollGesturesEnabled(true)
        val mapFragment = SupportMapFragment.newInstance(options)
        mapFragment.getMapAsync(this)
        supportFragmentManager.beginTransaction()
            .replace(R.id.flLocationContainer, mapFragment)
            .commitAllowingStateLoss()

        if (locateOnMap) {
            mMap?.setOnCameraMoveListener(onCameraMoveListener)
            mMap?.setOnCameraIdleListener(onCameraIdleListener)
        }

    }

    @SuppressLint("MissingPermission")
    override fun onClick(v: View) {
        super.onClick(v)
        when (v.id) {
            R.id.ivBack -> {
                binding.etSearch.hideKeyboard()
                onBackPressed()
            }

            R.id.rlCurrentLocation -> {
                locateOnMap = false
                selectedPlace = null
                mMap?.clear()
                binding.etSearch.hideKeyboard()
                mViewModel.checkAndGetCurrentLocation(this)
                showMapViews()
            }

            R.id.rlLocateOnMap -> {
                locateOnMap = true
                selectedPlace = null
                mMap?.clear()
                binding.etSearch.hideKeyboard()
                mViewModel.checkAndGetCurrentLocation(this)
                showMapViews()
            }

            R.id.btnConfirm -> {
                saveHomeLocation()
            }

            R.id.llSearchView -> {
                searchLocationListener.launch(Intent(this, NewSearchLocationActivity::class.java))
            }
        }
    }

    private fun saveHomeLocation() {
        selectedPlace?.let {
            PreferenceUtil.setHomeLocation(this, it)
            setResult(RESULT_OK)
            finish()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_LOCATION_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                mViewModel.getCurrentLocation()
            }
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        setIsEditOrNot()
    }

    override fun onBackPressed() {
        if (binding.cardMap.isVisible && !isEdit) {
            hideMapViews()
        } else {
            super.onBackPressed()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private val onCameraIdleListener = GoogleMap.OnCameraIdleListener {
        mMap?.cameraPosition?.target?.let {
            mViewModel.getAddress(this@ChooseLocationActivity, it.latitude, it.longitude)
        }
    }

    private val onCameraMoveListener = GoogleMap.OnCameraMoveListener {
    }


    private val searchLocationListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val temp = result.data?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        it.getSerializableExtra(PASS_LOCATION_DATA, AddressDataModel::class.java)
                    } else {
                        it.getSerializableExtra(PASS_LOCATION_DATA) as AddressDataModel
                    }
                }
                temp?.let {
                    selectedPlace = null
                    addLocationIntoMap(it)
                }
            }
        }
}

