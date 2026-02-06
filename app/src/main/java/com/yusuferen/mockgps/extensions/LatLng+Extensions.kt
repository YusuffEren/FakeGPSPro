package com.yusuferen.mockgps.extensions

import com.yusuferen.mockgps.ui.models.LatLng

fun LatLng.equalTo(other: LatLng): Boolean {
    return (latitude == other.latitude && longitude == other.longitude)
}

fun LatLng.prettyPrint(): String {
    return "Lat: ${this.latitude}\nLng: ${this.longitude}"
}