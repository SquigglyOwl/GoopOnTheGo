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

        binding.captureButton.setOnClickListener {
            captureCreature()
        }

        binding.captureButton.isEnabled = false
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
                                trySpawnCreature(dominantColor)
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

        // Start scanning timer
        startScanningLoop()
    }

    private fun startScanningLoop() {
        lifecycleScope.launch {
            while (isScanning) {
                delay(3000) // Check every 3 seconds
                if (!binding.arOverlay.hasCreature() && Random.nextFloat() < 0.3f) {
                    // 30% chance to spawn a creature
                    spawnRandomCreature()
                }
            }
        }
    }

    private fun trySpawnCreature(dominantColor: Int) {
        if (binding.arOverlay.hasCreature()) return

        // Determine creature type based on dominant color
        val type = when {
            isBlueish(dominantColor) -> GoopType.WATER
            isReddish(dominantColor) -> GoopType.FIRE
            isGreenish(dominantColor) -> GoopType.NATURE
            isYellowish(dominantColor) -> GoopType.ELECTRIC
            else -> GoopType.SHADOW
        }

        // Spawn with some randomness
        if (Random.nextFloat() < 0.1f) { // 10% chance when color matches
            spawnCreatureOfType(type)
        }
    }

    private fun spawnRandomCreature() {
        val types = GoopType.getBasicTypes()
        val type = types[Random.nextInt(types.size)]
        spawnCreatureOfType(type)
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
        binding.captureButton.isEnabled = true
        binding.scanningIndicator.visibility = View.GONE

        // Update creature info card
        binding.creatureInfoCard.visibility = View.VISIBLE
        binding.creatureNameText.text = "Wild ${creature.name}!"
        binding.creatureTypeText.text = creature.type.displayName
        binding.creatureTypeText.setTextColor(creature.type.primaryColor)
        binding.creatureRarityText.text = getRarityText(creature.rarity)

        binding.scanStatusText.text = "Creature Found!"
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

    private fun captureCreature() {
        val creature = binding.arOverlay.getCreature() ?: return

        binding.captureButton.isEnabled = false

        // Play capture animation
        playCaptureAnimation {
            // Save creature to database
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

                    // Reset for next creature
                    binding.arOverlay.hideCreature()
                    binding.creatureInfoCard.visibility = View.GONE
                    binding.scanningIndicator.visibility = View.VISIBLE
                    binding.scanStatusText.text = "Scanning..."
                }
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
