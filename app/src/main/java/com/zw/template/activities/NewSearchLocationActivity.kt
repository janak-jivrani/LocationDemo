package com.zw.template.activities

import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.zw.template.R
import com.zw.template.adapters.SearchLocationListAdapter
import com.zw.template.core.*
import com.zw.template.databinding.ActivityNewSearchLocationBinding
import com.zw.template.di.ZwApplication
import com.zw.template.viewmodels.LocationViewModel
import javax.inject.Inject

class NewSearchLocationActivity : BaseActivity() {

    private lateinit var binding: ActivityNewSearchLocationBinding
    private val searchList = arrayListOf<AutocompletePrediction>()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LocationViewModel>
    private lateinit var mViewModel: LocationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ZwApplication.component.inject(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_new_search_location)

        mViewModel = getViewModelFromFactory(viewModelFactory)

        setUpObserver()

        mViewModel.initLocationClient(this)

        setUpClicks(binding.ivBack)

        binding.etSearch.doOnTextChanged { _, _, _, _ ->
            mViewModel.handlePlaceSearch(binding.etSearch.text.toString())
        }

        binding.etSearch.showKeyboard()
    }

    override fun onClick(v: View) {
        super.onClick(v)
        when (v.id) {
            R.id.ivBack -> {
                binding.etSearch.hideKeyboard()
                finish()
            }
        }
    }

    private fun setUpObserver() {

        mViewModel.gecodeResultLiveData.observe(this) {
            setResult(RESULT_OK, intent.putExtra(Constant.PASS_LOCATION_DATA, it))
            finish()
        }

        mViewModel.searchResultLiveData.observe(this) {
            searchList.clear()
            searchList.addAll(it)
            binding.rvLocations.adapter?.notifyDataSetChanged() ?: kotlin.run {
                binding.rvLocations.adapter = SearchLocationListAdapter(searchList) {
                    mViewModel.getGeocodeResultByPlacePrediction(it)
                    binding.etSearch.hideKeyboard()
                }
            }
        }

    }

}