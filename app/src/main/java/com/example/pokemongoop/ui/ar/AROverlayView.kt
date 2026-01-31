package com.example.pokemongoop.ui.ar

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.example.pokemongoop.data.database.entities.Creature
import com.example.pokemongoop.models.GoopType
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class AROverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var creature: Creature? = null
    private var creatureX = 0f
    private var creatureY = 0f
    private var creatureSize = 150f
    private var bounceOffset = 0f
    private var glowRadius = 0f
    private var isAnimating = false

    // Tap-to-catch listener - returns creature and success based on rarity
    var onCreatureTapped: ((Creature, Boolean) -> Unit)? = null

    // Arrow indicator paint
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val creaturePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
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

        // Random position on screen
        creatureX = width * (0.2f + Random.nextFloat() * 0.6f)
        creatureY = height * (0.3f + Random.nextFloat() * 0.3f)

        // Set colors based on type
        creaturePaint.color = creature.type.primaryColor
        glowPaint.color = creature.type.secondaryColor
        glowPaint.alpha = 100

        startAnimations()
        invalidate()
    }

    fun hideCreature() {
        creature = null
        stopAnimations()
        invalidate()
    }

    fun hasCreature(): Boolean = creature != null

    fun getCreature(): Creature? = creature

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && creature != null) {
            val touchX = event.x
            val touchY = event.y

            // Check if tap is on the creature (with some tolerance)
            val distance = sqrt(
                (touchX - creatureX) * (touchX - creatureX) +
                (touchY - (creatureY - bounceOffset)) * (touchY - (creatureY - bounceOffset))
            )

            val tapRadius = creatureSize * 0.8f // Generous tap area

            if (distance <= tapRadius) {
                creature?.let { c ->
                    // Calculate catch success based on rarity
                    // Common (1): 70%, Uncommon (2): 55%, Rare (3): 40%, Epic (4): 25%, Legendary (5): 15%
                    val catchRate = when (c.rarity) {
                        1 -> 0.70f
                        2 -> 0.55f
                        3 -> 0.40f
                        4 -> 0.25f
                        5 -> 0.15f
                        else -> 0.50f
                    }

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
                bounceOffset = (animation.animatedValue as Float) * 20f
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

            // Check if creature is off-screen or near edges - draw arrow indicator
            val screenCenterX = width / 2f
            val screenCenterY = height / 2f
            val margin = creatureSize

            val isOffScreen = centerX < margin || centerX > width - margin ||
                    centerY < margin || centerY > height - margin

            if (isOffScreen) {
                drawArrowIndicator(canvas, screenCenterX, screenCenterY, centerX, centerY, c.type)
            }

            // Draw glow effect
            glowPaint.maskFilter = BlurMaskFilter(glowRadius + 20f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawCircle(centerX, centerY, creatureSize / 2 + glowRadius, glowPaint)

            // Draw creature body (blob shape)
            drawGoopBody(canvas, centerX, centerY, c.type)

            // Draw eyes
            drawEyes(canvas, centerX, centerY)

            // Draw type indicator
            drawTypeIndicator(canvas, centerX, centerY + creatureSize / 2 + 30, c.type)
        }
    }

    private fun drawArrowIndicator(canvas: Canvas, fromX: Float, fromY: Float, toX: Float, toY: Float, type: GoopType) {
        // Calculate direction to creature
        val dx = toX - fromX
        val dy = toY - fromY
        val distance = sqrt(dx * dx + dy * dy)
        if (distance == 0f) return

        // Normalize and position arrow at edge of screen center area
        val arrowDistance = 150f
        val arrowX = fromX + (dx / distance) * arrowDistance
        val arrowY = fromY + (dy / distance) * arrowDistance

        // Calculate arrow rotation angle
        val angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat()

        // Draw arrow
        canvas.save()
        canvas.translate(arrowX, arrowY)
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat())

        // Arrow shape pointing right (will be rotated)
        val arrowPath = Path().apply {
            moveTo(30f, 0f)      // Tip
            lineTo(-15f, -20f)  // Top back
            lineTo(-5f, 0f)     // Inner notch
            lineTo(-15f, 20f)   // Bottom back
            close()
        }

        // Draw with creature's color
        arrowPaint.color = type.primaryColor
        canvas.drawPath(arrowPath, arrowPaint)

        // Draw outline
        val outlinePaint = Paint(arrowPaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.WHITE
        }
        canvas.drawPath(arrowPath, outlinePaint)

        canvas.restore()

        // Draw pulsing circle around arrow
        val pulseRadius = 40f + (glowRadius / 2)
        val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = type.secondaryColor
            alpha = 100
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(arrowX, arrowY, pulseRadius, pulsePaint)
    }

    private fun drawGoopBody(canvas: Canvas, cx: Float, cy: Float, type: GoopType) {
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
            textSize = 32f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = type.secondaryColor
        }

        val text = type.displayName
        val textWidth = textPaint.measureText(text)
        val padding = 16f

        val rect = RectF(
            cx - textWidth / 2 - padding,
            cy - 20,
            cx + textWidth / 2 + padding,
            cy + 20
        )

        canvas.drawRoundRect(rect, 10f, 10f, bgPaint)
        canvas.drawText(text, cx, cy + 10, textPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimations()
    }
}
