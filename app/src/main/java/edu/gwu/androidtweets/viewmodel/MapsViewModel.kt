package edu.gwu.androidtweets.viewmodel

import android.app.Application
import android.location.Address
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Library-free coordinate pair so no map SDK leaks into the ViewModel layer. */
data class LocationSelection(
    val latitude: Double,
    val longitude: Double,
    val address: Address
)

class MapsViewModel(application: Application) : AndroidViewModel(application) {

    private val _selection = MutableStateFlow<LocationSelection?>(null)
    val selection: StateFlow<LocationSelection?> = _selection.asStateFlow()

    fun geocode(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val address = withContext(Dispatchers.IO) {
                val geocoder = Geocoder(getApplication())
                @Suppress("DEPRECATION")
                try {
                    geocoder.getFromLocation(latitude, longitude, 10)?.firstOrNull()
                } catch (e: Exception) {
                    null
                }
            }
            if (address != null) {
                _selection.value = LocationSelection(latitude, longitude, address)
            }
        }
    }
}
