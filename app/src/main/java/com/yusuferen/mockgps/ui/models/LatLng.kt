package com.yusuferen.mockgps.ui.models

/**
 * Simple LatLng data class to replace Google Maps LatLng
 * This allows us to use OpenStreetMap without depending on Google Play Services
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        val DEFAULT = LatLng(41.0082, 28.9784) // Istanbul, Turkey
    }
}
