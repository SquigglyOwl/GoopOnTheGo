package com.example.gooponthego.ui.map

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.gooponthego.models.GoopType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class HabitatMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var playerLat = 0.0
    private var playerLng = 0.0

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1a1a2e")
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2a2a4e")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    private val playerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4FC3F7")
        maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.NORMAL)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    // Generate random habitat zones around the player
    private var habitatZones = mutableListOf<HabitatZone>()

    data class HabitatZone(
        val type: GoopType,
        val offsetX: Float,
        val offsetY: Float,
        val radius: Float
    )

    init {
        generateHabitatZones()
    }

    private fun generateHabitatZones() {
        habitatZones.clear()
        val random = Random(System.currentTimeMillis())

        // Generate 8-12 random habitat zones
        val zoneCount = 8 + random.nextInt(5)

        for (i in 0 until zoneCount) {
            val angle = random.nextDouble() * 2 * Math.PI
            val distance = 100f + random.nextFloat() * 300f
            val offsetX = (cos(angle) * distance).toFloat()
            val offsetY = (sin(angle) * distance).toFloat()
            val radius = 60f + random.nextFloat() * 80f
            val type = GoopType.getBasicTypes()[random.nextInt(5)]

            habitatZones.add(HabitatZone(type, offsetX, offsetY, radius))
        }
    }

    fun setPlayerLocation(lat: Double, lng: Double) {
        playerLat = lat
        playerLng = lng
        // Regenerate zones based on new location seed
        generateHabitatZones()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        // Draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Draw grid
        drawGrid(canvas, centerX, centerY)

        // Draw habitat zones
        for (zone in habitatZones) {
            drawHabitatZone(canvas, centerX + zone.offsetX, centerY + zone.offsetY, zone)
        }

        // Draw player position (center)
        drawPlayer(canvas, centerX, centerY)

        // Draw compass directions
        drawCompass(canvas, centerX, centerY)
    }

    private fun drawGrid(canvas: Canvas, centerX: Float, centerY: Float) {
        val gridSize = 50f

        // Vertical lines
        var x = centerX % gridSize
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += gridSize
        }

        // Horizontal lines
        var y = centerY % gridSize
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += gridSize
        }
    }

    private fun drawHabitatZone(canvas: Canvas, x: Float, y: Float, zone: HabitatZone) {
        // Draw zone circle
        val zonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = zone.type.primaryColor
            alpha = 60
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = zone.type.primaryColor
            alpha = 150
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        canvas.drawCircle(x, y, zone.radius, zonePaint)
        canvas.drawCircle(x, y, zone.radius, borderPaint)

        // Draw zone icon/label
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = zone.type.primaryColor
            textSize = 20f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        val emoji = when (zone.type) {
            GoopType.WATER -> "💧"
            GoopType.FIRE -> "🔥"
            GoopType.NATURE -> "🌿"
            GoopType.ELECTRIC -> "⚡"
            GoopType.SHADOW -> "🌙"
            else -> "✨"
        }

        canvas.drawText(emoji, x, y + 8f, labelPaint)
    }

    private fun drawPlayer(canvas: Canvas, x: Float, y: Float) {
        // Draw glow
        canvas.drawCircle(x, y, 30f, playerGlowPaint)

        // Draw player marker
        canvas.drawCircle(x, y, 15f, playerPaint)

        // Draw inner circle
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4FC3F7")
        }
        canvas.drawCircle(x, y, 8f, innerPaint)

        // Draw "You" label
        val youPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("YOU", x, y - 35f, youPaint)
    }

    private fun drawCompass(canvas: Canvas, centerX: Float, centerY: Float) {
        val compassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#666666")
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }

        // Draw cardinal directions
        canvas.drawText("N", centerX, 40f, compassPaint)
        canvas.drawText("S", centerX, height - 20f, compassPaint)
        canvas.drawText("W", 30f, centerY, compassPaint)
        canvas.drawText("E", width - 30f, centerY, compassPaint)

        // Draw range circles
        val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#333366")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        canvas.drawCircle(centerX, centerY, 100f, rangePaint)
        canvas.drawCircle(centerX, centerY, 200f, rangePaint)
        canvas.drawCircle(centerX, centerY, 300f, rangePaint)
    }
}
