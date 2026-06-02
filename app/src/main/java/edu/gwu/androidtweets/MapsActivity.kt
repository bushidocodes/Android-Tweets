package edu.gwu.androidtweets

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.appcompat.content.res.AppCompatResources
import com.google.firebase.auth.FirebaseAuth
import edu.gwu.androidtweets.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mMap: GoogleMap
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var locationProvider: FusedLocationProviderClient
    private var currentAddress: Address? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationProvider = LocationServices.getFusedLocationProviderClient(this)
        firebaseAuth = FirebaseAuth.getInstance()
        title = getString(R.string.maps_title, firebaseAuth.currentUser!!.email)

        binding.currentLocation.setOnClickListener {
            checkLocationPermission()
        }

        binding.confirm.setOnClickListener {
            if (currentAddress != null) {
                val intent = Intent(this, TweetsActivity::class.java)
                intent.putExtra("address", currentAddress)
                startActivity(intent)
            }
        }
        binding.confirm.isEnabled = false

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
            doGeocoding(LatLng(location.latitude, location.longitude))
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
        mMap.setOnMapLongClickListener { coords -> doGeocoding(coords) }
    }

    private fun doGeocoding(coords: LatLng) {
        mMap.clear()

        Thread {
            val geocoder = Geocoder(this@MapsActivity)
            @Suppress("DEPRECATION")
            val results: List<Address> = try {
                geocoder.getFromLocation(coords.latitude, coords.longitude, 10) ?: listOf()
            } catch (exception: Exception) {
                Log.e("MapsActivity", "Geocoding failed", exception)
                listOf()
            }

            runOnUiThread {
                if (results.isNotEmpty()) {
                    val firstResult = results.first()
                    mMap.addMarker(
                        MarkerOptions()
                            .position(coords)
                            .title(firstResult.getAddressLine(0))
                    )
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(coords))
                    updateConfirmButton(firstResult)
                } else {
                    Log.e("MapsActivity", "Geocoding failed or returned no results")
                    Toast.makeText(this@MapsActivity, "No results for location!", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun updateConfirmButton(address: Address) {
        binding.confirm.setBackgroundColor(getColor(R.color.buttonGreen))
        binding.confirm.setCompoundDrawablesWithIntrinsicBounds(
            AppCompatResources.getDrawable(this, R.drawable.ic_baseline_check_24), null, null, null
        )
        binding.confirm.text = address.getAddressLine(0)
        binding.confirm.isEnabled = true
        currentAddress = address
    }
}
