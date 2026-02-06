package com.yusuferen.mockgps.ui.models

import com.yusuferen.mockgps.ui.models.LatLng

data class LocationEntry(
    var latLng: LatLng,
    var addressLine: String? = null,
)
