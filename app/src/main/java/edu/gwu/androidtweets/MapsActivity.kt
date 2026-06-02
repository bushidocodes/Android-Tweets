package edu.gwu.androidtweets

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import edu.gwu.androidtweets.databinding.ActivityMapsBinding
import edu.gwu.androidtweets.viewmodel.LocationSelection
import edu.gwu.androidtweets.viewmodel.MapsViewModel
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mMap: GoogleMap
    private lateinit var locationProvider: FusedLocationProviderClient
    private val viewModel: MapsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationProvider = LocationServices.getFusedLocationProviderClient(this)
        title = getString(R.string.maps_title, FirebaseAuth.getInstance().currentUser!!.email)

        binding.currentLocation.setOnClickListener { checkLocationPermission() }
        binding.confirm.isEnabled = false
        binding.confirm.setOnClickListener {
            viewModel.selection.value?.let { selection ->
                startActivity(
                    Intent(this, TweetsActivity::class.java)
                        .putExtra("address", selection.address)
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selection.collect { selection ->
                    if (selection != null && ::mMap.isInitialized) {
                        placePin(selection)
                    }
                }
            }
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun checkLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("MapsActivity", "Check permission: Permission already granted")
            useCurrentLocation()
        } else {
            Log.d("MapsActivity", "Check permission: Permission not granted")
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 200)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            locationProvider.removeLocationUpdates(this)
            val location = result.lastLocation ?: return
            viewModel.geocode(LatLng(location.latitude, location.longitude))
        }
    }

    @SuppressLint("MissingPermission")
    private fun useCurrentLocation() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5_000L)
            .build()
        locationProvider.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MapsActivity", "Permission result: Permission granted")
                useCurrentLocation()
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Log.d("MapsActivity", "Permission result: Permission denied (regular)")
                } else {
                    Log.d("MapsActivity", "Permission result: Permission denied (do not re-prompt)")
                    Toast.makeText(
                        this,
                        "To use this feature, go into your Settings and enable the Location permission",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener { coords -> viewModel.geocode(coords) }
        // Restore pin on rotation — ViewModel already has the selection
        viewModel.selection.value?.let { placePin(it) }
    }

    private fun placePin(selection: LocationSelection) {
        mMap.clear()
        mMap.addMarker(
            MarkerOptions()
                .position(selection.coords)
                .title(selection.address.getAddressLine(0))
        )
        mMap.animateCamera(CameraUpdateFactory.newLatLng(selection.coords))
        updateConfirmButton(selection.address)
    }

    private fun updateConfirmButton(address: Address) {
        binding.confirm.setBackgroundColor(getColor(R.color.buttonGreen))
        binding.confirm.setCompoundDrawablesWithIntrinsicBounds(
            AppCompatResources.getDrawable(this, R.drawable.ic_baseline_check_24), null, null, null
        )
        binding.confirm.text = address.getAddressLine(0)
        binding.confirm.isEnabled = true
    }
}
