package com.yusuferen.mockgps.ui.models

/**
 * Enlem ve boylam bilgisini tutan veri sınıfı
 * OpenStreetMap ile kullanım için tasarlandı
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        val DEFAULT = LatLng(41.0082, 28.9784) // İstanbul
    }
}
