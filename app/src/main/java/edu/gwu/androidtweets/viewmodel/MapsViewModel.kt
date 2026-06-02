package edu.gwu.androidtweets.viewmodel

import android.app.Application
import android.location.Address
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LocationSelection(val coords: LatLng, val address: Address)

class MapsViewModel(application: Application) : AndroidViewModel(application) {

    private val _selection = MutableStateFlow<LocationSelection?>(null)
    val selection: StateFlow<LocationSelection?> = _selection.asStateFlow()

    fun geocode(coords: LatLng) {
        viewModelScope.launch {
            val address = withContext(Dispatchers.IO) {
                val geocoder = Geocoder(getApplication())
                @Suppress("DEPRECATION")
                try {
                    geocoder.getFromLocation(coords.latitude, coords.longitude, 10)?.firstOrNull()
                } catch (e: Exception) {
                    null
                }
            }
            if (address != null) {
                _selection.value = LocationSelection(coords, address)
            }
        }
    }
}
