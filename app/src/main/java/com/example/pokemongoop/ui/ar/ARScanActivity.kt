package com.example.pokemongoop.ui.ar

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pokemongoop.GoopApplication
import com.example.pokemongoop.data.database.entities.Creature
import com.example.pokemongoop.databinding.ActivityArScanBinding
import com.example.pokemongoop.models.GoopType
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random

class ARScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArScanBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var imageAnalyzer: ImageAnalysis? = null
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var isScanning = true

    // Random spawn while scanning
    private var lastSpawnCheckTime = 0L
    private val SPAWN_CHECK_INTERVAL_MS = 2000L // Check for spawn every 2 seconds
    private val SPAWN_CHANCE = 0.25f // 25% chance to spawn on each check

    // Catch attempt tracking
    private var catchAttemptsRemaining = 5
    private val MAX_CATCH_ATTEMPTS = 5

    private val repository by lazy {
        (application as GoopApplication).repository
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required for AR scanning", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            getLastLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityArScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        binding.backButton.setOnClickListener {
            finish()
        }

        // Hide the old capture button - we now use tap-to-catch
        binding.captureButton.visibility = View.GONE

        // Set up tap-to-catch listener
        binding.arOverlay.onCreatureTapped = { creature, success ->
            handleCatchAttempt(creature, success)
        }
    }

    private fun checkPermissions() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // Request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLatitude = it.latitude
                    currentLongitude = it.longitude
                    updateHabitatText(it.latitude, it.longitude)
                }
            }
        }
    }

    private fun updateHabitatText(lat: Double, lng: Double) {
        // Simple habitat detection based on coordinates
        val habitat = detectHabitat(lat, lng)
        binding.habitatText.text = habitat.displayName
        binding.habitatText.setBackgroundColor(Color.argb(128,
            Color.red(habitat.primaryColor),
            Color.green(habitat.primaryColor),
            Color.blue(habitat.primaryColor)
        ))
    }

    private fun detectHabitat(lat: Double, lng: Double): GoopType {
        // Simplified habitat detection - in a real app you'd use more sophisticated logic
        val hash = (lat * 1000 + lng * 1000).toInt()
        return when (hash % 5) {
            0 -> GoopType.WATER
            1 -> GoopType.FIRE
            2 -> GoopType.NATURE
            3 -> GoopType.ELECTRIC
            else -> GoopType.SHADOW
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }

            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ScanAnalyzer {
                        runOnUiThread {
                            if (isScanning && !binding.arOverlay.hasCreature()) {
                                checkForRandomSpawn()
                            }
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))

    }

    private fun checkForRandomSpawn() {
        if (binding.arOverlay.hasCreature()) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSpawnCheckTime < SPAWN_CHECK_INTERVAL_MS) return

        lastSpawnCheckTime = currentTime

        // Random chance to spawn
        if (Random.nextFloat() < SPAWN_CHANCE) {
            spawnRandomCreature()
        }
    }

    private fun spawnRandomCreature() {
        lifecycleScope.launch {
            // Get a random creature from the database
            val creature = repository.getRandomCreature()
            creature?.let {
                withContext(Dispatchers.Main) {
                    showCreature(it)
                }
            }
        }
    }

    private fun showCreature(creature: Creature) {
        // Reset catch attempts
        catchAttemptsRemaining = MAX_CATCH_ATTEMPTS

        binding.arOverlay.showCreature(creature)
        binding.scanningIndicator.visibility = View.GONE

        // Update creature info card
        binding.creatureInfoCard.visibility = View.VISIBLE
        binding.creatureNameText.text = "Wild ${creature.name}!"
        binding.creatureTypeText.text = creature.type.displayName
        binding.creatureTypeText.setTextColor(creature.type.primaryColor)
        binding.creatureRarityText.text = getRarityText(creature.rarity)

        // Show catch info with attempts
        updateCatchStatus(creature)
    }

    private fun updateCatchStatus(creature: Creature) {
        val catchRate = getCatchRate(creature.rarity)
        binding.scanStatusText.text = "Tap to catch! ($catchRate%) - $catchAttemptsRemaining attempts left"
        binding.scanStatusText.setTextColor(creature.type.primaryColor)
    }

    private fun getCatchRate(rarity: Int): Int {
        return when (rarity) {
            1 -> 70  // Common
            2 -> 55  // Uncommon
            3 -> 40  // Rare
            4 -> 25  // Epic
            5 -> 15  // Legendary
            else -> 50
        }
    }

    private fun getRarityText(rarity: Int): String {
        return when (rarity) {
            1 -> "Common"
            2 -> "Uncommon"
            3 -> "Rare"
            4 -> "Epic"
            5 -> "Legendary"
            else -> "Unknown"
        }
    }

    private fun handleCatchAttempt(creature: Creature, success: Boolean) {
        if (success) {
            // Successful catch!
            playCaptureAnimation {
                lifecycleScope.launch {
                    repository.catchCreature(
                        creatureId = creature.id,
                        latitude = currentLatitude,
                        longitude = currentLongitude
                    )

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ARScanActivity,
                            "${creature.name} caught!",
                            Toast.LENGTH_SHORT
                        ).show()
                        resetAfterCatch()
                    }
                }
            }
        } else {
            // Failed attempt - decrease remaining attempts
            catchAttemptsRemaining--

            if (catchAttemptsRemaining <= 0) {
                // No more attempts - creature escapes!
                playEscapeAnimation {
                    Toast.makeText(
                        this@ARScanActivity,
                        "${creature.name} escaped!",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetAfterCatch()
                }
            } else {
                // Still have attempts - show feedback and update UI
                playMissAnimation()
                Toast.makeText(
                    this@ARScanActivity,
                    "Missed! $catchAttemptsRemaining attempts left",
                    Toast.LENGTH_SHORT
                ).show()
                updateCatchStatus(creature)
            }
        }
    }

    private fun playMissAnimation() {
        // Quick shake to indicate miss
        val shake1 = ObjectAnimator.ofFloat(binding.arOverlay, View.TRANSLATION_X, 0f, 15f)
        val shake2 = ObjectAnimator.ofFloat(binding.arOverlay, View.TRANSLATION_X, 15f, -15f)
        val shake3 = ObjectAnimator.ofFloat(binding.arOverlay, View.TRANSLATION_X, -15f, 0f)

        shake1.duration = 50
        shake2.duration = 50
        shake3.duration = 50

        AnimatorSet().apply {
            playSequentially(shake1, shake2, shake3)
            start()
        }
    }

    private fun resetAfterCatch() {
        binding.arOverlay.hideCreature()
        binding.creatureInfoCard.visibility = View.GONE
        binding.scanningIndicator.visibility = View.VISIBLE
        binding.scanStatusText.text = "Scanning..."
        binding.scanStatusText.setTextColor(Color.WHITE)
        catchAttemptsRemaining = MAX_CATCH_ATTEMPTS
    }

    private fun playEscapeAnimation(onComplete: () -> Unit) {
        // Quick shake and fade out
        val shake1 = ObjectAnimator.ofFloat(binding.arOverlay, View.TRANSLATION_X, 0f, 25f)
        val shake2 = ObjectAnimator.ofFloat(binding.arOverlay, View.TRANSLATION_X, 25f, -25f)
        val shake3 = ObjectAnimator.ofFloat(binding.arOverlay, View.TRANSLATION_X, -25f, 0f)
        val fadeOut = ObjectAnimator.ofFloat(binding.arOverlay, View.ALPHA, 1f, 0f)

        shake1.duration = 100
        shake2.duration = 100
        shake3.duration = 100
        fadeOut.duration = 300

        AnimatorSet().apply {
            playSequentially(shake1, shake2, shake3, fadeOut)
            start()
        }

        lifecycleScope.launch {
            delay(600)
            withContext(Dispatchers.Main) {
                binding.arOverlay.translationX = 0f
                binding.arOverlay.alpha = 1f
                onComplete()
            }
        }
    }

    private fun playCaptureAnimation(onComplete: () -> Unit) {
        val scaleX = ObjectAnimator.ofFloat(binding.arOverlay, View.SCALE_X, 1f, 0f)
        val scaleY = ObjectAnimator.ofFloat(binding.arOverlay, View.SCALE_Y, 1f, 0f)
        val alpha = ObjectAnimator.ofFloat(binding.arOverlay, View.ALPHA, 1f, 0f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 500
            start()
        }

        lifecycleScope.launch {
            delay(500)
            withContext(Dispatchers.Main) {
                binding.arOverlay.scaleX = 1f
                binding.arOverlay.scaleY = 1f
                binding.arOverlay.alpha = 1f
                onComplete()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isScanning = false
        cameraExecutor.shutdown()
    }

    // Simple analyzer that triggers scan callbacks
    private class ScanAnalyzer(private val onScanFrame: () -> Unit) : ImageAnalysis.Analyzer {
        private var lastAnalyzedTime = 0L

        override fun analyze(image: ImageProxy) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastAnalyzedTime < 500) { // Check twice per second
                image.close()
                return
            }
            lastAnalyzedTime = currentTime

            onScanFrame()
            image.close()
        }
    }
}
