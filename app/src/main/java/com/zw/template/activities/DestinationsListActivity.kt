package com.zw.template.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import com.zw.template.R
import com.zw.template.adapters.MyDestinationLocationListAdapter
import com.zw.template.core.PreferenceUtil
import com.zw.template.databinding.ActivityDestinationsListBinding
import com.zw.template.models.AddressDataModel

class DestinationsListActivity : BaseActivity() {

    private var destinationAdapter: MyDestinationLocationListAdapter? = null
    lateinit var mBinding: ActivityDestinationsListBinding

    val destinationList = arrayListOf<AddressDataModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityDestinationsListBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setUpClicks(mBinding.btnAddLocation, mBinding.ivBack)

    }

    private fun getPrefDestinationList() {

        val destination = PreferenceUtil.getDestinationList(this)
        destination?.let {
            destinationList.clear()
            destinationList.addAll(it.destinationList)
            setDestinationListAdapter()
        }

    }

    override fun onResume() {
        super.onResume()
        getPrefDestinationList()
    }

    private fun setDestinationListAdapter() {
        if (destinationList.isNotEmpty()) {
            destinationAdapter = MyDestinationLocationListAdapter { model, position, type ->
                PreferenceUtil.setHomeLocation(this, model)
                setResult(RESULT_OK)
                finish()
            }
            mBinding.rvDestinationList.apply {
                adapter = destinationAdapter
            }
            destinationAdapter?.setData(destinationList)
            mBinding.rvDestinationList.visibility = View.VISIBLE
            mBinding.llNoDestinationFound.visibility = View.GONE
        } else {
            mBinding.rvDestinationList.visibility = View.GONE
            mBinding.llNoDestinationFound.visibility = View.VISIBLE
        }
    }

    override fun onClick(v: View) {
        super.onClick(v)
        when (v.id) {

            R.id.btnAddLocation -> {
                startActivity(Intent(this, ChooseLocationActivity::class.java))
            }

            R.id.ivBack -> {
                finish()
            }

        }
    }


}