package edu.gwu.androidtweets

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
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
import edu.gwu.androidtweets.databinding.FragmentMapsBinding
import edu.gwu.androidtweets.viewmodel.LocationSelection
import edu.gwu.androidtweets.viewmodel.MapsViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class MapsFragment : Fragment() {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapsViewModel by activityViewModels()

    private var locationManager: LocationManager? = null
    private val locationListener: LocationListener = LocationListener { location ->
        locationManager?.removeUpdates(locationListener)
        viewModel.geocode(location.latitude, location.longitude)
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) useCurrentLocation()
        else Toast.makeText(
            requireContext(),
            "Enable Location permission in Settings to use this feature",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // OSMDroid must be configured before the MapView is inflated
        Configuration.getInstance().apply {
            load(
                requireContext(),
                requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
            )
            userAgentValue = requireContext().packageName
        }
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = requireContext()
            .getSharedPreferences("android-tweets", Context.MODE_PRIVATE)
            .getString("SAVED_USERNAME", "")
        requireActivity().title = getString(R.string.maps_title, email)

        // Map setup — MapEventsOverlay must be added last so it receives touch events first
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?) = false
            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let { viewModel.geocode(it.latitude, it.longitude) }
                return true
            }
        })
        binding.map.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(3.0)
            overlays.add(mapEventsOverlay)
        }

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
                    selection?.let { placePin(it) }
                }
            }
        }

        // Restore pin after rotation
        viewModel.selection.value?.let { placePin(it) }
    }

    override fun onResume() {
        super.onResume()
        _binding?.map?.onResume()
    }

    override fun onPause() {
        super.onPause()
        _binding?.map?.onPause()
    }

    private fun checkLocationPermission() {
        if (requireContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            useCurrentLocation()
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun useCurrentLocation() {
        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager = lm
        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> {
                Toast.makeText(requireContext(), "Location unavailable", Toast.LENGTH_SHORT).show()
                return
            }
        }
        lm.requestLocationUpdates(provider, 0L, 0f, locationListener, Looper.getMainLooper())
    }

    private fun placePin(selection: LocationSelection) {
        val map = _binding?.map ?: return
        val point = GeoPoint(selection.latitude, selection.longitude)

        // Remove previous markers
        map.overlays.removeAll { it is Marker }

        val marker = Marker(map).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = selection.address.getAddressLine(0)
        }
        map.overlays.add(marker)
        map.controller.animateTo(point)
        map.invalidate()

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
        locationManager?.removeUpdates(locationListener)
        _binding?.map?.onDetach()
        super.onDestroyView()
        _binding = null
    }
}
