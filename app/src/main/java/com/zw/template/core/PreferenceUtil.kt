package com.zw.template.core

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.zw.template.core.Constant.DESTINATION_LIST
import com.zw.template.core.Constant.PREF_HOME_LOCATION
import com.zw.template.models.AddressDataModel
import com.zw.template.models.DestinationListDataModel

object PreferenceUtil {
    lateinit var m_sharedPreference: SharedPreferences
    lateinit var m_sharedPrefEditor: SharedPreferences.Editor
    var SHARED_PREF_NAME = "PREF_ZW"

    private fun setSharedPreference(p_context: Context?) {
        if (!::m_sharedPreference.isInitialized && p_context != null) m_sharedPreference =
            p_context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun setEditor(p_context: Context?) {
        setSharedPreference(p_context)
        if (!::m_sharedPrefEditor.isInitialized)
            m_sharedPrefEditor = m_sharedPreference.edit()
    }

    fun setIntSharedPref(p_context: Context?, p_spKey: String?, p_value: Int?) {
        setEditor(p_context)
        m_sharedPrefEditor.putInt(p_spKey, p_value!!)
        m_sharedPrefEditor.commit()
    }

    fun setLongSharedPref(p_context: Context?, p_spKey: String?, p_value: Long) {
        setEditor(p_context)
        m_sharedPrefEditor.putLong(p_spKey, p_value)
        m_sharedPrefEditor.commit()
    }

    fun setStringSharedPref(p_context: Context?, p_spKey: String?, p_value: String?) {
        setEditor(p_context)
        m_sharedPrefEditor.putString(p_spKey, p_value)
        m_sharedPrefEditor.commit()
    }

    fun setBooleanPref(p_context: Context?, p_spKey: String?, p_value: Boolean) {
        setEditor(p_context)
        m_sharedPrefEditor.putBoolean(p_spKey, p_value)
        m_sharedPrefEditor.commit()
    }

    fun getIntSharedPref(p_context: Context?, p_spKey: String?, p_value: Int): Int {
        setSharedPreference(p_context)
        return m_sharedPreference.getInt(p_spKey, p_value)
    }

    fun getLongSharedPref(p_context: Context?, p_spKey: String?, p_value: Long): Long {
        setSharedPreference(p_context)
        return m_sharedPreference.getLong(p_spKey, p_value)
    }

    fun getStringSharedPref(p_context: Context?, p_spKey: String?, p_value: String?): String? {
        setSharedPreference(p_context)
        return m_sharedPreference.getString(p_spKey, p_value)
    }

    fun getBooleanSharedPref(p_context: Context?, p_spKey: String?, p_value: Boolean): Boolean {
        setSharedPreference(p_context)
        return m_sharedPreference.getBoolean(p_spKey, p_value)
    }

    fun setHomeLocation(p_context: Context?, addressDataModel: AddressDataModel?) {
        val gson = Gson()
        val json = gson.toJson(addressDataModel)
        setStringSharedPref(p_context, PREF_HOME_LOCATION, json)
    }

    fun getHomeLocation(p_context: Context): AddressDataModel? {
        val gson = Gson()
        val json = getStringSharedPref(p_context, PREF_HOME_LOCATION, null)
        if (json != null)
            return gson.fromJson(json, AddressDataModel::class.java)
        return null
    }

    fun setDestinationList(
        p_context: Context?,
        destinationListDataModel: DestinationListDataModel
    ) {
        val string = Gson().toJson(destinationListDataModel)
        setStringSharedPref(p_context, DESTINATION_LIST, string)
    }

    fun getDestinationList(p_context: Context?): DestinationListDataModel? {
        val gson = Gson()
        val json = getStringSharedPref(p_context, DESTINATION_LIST, null)
        if (json != null)
            return gson.fromJson(json, DestinationListDataModel::class.java)
        return null
    }

}