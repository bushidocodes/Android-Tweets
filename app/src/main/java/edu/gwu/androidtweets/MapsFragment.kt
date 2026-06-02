package edu.gwu.androidtweets

import android.Manifest
import android.annotation.SuppressLint
import android.location.Address
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
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
import android.content.Context
import edu.gwu.androidtweets.databinding.FragmentMapsBinding
import edu.gwu.androidtweets.viewmodel.LocationSelection
import edu.gwu.androidtweets.viewmodel.MapsViewModel
import kotlinx.coroutines.launch

class MapsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private lateinit var mMap: GoogleMap
    private lateinit var locationProvider: FusedLocationProviderClient

    // Scoped to Activity so TweetsFragment can read the selected address
    private val viewModel: MapsViewModel by activityViewModels()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MapsFragment", "Permission result: Permission granted")
            useCurrentLocation()
        } else {
            Log.d("MapsFragment", "Permission result: Permission denied")
            Toast.makeText(
                requireContext(),
                "To use this feature, enable Location permission in Settings",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationProvider = LocationServices.getFusedLocationProviderClient(requireContext())
        val email = requireContext()
            .getSharedPreferences("android-tweets", Context.MODE_PRIVATE)
            .getString("SAVED_USERNAME", "")
        requireActivity().title = getString(R.string.maps_title, email)

        binding.currentLocation.setOnClickListener { checkLocationPermission() }
        binding.confirm.isEnabled = false
        binding.confirm.setOnClickListener {
            viewModel.selection.value?.let {
                findNavController().navigate(R.id.action_mapsFragment_to_tweetsFragment)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selection.collect { selection ->
                    if (selection != null && ::mMap.isInitialized) {
                        placePin(selection)
                    }
                }
            }
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun checkLocationPermission() {
        if (requireContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("MapsFragment", "Check permission: Permission already granted")
            useCurrentLocation()
        } else {
            Log.d("MapsFragment", "Check permission: Requesting permission")
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener { coords -> viewModel.geocode(coords) }
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
        binding.confirm.setBackgroundColor(requireContext().getColor(R.color.buttonGreen))
        binding.confirm.setCompoundDrawablesWithIntrinsicBounds(
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_baseline_check_24),
            null, null, null
        )
        binding.confirm.text = address.getAddressLine(0)
        binding.confirm.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
