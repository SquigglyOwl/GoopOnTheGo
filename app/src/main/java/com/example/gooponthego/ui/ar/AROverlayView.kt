package com.example.gooponthego.ui.ar

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.gooponthego.data.database.entities.Creature
import com.example.gooponthego.models.GoopType
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class AROverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var creature: Creature? = null
    private var creatureBitmap: Bitmap? = null
    private var creatureX = 0f
    private var creatureY = 0f
    private var creatureSize = 200f
    private var bounceOffset = 0f
    private var glowRadius = 0f
    private var isAnimating = false
    private var habitatMatch = false

    // Tap-to-catch listener
    var onCreatureTapped: ((Creature, Boolean) -> Unit)? = null

    private val creaturePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }

    private var bounceAnimator: ValueAnimator? = null
    private var glowAnimator: ValueAnimator? = null

    fun showCreature(creature: Creature) {
        this.creature = creature

        // Random position on screen (keep away from edges)
        creatureX = width * (0.2f + Random.nextFloat() * 0.6f)
        creatureY = height * (0.25f + Random.nextFloat() * 0.4f)

        // Set colors based on type
        creaturePaint.color = creature.type.primaryColor
        glowPaint.color = creature.type.secondaryColor
        glowPaint.alpha = 100

        // Load creature image if available
        creatureBitmap = loadCreatureImage(creature)

        startAnimations()
        invalidate()
    }

    private fun loadCreatureImage(creature: Creature): Bitmap? {
        val resName = creature.imageResName ?: return null
        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        if (resId == 0) return null

        return try {
            val drawable = ContextCompat.getDrawable(context, resId) ?: return null
            val size = creatureSize.toInt()
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun hideCreature() {
        creature = null
        creatureBitmap?.recycle()
        creatureBitmap = null
        stopAnimations()
        invalidate()
    }

    fun hasCreature(): Boolean = creature != null

    fun getCreature(): Creature? = creature

    fun setHabitatMatch(match: Boolean) {
        habitatMatch = match
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && creature != null) {
            val touchX = event.x
            val touchY = event.y

            // Check if tap is on the creature
            val currentY = creatureY - bounceOffset
            val distance = sqrt(
                (touchX - creatureX) * (touchX - creatureX) +
                (touchY - currentY) * (touchY - currentY)
            )

            val tapRadius = creatureSize * 0.9f

            if (distance <= tapRadius) {
                creature?.let { c ->
                    // Base catch rate by rarity
                    val baseCatchRate = when (c.rarity) {
                        1 -> 0.55f  // Common
                        2 -> 0.40f  // Uncommon
                        3 -> 0.28f  // Rare
                        4 -> 0.18f  // Epic
                        5 -> 0.10f  // Legendary
                        else -> 0.40f
                    }
                    // +15% bonus when creature type matches current habitat
                    val catchRate = if (habitatMatch) (baseCatchRate + 0.15f).coerceAtMost(0.95f) else baseCatchRate

                    val success = Random.nextFloat() < catchRate
                    onCreatureTapped?.invoke(c, success)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun startAnimations() {
        if (isAnimating) return
        isAnimating = true

        // Bounce animation
        bounceAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                bounceOffset = (animation.animatedValue as Float) * 25f
                invalidate()
            }
            start()
        }

        // Glow animation
        glowAnimator = ValueAnimator.ofFloat(0f, 30f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animation ->
                glowRadius = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopAnimations() {
        isAnimating = false
        bounceAnimator?.cancel()
        glowAnimator?.cancel()
        bounceAnimator = null
        glowAnimator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        creature?.let { c ->
            val centerX = creatureX
            val centerY = creatureY - bounceOffset

            // Draw glow effect
            glowPaint.maskFilter = BlurMaskFilter(glowRadius + 20f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawCircle(centerX, centerY, creatureSize / 2 + glowRadius, glowPaint)

            // Draw creature - use image if available, otherwise fallback to blob
            val bitmap = creatureBitmap
            if (bitmap != null && !bitmap.isRecycled) {
                // Draw image
                val left = centerX - creatureSize / 2
                val top = centerY - creatureSize / 2
                canvas.drawBitmap(bitmap, left, top, bitmapPaint)
            } else {
                // Fallback: Draw blob shape
                drawGoopBody(canvas, centerX, centerY, c.type)
                drawEyes(canvas, centerX, centerY)
            }

            // Draw type indicator
            drawTypeIndicator(canvas, centerX, centerY + creatureSize / 2 + 40, c.type)
        }
    }


    private fun drawGoopBody(canvas: Canvas, cx: Float, cy: Float, @Suppress("UNUSED_PARAMETER") type: GoopType) {
        val path = Path()
        val radius = creatureSize / 2

        // Create wobbly blob shape
        val points = 12
        val wobbleAmount = 15f

        for (i in 0 until points) {
            val angle = (i.toFloat() / points) * 2 * Math.PI
            val wobble = sin(angle * 3 + System.currentTimeMillis() / 200.0) * wobbleAmount
            val r = radius + wobble.toFloat()
            val x = cx + (r * kotlin.math.cos(angle)).toFloat()
            val y = cy + (r * kotlin.math.sin(angle)).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        // Draw shadow
        val shadowPaint = Paint(creaturePaint)
        shadowPaint.color = Color.argb(50, 0, 0, 0)
        canvas.drawCircle(cx + 5, cy + creatureSize / 2 + 10, radius * 0.8f, shadowPaint)

        // Draw body
        creaturePaint.style = Paint.Style.FILL
        canvas.drawPath(path, creaturePaint)

        // Draw highlight
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        highlightPaint.color = Color.WHITE
        highlightPaint.alpha = 80
        canvas.drawCircle(cx - radius * 0.3f, cy - radius * 0.3f, radius * 0.2f, highlightPaint)
    }

    private fun drawEyes(canvas: Canvas, cx: Float, cy: Float) {
        val eyeOffsetX = creatureSize * 0.15f
        val eyeOffsetY = creatureSize * 0.1f
        val eyeRadius = creatureSize * 0.12f
        val pupilRadius = eyeRadius * 0.5f

        // Left eye
        canvas.drawCircle(cx - eyeOffsetX, cy - eyeOffsetY, eyeRadius, eyePaint)
        canvas.drawCircle(cx - eyeOffsetX + 2, cy - eyeOffsetY, pupilRadius, pupilPaint)

        // Right eye
        canvas.drawCircle(cx + eyeOffsetX, cy - eyeOffsetY, eyeRadius, eyePaint)
        canvas.drawCircle(cx + eyeOffsetX + 2, cy - eyeOffsetY, pupilRadius, pupilPaint)
    }

    private fun drawTypeIndicator(canvas: Canvas, cx: Float, cy: Float, type: GoopType) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = type.secondaryColor
        }

        val text = type.displayName
        val textWidth = textPaint.measureText(text)
        val padding = 20f

        val rect = RectF(
            cx - textWidth / 2 - padding,
            cy - 24,
            cx + textWidth / 2 + padding,
            cy + 24
        )

        canvas.drawRoundRect(rect, 12f, 12f, bgPaint)
        canvas.drawText(text, cx, cy + 12, textPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimations()
    }
}
