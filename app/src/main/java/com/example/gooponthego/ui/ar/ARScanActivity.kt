package com.example.gooponthego.ui.ar

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
import com.example.gooponthego.GoopApplication
import com.example.gooponthego.data.database.entities.Creature
import com.example.gooponthego.databinding.ActivityArScanBinding
import com.example.gooponthego.models.GoopType
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

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var isScanning = true

    // Color detection for spawning
    private var currentDetectedType: GoopType? = null
    private var colorDetectionStartTime = 0L
    private val COLOR_HOLD_TIME_MS = 2000L  // Hold on color for 2 seconds to spawn

    // Catch attempts
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

        // Hide capture button - we use tap-to-catch
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
        val habitat = detectHabitat(lat, lng)
        binding.habitatText.text = habitat.displayName
        binding.habitatText.setBackgroundColor(Color.argb(128,
            Color.red(habitat.primaryColor),
            Color.green(habitat.primaryColor),
            Color.blue(habitat.primaryColor)
        ))
    }

    private fun detectHabitat(lat: Double, lng: Double): GoopType {
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

            // Image analyzer for color detection
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ColorAnalyzer { dominantColor ->
                        runOnUiThread {
                            if (isScanning && !binding.arOverlay.hasCreature()) {
                                processDetectedColor(dominantColor)
                            }
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processDetectedColor(color: Int) {
        // Determine goop type from color
        val detectedType = getGoopTypeFromColor(color)

        if (detectedType != null) {
            if (detectedType == currentDetectedType) {
                // Same color - check progress
                val elapsed = System.currentTimeMillis() - colorDetectionStartTime
                val progress = (elapsed.toFloat() / COLOR_HOLD_TIME_MS * 100).toInt().coerceIn(0, 100)

                binding.scanStatusText.text = "Detecting ${detectedType.displayName}... $progress%"
                binding.scanStatusText.setTextColor(detectedType.primaryColor)

                if (elapsed >= COLOR_HOLD_TIME_MS) {
                    // Spawn this type!
                    spawnCreatureOfType(detectedType)
                    resetColorDetection()
                }
            } else {
                // New color - start tracking
                currentDetectedType = detectedType
                colorDetectionStartTime = System.currentTimeMillis()
                binding.scanStatusText.text = "Detecting ${detectedType.displayName}... 0%"
                binding.scanStatusText.setTextColor(detectedType.primaryColor)
            }
        } else {
            // No valid color detected
            if (currentDetectedType != null) {
                resetColorDetection()
            }
        }
    }

    private fun getGoopTypeFromColor(color: Int): GoopType? {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)

        // Shadow: dark colours have low brightness across all channels
        // Check this before the saturation guard so dark greys/blacks aren't rejected
        if (r < 60 && g < 60 && b < 60) return GoopType.SHADOW

        // Require meaningful saturation for all other colours
        if (max - min < 40) return null  // Too grey/neutral

        return when {
            // Yellow (Electric): high red AND high green, low blue — check BEFORE red-only Fire
            r > 140 && g > 120 && b < 110 && g > b * 2 -> GoopType.ELECTRIC

            // Red/Orange (Fire): red dominant, green noticeably lower
            r > 130 && r > g + 40 && r > b + 60 -> GoopType.FIRE

            // Blue (Water): blue clearly dominant
            b > r && b > g && b > 90 -> GoopType.WATER

            // Green (Nature): green clearly dominant
            g > r && g > b && g > 90 -> GoopType.NATURE

            else -> null
        }
    }

    private fun resetColorDetection() {
        currentDetectedType = null
        colorDetectionStartTime = 0L
        binding.scanStatusText.text = "Point camera at colors to find Goops!"
        binding.scanStatusText.setTextColor(Color.WHITE)
    }

    private fun spawnCreatureOfType(type: GoopType) {
        lifecycleScope.launch {
            val creature = repository.getBaseCreatureByType(type)
            withContext(Dispatchers.Main) {
                if (creature != null) {
                    showCreature(creature)
                } else {
                    Toast.makeText(this@ARScanActivity, "No creature found for $type", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCreature(creature: Creature) {
        catchAttemptsRemaining = MAX_CATCH_ATTEMPTS

        binding.arOverlay.showCreature(creature)
        binding.scanningIndicator.visibility = View.GONE

        // Update info card
        binding.creatureInfoCard.visibility = View.VISIBLE
        binding.creatureNameText.text = "Wild ${creature.name}!"
        binding.creatureTypeText.text = creature.type.displayName
        binding.creatureTypeText.setTextColor(creature.type.primaryColor)
        binding.creatureRarityText.text = getRarityText(creature.rarity)

        updateCatchStatus(creature)

        Toast.makeText(this, "A wild ${creature.name} appeared!", Toast.LENGTH_SHORT).show()
    }

    private fun updateCatchStatus(creature: Creature) {
        val catchRate = getCatchRate(creature.rarity)
        binding.scanStatusText.text = "Tap to catch! ($catchRate% per tap) - $catchAttemptsRemaining tries left"
        binding.scanStatusText.setTextColor(creature.type.primaryColor)
    }

    private fun getCatchRate(rarity: Int): Int {
        return when (rarity) {
            1 -> 70
            2 -> 55
            3 -> 40
            4 -> 25
            5 -> 15
            else -> 50
        }
    }

    private fun getRarityText(rarity: Int): String {
        return when (rarity) {
            1 -> "★ Common"
            2 -> "★★ Uncommon"
            3 -> "★★★ Rare"
            4 -> "★★★★ Epic"
            5 -> "★★★★★ Legendary"
            else -> "Unknown"
        }
    }

    private fun handleCatchAttempt(creature: Creature, success: Boolean) {
        if (success) {
            // Caught!
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
                            "🎉 ${creature.name} caught!",
                            Toast.LENGTH_SHORT
                        ).show()
                        resetForNextCreature()
                    }
                }
            }
        } else {
            // Missed
            catchAttemptsRemaining--

            if (catchAttemptsRemaining <= 0) {
                // Escaped
                playEscapeAnimation {
                    Toast.makeText(
                        this@ARScanActivity,
                        "${creature.name} escaped!",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetForNextCreature()
                }
            } else {
                // Try again
                playMissAnimation()
                Toast.makeText(
                    this@ARScanActivity,
                    "Missed! $catchAttemptsRemaining tries left",
                    Toast.LENGTH_SHORT
                ).show()
                updateCatchStatus(creature)
            }
        }
    }

    private fun resetForNextCreature() {
        binding.arOverlay.hideCreature()
        binding.creatureInfoCard.visibility = View.GONE
        binding.scanningIndicator.visibility = View.VISIBLE
        catchAttemptsRemaining = MAX_CATCH_ATTEMPTS
        resetColorDetection()
    }

    private fun playMissAnimation() {
        val shake1 = ObjectAnimator.ofFloat(binding.arOverlay, View.TRANSLATION_X, 0f, 20f)
        val shake2 = ObjectAnimator.ofFloat(binding.arOverlay, View.TRANSLATION_X, 20f, -20f)
        val shake3 = ObjectAnimator.ofFloat(binding.arOverlay, View.TRANSLATION_X, -20f, 0f)

        shake1.duration = 50
        shake2.duration = 50
        shake3.duration = 50

        AnimatorSet().apply {
            playSequentially(shake1, shake2, shake3)
            start()
        }
    }

    private fun playEscapeAnimation(onComplete: () -> Unit) {
        val shake1 = ObjectAnimator.ofFloat(binding.arOverlay, View.TRANSLATION_X, 0f, 30f)
        val shake2 = ObjectAnimator.ofFloat(binding.arOverlay, View.TRANSLATION_X, 30f, -30f)
        val shake3 = ObjectAnimator.ofFloat(binding.arOverlay, View.TRANSLATION_X, -30f, 0f)
        val fadeOut = ObjectAnimator.ofFloat(binding.arOverlay, View.ALPHA, 1f, 0f)

        shake1.duration = 80
        shake2.duration = 80
        shake3.duration = 80
        fadeOut.duration = 300

        AnimatorSet().apply {
            playSequentially(shake1, shake2, shake3, fadeOut)
            start()
        }

        lifecycleScope.launch {
            delay(550)
            withContext(Dispatchers.Main) {
                binding.arOverlay.translationX = 0f
                binding.arOverlay.alpha = 1f
                onComplete()
            }
        }
    }

    private fun playCaptureAnimation(onComplete: () -> Unit) {
        val scaleX = ObjectAnimator.ofFloat(binding.arOverlay, View.SCALE_X, 1f, 1.2f, 0f)
        val scaleY = ObjectAnimator.ofFloat(binding.arOverlay, View.SCALE_Y, 1f, 1.2f, 0f)
        val alpha = ObjectAnimator.ofFloat(binding.arOverlay, View.ALPHA, 1f, 1f, 0f)

        scaleX.duration = 400
        scaleY.duration = 400
        alpha.duration = 400

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            start()
        }

        lifecycleScope.launch {
            delay(450)
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

    // Analyzes camera frames to detect dominant color
    private class ColorAnalyzer(private val onColorDetected: (Int) -> Unit) : ImageAnalysis.Analyzer {
        private var lastAnalysisTime = 0L

        override fun analyze(image: ImageProxy) {
            val currentTime = System.currentTimeMillis()
            // Analyze every 200ms
            if (currentTime - lastAnalysisTime < 200) {
                image.close()
                return
            }
            lastAnalysisTime = currentTime

            val bitmap = image.toBitmap()
            val dominantColor = getDominantColor(bitmap)
            onColorDetected(dominantColor)

            image.close()
        }

        private fun getDominantColor(bitmap: Bitmap): Int {
            // Sample the center region of the image
            val centerX = bitmap.width / 2
            val centerY = bitmap.height / 2
            val sampleSize = minOf(bitmap.width, bitmap.height) / 4

            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var count = 0

            val startX = (centerX - sampleSize).coerceAtLeast(0)
            val endX = (centerX + sampleSize).coerceAtMost(bitmap.width)
            val startY = (centerY - sampleSize).coerceAtLeast(0)
            val endY = (centerY + sampleSize).coerceAtMost(bitmap.height)

            // Sample every 4th pixel for performance
            for (x in startX until endX step 4) {
                for (y in startY until endY step 4) {
                    val pixel = bitmap.getPixel(x, y)
                    rSum += Color.red(pixel)
                    gSum += Color.green(pixel)
                    bSum += Color.blue(pixel)
                    count++
                }
            }

            return if (count > 0) {
                Color.rgb(
                    (rSum / count).toInt(),
                    (gSum / count).toInt(),
                    (bSum / count).toInt()
                )
            } else {
                Color.GRAY
            }
        }
    }
}
