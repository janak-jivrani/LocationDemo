package com.uppereastcare.mednurse.worklocation.search

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.uppereastcare.mednurse.R
import com.uppereastcare.mednurse.activities.BaseActivity
import com.uppereastcare.mednurse.core.Constant
import com.uppereastcare.mednurse.core.ViewModelFactory
import com.uppereastcare.mednurse.core.getViewModelFromFactory
import com.uppereastcare.mednurse.databinding.ActivityNewSearchLocationBinding
import com.uppereastcare.mednurse.di.ZwApplication
import com.uppereastcare.mednurse.extensions.hideKeyboard
import com.uppereastcare.mednurse.extensions.showKeyboard
import javax.inject.Inject

class NewSearchLocationActivity : BaseActivity() {

    private lateinit var binding: ActivityNewSearchLocationBinding

    val searchList = arrayListOf<AutocompletePrediction>()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ChooseLocationViewModel>
    private lateinit var mViewModel: ChooseLocationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ZwApplication).component.inject(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_new_search_location)

        mViewModel = getViewModelFromFactory(viewModelFactory)

        setUpObserver()

        mViewModel.initLocationClient(mActivity!!)

        setUpClicks(binding.ivBack)

        binding.etSearch.doOnTextChanged { _, _, _, _ ->
            mpEventLogger.logSearchWorkLocationEvent(binding.etSearch.text.toString())
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
            setResult(Activity.RESULT_OK, intent.putExtra(Constant.PASS_LOCATION_DATA, it))
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