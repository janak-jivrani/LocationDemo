package com.zw.template.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.zw.template.databinding.RowLocationPickerListBinding

class SearchLocationListAdapter(
    private val locationList: ArrayList<AutocompletePrediction>,
    val callback: (AutocompletePrediction) -> Unit,
) : RecyclerView.Adapter<SearchLocationListAdapter.LocationViewHolder>() {

    inner class LocationViewHolder(private val mBinding: RowLocationPickerListBinding) :
        RecyclerView.ViewHolder(mBinding.root) {

        fun bind(position: Int, model: AutocompletePrediction) {
            mBinding.tvTitle.text = model.getPrimaryText(null)
            mBinding.tvFullLocation.text = model.getSecondaryText(null)
            mBinding.root.setOnClickListener {
                callback.invoke(model)
            }
        }
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(position, locationList[position])
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int,
    ): SearchLocationListAdapter.LocationViewHolder {
        return LocationViewHolder(
            RowLocationPickerListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return locationList.size
    }

}