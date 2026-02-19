package com.example.gooponthego.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gooponthego.databinding.ActivityHabitatMapBinding
import com.example.gooponthego.models.GoopType
import com.google.android.gms.location.*
import kotlin.math.abs

class HabitatMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHabitatMapBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var currentLocation: Location? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHabitatMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()
        checkLocationPermission()
    }

    private fun setupUI() {
        binding.backButton.setOnClickListener {
            finish()
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    currentLocation = location
                    updateLocationUI(location)
                }
            }
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED -> {
                startLocationUpdates()
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000 // 5 seconds
        ).apply {
            setMinUpdateIntervalMillis(2000)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // Get last known location first
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLocation = it
                updateLocationUI(it)
            }
        }
    }

    private fun updateLocationUI(location: Location) {
        // Update map view
        binding.habitatMapView.setPlayerLocation(location.latitude, location.longitude)

        // Update coordinates text
        binding.coordinatesText.text = String.format(
            "Lat: %.4f, Lng: %.4f",
            location.latitude,
            location.longitude
        )

        // Update accuracy
        binding.accuracyText.text = String.format("GPS: ±%.0fm", location.accuracy)

        // Determine current habitat based on location
        val habitat = determineHabitat(location.latitude, location.longitude)
        updateHabitatInfo(habitat)
    }

    private fun determineHabitat(lat: Double, lng: Double): GoopType {
        // Use location coordinates to deterministically assign habitat type
        // This creates "zones" based on geographic coordinates

        // Create a simple hash based on coordinates (rounded to create zones)
        val latZone = (lat * 100).toInt()
        val lngZone = (lng * 100).toInt()
        val hash = abs(latZone + lngZone)

        return when (hash % 10) {
            0, 1 -> GoopType.WATER    // 20% water
            2, 3 -> GoopType.NATURE   // 20% nature
            4, 5 -> GoopType.ELECTRIC // 20% electric
            6, 7 -> GoopType.FIRE     // 20% fire
            else -> GoopType.SHADOW   // 20% shadow
        }
    }

    private fun updateHabitatInfo(habitat: GoopType) {
        binding.currentHabitatText.text = "${habitat.displayName} Zone"
        binding.currentHabitatText.setTextColor(habitat.primaryColor)

        val bonusText = when (habitat) {
            GoopType.WATER -> "Water types +50% spawn rate"
            GoopType.FIRE -> "Fire types +50% spawn rate"
            GoopType.NATURE -> "Nature types +50% spawn rate"
            GoopType.ELECTRIC -> "Electric types +50% spawn rate"
            GoopType.SHADOW -> "Shadow types +50% spawn rate"
            else -> "Hybrid types may appear"
        }
        binding.habitatBonusText.text = bonusText
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
