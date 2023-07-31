package com.zw.template.models

data class AddressDataModel(
    var name: String? = null,
    var address: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
) : java.io.Serializable