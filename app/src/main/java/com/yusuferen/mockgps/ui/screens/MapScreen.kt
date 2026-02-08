package com.yusuferen.mockgps.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yusuferen.mockgps.MainActivity
import com.yusuferen.mockgps.extensions.roundedShadow
import com.yusuferen.mockgps.service.LocationHelper
import com.yusuferen.mockgps.storage.StorageManager
import com.yusuferen.mockgps.ui.components.FavoritesListComponent
import com.yusuferen.mockgps.ui.components.FooterComponent
import com.yusuferen.mockgps.ui.components.SearchComponent
import com.yusuferen.mockgps.ui.screens.viewmodels.MapViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel(),
    activity: MainActivity,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isMocking by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // Reference to the MapView for external control
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var markerRef by remember { mutableStateOf<Marker?>(null) }

    // Initialize OSMDroid configuration
    DisposableEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    fun updateMarkerOnMap(mapView: MapView, lat: Double, lon: Double) {
        markerRef?.let { mapView.overlays.remove(it) }
        
        val newMarker = Marker(mapView).apply {
            position = GeoPoint(lat, lon)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Seçili Konum"
        }
        markerRef = newMarker
        mapView.overlays.add(newMarker)
        mapView.invalidate()
    }

    fun animateToPosition(mapView: MapView, lat: Double, lon: Double) {
        mapView.controller.animateTo(GeoPoint(lat, lon))
        updateMarkerOnMap(mapView, lat, lon)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // OpenStreetMap View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    
                    // Set initial position
                    val initialLat = mapViewModel.markerPosition.value.latitude
                    val initialLon = mapViewModel.markerPosition.value.longitude
                    controller.setCenter(GeoPoint(initialLat, initialLon))
                    
                    // Add initial marker
                    val marker = Marker(this).apply {
                        position = GeoPoint(initialLat, initialLon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Seçili Konum"
                    }
                    markerRef = marker
                    overlays.add(marker)
                    
                    // Handle map clicks
                    setOnTouchListener { _, event ->
                        if (event.action == android.view.MotionEvent.ACTION_UP && !isMocking) {
                            val projection = projection
                            val geoPoint = projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                            
                            // Update marker position
                            mapViewModel.updateMarkerPositionOSM(geoPoint.latitude, geoPoint.longitude)
                            updateMarkerOnMap(this, geoPoint.latitude, geoPoint.longitude)
                        }
                        false
                    }
                    
                    mapViewRef = this
                    
                    // Request permissions
                    LocationHelper.requestPermissions(activity)
                }
            },
            update = { mapView ->
                mapViewRef = mapView
            }
        )

        Column(
            modifier = Modifier.statusBarsPadding()
        ) {
            SearchComponent(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxHeight(0.075f)
                    .fillMaxWidth()
                    .padding(4.dp)
                    .roundedShadow(32.dp)
                    .zIndex(32f),
                onSearch = { searchTerm ->
                    if (isMocking) {
                        Toast.makeText(
                            activity,
                            "Konum sahtelenirken arama yapamazsınız",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@SearchComponent
                    }

                    LocationHelper.geocoding(searchTerm) { foundLatLng ->
                        foundLatLng?.let {
                            mapViewModel.updateMarkerPosition(it)
                            mapViewRef?.let { mv ->
                                animateToPosition(mv, it.latitude, it.longitude)
                            }
                        }
                    }
                }
            )

            // Favorites button
            IconButton(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .align(Alignment.End),
                onClick = { showBottomSheet = true },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Blue, contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.List,
                    contentDescription = "Favorileri göster"
                )
            }
        }

        FooterComponent(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(1f)
                .navigationBarsPadding()
                .padding(4.dp)
                .zIndex(32f)
                .roundedShadow(16.dp),
            address = mapViewModel.address.value,
            latLng = mapViewModel.markerPosition.value,
            isMocking = isMocking,
            isFavorite = mapViewModel.markerPositionIsFavorite.value,
            onStart = { isMocking = activity.toggleMocking() },
            onFavorite = { mapViewModel.toggleFavoriteForLocation() }
        )

        if (showBottomSheet) {
            FavoritesListComponent(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState,
                data = StorageManager.favorites,
                onEntryClicked = { clickedEntry ->
                    if (isMocking) {
                        Toast.makeText(
                            activity,
                            "Konum sahtelenirken değişiklik yapamazsınız",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@FavoritesListComponent
                    }
                    mapViewModel.updateMarkerPosition(clickedEntry.latLng)
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                    mapViewRef?.let { mv ->
                        animateToPosition(mv, clickedEntry.latLng.latitude, clickedEntry.latLng.longitude)
                    }
                }
            )
        }
    }
}