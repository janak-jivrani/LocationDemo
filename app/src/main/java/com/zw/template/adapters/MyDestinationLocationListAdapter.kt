package com.zw.template.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.zw.template.databinding.RowDestinationLocationListBinding
import com.zw.template.models.AddressDataModel

class MyDestinationLocationListAdapter(
    private val callback: (AddressDataModel, Int, Int) -> Unit,
) : RecyclerView.Adapter<MyDestinationLocationListAdapter.LocationViewHolder>() {

    private val locationList = ArrayList<AddressDataModel>()

    inner class LocationViewHolder(private val mBinding: RowDestinationLocationListBinding) :
        ViewHolder(mBinding.root) {

        fun bind(position: Int, model: AddressDataModel) {
            mBinding.tvTitle.text = model.name
            mBinding.tvFullLocation.text = model.address

            mBinding.root.setOnClickListener {
                callback.invoke(model, adapterPosition, 0)
            }

        }

    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(position, locationList[position])
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int,
    ): MyDestinationLocationListAdapter.LocationViewHolder {
        return LocationViewHolder(
            RowDestinationLocationListBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun getItemCount(): Int {
        return locationList.size
    }

    fun setData(newRating: List<AddressDataModel>) {
        val diffCallback = ChatDiffUtil(locationList, newRating)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        locationList.clear()
        locationList.addAll(newRating)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ChatDiffUtil(
        private val oldList: List<AddressDataModel>,
        private val newList: List<AddressDataModel>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] === newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val (_, value, name) = oldList[oldItemPosition]
            val (_, value1, name1) = newList[newItemPosition]
            return name == name1 && value == value1
        }

    }

}