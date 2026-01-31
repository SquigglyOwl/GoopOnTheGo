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

    // Color tracking for sustained scanning
    private var currentDetectedType: GoopType? = null
    private var colorDetectionStartTime: Long = 0L
    private val SPAWN_THRESHOLD_MS = 2500L // Must scan color for 2.5 seconds to spawn
    private var spawnProgress = 0f // 0.0 to 1.0 for UI feedback

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
                    it.setAnalyzer(cameraExecutor, ColorAnalyzer { dominantColor ->
                        runOnUiThread {
                            if (isScanning && !binding.arOverlay.hasCreature()) {
                                trackColorForSpawn(dominantColor)
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

    private fun trackColorForSpawn(dominantColor: Int) {
        if (binding.arOverlay.hasCreature()) return

        // Determine creature type based on dominant color
        val detectedType = when {
            isBlueish(dominantColor) -> GoopType.WATER
            isReddish(dominantColor) -> GoopType.FIRE
            isGreenish(dominantColor) -> GoopType.NATURE
            isYellowish(dominantColor) -> GoopType.ELECTRIC
            isDarkish(dominantColor) -> GoopType.SHADOW
            else -> null
        }

        val currentTime = System.currentTimeMillis()

        if (detectedType != null) {
            if (detectedType == currentDetectedType) {
                // Same color - check if we've scanned long enough
                val elapsedTime = currentTime - colorDetectionStartTime
                spawnProgress = (elapsedTime.toFloat() / SPAWN_THRESHOLD_MS).coerceIn(0f, 1f)

                // Update UI to show scanning progress
                updateScanProgress(detectedType, spawnProgress)

                if (elapsedTime >= SPAWN_THRESHOLD_MS) {
                    // Spawn the creature!
                    spawnCreatureOfType(detectedType)
                    resetColorTracking()
                }
            } else {
                // Different color detected - reset tracking
                currentDetectedType = detectedType
                colorDetectionStartTime = currentTime
                spawnProgress = 0f
                updateScanProgress(detectedType, 0f)
            }
        } else {
            // No valid color - reset
            if (currentDetectedType != null) {
                resetColorTracking()
                binding.scanStatusText.text = "Scanning..."
            }
        }
    }

    private fun resetColorTracking() {
        currentDetectedType = null
        colorDetectionStartTime = 0L
        spawnProgress = 0f
    }

    private fun updateScanProgress(type: GoopType, progress: Float) {
        val percentage = (progress * 100).toInt()
        binding.scanStatusText.text = "Detecting ${type.displayName}... $percentage%"
        binding.scanStatusText.setTextColor(type.primaryColor)
    }

    private fun isDarkish(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        // Dark colors with low brightness
        return r < 60 && g < 60 && b < 60
    }

    private fun spawnCreatureOfType(type: GoopType) {
        lifecycleScope.launch {
            val creature = repository.getBaseCreatureByType(type)
            creature?.let {
                withContext(Dispatchers.Main) {
                    showCreature(it)
                }
            }
        }
    }

    private fun showCreature(creature: Creature) {
        binding.arOverlay.showCreature(creature)
        binding.scanningIndicator.visibility = View.GONE

        // Update creature info card
        binding.creatureInfoCard.visibility = View.VISIBLE
        binding.creatureNameText.text = "Wild ${creature.name}!"
        binding.creatureTypeText.text = creature.type.displayName
        binding.creatureTypeText.setTextColor(creature.type.primaryColor)
        binding.creatureRarityText.text = getRarityText(creature.rarity)

        // Show catch rate hint
        val catchRate = when (creature.rarity) {
            1 -> 90
            2 -> 75
            3 -> 55
            4 -> 35
            5 -> 20
            else -> 70
        }
        binding.scanStatusText.text = "Tap to catch! ($catchRate% chance)"
        binding.scanStatusText.setTextColor(creature.type.primaryColor)
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
            // Failed catch - creature escapes!
            playEscapeAnimation {
                Toast.makeText(
                    this@ARScanActivity,
                    "${creature.name} escaped!",
                    Toast.LENGTH_SHORT
                ).show()
                resetAfterCatch()
            }
        }
    }

    private fun resetAfterCatch() {
        binding.arOverlay.hideCreature()
        binding.creatureInfoCard.visibility = View.GONE
        binding.scanningIndicator.visibility = View.VISIBLE
        binding.scanStatusText.text = "Scanning..."
        resetColorTracking()
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

    private fun isBlueish(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return b > r && b > g && b > 100
    }

    private fun isReddish(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return r > g && r > b && r > 100
    }

    private fun isGreenish(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return g > r && g > b && g > 100
    }

    private fun isYellowish(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return r > 150 && g > 150 && b < 100
    }

    override fun onDestroy() {
        super.onDestroy()
        isScanning = false
        cameraExecutor.shutdown()
    }

    // Image analyzer for detecting dominant colors
    private class ColorAnalyzer(private val onColorDetected: (Int) -> Unit) : ImageAnalysis.Analyzer {
        private var lastAnalyzedTime = 0L

        override fun analyze(image: ImageProxy) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastAnalyzedTime < 1000) { // Analyze once per second
                image.close()
                return
            }
            lastAnalyzedTime = currentTime

            // Convert to bitmap and get dominant color
            val bitmap = image.toBitmap()
            val dominantColor = getDominantColor(bitmap)
            onColorDetected(dominantColor)

            image.close()
        }

        private fun ImageProxy.toBitmap(): Bitmap {
            val buffer = planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

        private fun getDominantColor(bitmap: Bitmap): Int {
            // Sample center region of the image
            val centerX = bitmap.width / 2
            val centerY = bitmap.height / 2
            val sampleSize = minOf(bitmap.width, bitmap.height) / 4

            var rSum = 0
            var gSum = 0
            var bSum = 0
            var count = 0

            for (x in (centerX - sampleSize).coerceAtLeast(0) until (centerX + sampleSize).coerceAtMost(bitmap.width)) {
                for (y in (centerY - sampleSize).coerceAtLeast(0) until (centerY + sampleSize).coerceAtMost(bitmap.height)) {
                    val pixel = bitmap.getPixel(x, y)
                    rSum += Color.red(pixel)
                    gSum += Color.green(pixel)
                    bSum += Color.blue(pixel)
                    count++
                }
            }

            return if (count > 0) {
                Color.rgb(rSum / count, gSum / count, bSum / count)
            } else {
                Color.GRAY
            }
        }
    }
}
