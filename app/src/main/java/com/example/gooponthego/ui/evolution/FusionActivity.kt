package com.example.gooponthego.ui.evolution

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gooponthego.GoopApplication
import com.example.gooponthego.data.database.dao.PlayerCreatureWithDetails
import com.example.gooponthego.data.database.entities.Creature
import com.example.gooponthego.databinding.ActivityFusionBinding
import com.example.gooponthego.models.GoopType
import com.example.gooponthego.ui.collection.CreatureAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FusionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFusionBinding
    private lateinit var creatureAdapter: CreatureAdapter

    private var slot1Creature: PlayerCreatureWithDetails? = null
    private var slot2Creature: PlayerCreatureWithDetails? = null
    private var selectingSlot = 1
    private var fusionResult: Creature? = null

    private val repository by lazy {
        (application as GoopApplication).repository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFusionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        observeData()
    }

    private fun setupUI() {
        binding.backButton.setOnClickListener {
            finish()
        }

        creatureAdapter = CreatureAdapter { details ->
            selectCreature(details)
        }

        binding.creaturesRecyclerView.apply {
            layoutManager = GridLayoutManager(this@FusionActivity, 3)
            adapter = creatureAdapter
        }

        binding.slot1Card.setOnClickListener {
            selectingSlot = 1
            highlightSelectedSlot()
        }

        binding.slot2Card.setOnClickListener {
            selectingSlot = 2
            highlightSelectedSlot()
        }

        binding.fuseButton.setOnClickListener {
            performFusion()
        }

        highlightSelectedSlot()
    }

    private fun highlightSelectedSlot() {
        // Visual feedback for which slot is being selected
        binding.slot1Card.alpha = if (selectingSlot == 1) 1f else 0.7f
        binding.slot2Card.alpha = if (selectingSlot == 2) 1f else 0.7f
    }

    private fun observeData() {
        lifecycleScope.launch {
            repository.getAllPlayerCreaturesWithDetails().collectLatest { creatures ->
                // Only show base types — hybrids cannot be fused further
                val fusible = creatures.filter { it.creature.type in GoopType.getBasicTypes() }
                creatureAdapter.submitList(fusible)
            }
        }
    }

    private fun selectCreature(details: PlayerCreatureWithDetails) {
        if (selectingSlot == 1) {
            if (details.playerCreature.id == slot2Creature?.playerCreature?.id) {
                Toast.makeText(this, "Already selected for slot 2", Toast.LENGTH_SHORT).show()
                return
            }
            // If slot 2 is filled, check compatibility before accepting
            val other = slot2Creature
            if (other != null && GoopType.getFusionResult(details.creature.type, other.creature.type) == null) {
                Toast.makeText(this, "${details.creature.type.displayName} can't fuse with ${other.creature.type.displayName}", Toast.LENGTH_SHORT).show()
                return
            }
            slot1Creature = details
            updateSlot1UI(details)
            selectingSlot = 2
        } else {
            if (details.playerCreature.id == slot1Creature?.playerCreature?.id) {
                Toast.makeText(this, "Already selected for slot 1", Toast.LENGTH_SHORT).show()
                return
            }
            // If slot 1 is filled, check compatibility before accepting
            val other = slot1Creature
            if (other != null && GoopType.getFusionResult(other.creature.type, details.creature.type) == null) {
                Toast.makeText(this, "${other.creature.type.displayName} can't fuse with ${details.creature.type.displayName}", Toast.LENGTH_SHORT).show()
                return
            }
            slot2Creature = details
            updateSlot2UI(details)
            selectingSlot = 1
        }

        highlightSelectedSlot()
        checkFusionPossible()
    }

    private fun updateSlot1UI(details: PlayerCreatureWithDetails) {
        binding.slot1Visual.visibility = View.VISIBLE
        binding.slot1Text.text = details.creature.name
        setGoopImage(binding.slot1Visual, details.creature)
    }

    private fun updateSlot2UI(details: PlayerCreatureWithDetails) {
        binding.slot2Visual.visibility = View.VISIBLE
        binding.slot2Text.text = details.creature.name
        setGoopImage(binding.slot2Visual, details.creature)
    }

    private fun setGoopImage(imageView: android.widget.ImageView, creature: Creature) {
        val resName = creature.imageResName
        val resId = if (resName != null) {
            resources.getIdentifier(resName, "drawable", packageName)
        } else 0

        if (resId != 0) {
            imageView.setImageDrawable(ContextCompat.getDrawable(this, resId))
        } else {
            imageView.setImageDrawable(null)
        }
    }

    private fun checkFusionPossible() {
        val s1 = slot1Creature
        val s2 = slot2Creature

        if (s1 == null || s2 == null) {
            binding.resultCard.visibility = View.GONE
            binding.unknownFusionText.visibility = View.GONE
            binding.fuseButton.isEnabled = false
            return
        }

        // Check if fusion is possible
        lifecycleScope.launch {
            val recipe = repository.findFusionRecipe(s1.creature.type, s2.creature.type)

            withContext(Dispatchers.Main) {
                if (recipe != null) {
                    // Show fusion result
                    loadFusionResult(recipe.resultCreatureId)
                } else {
                    // Show error
                    binding.resultCard.visibility = View.GONE
                    binding.unknownFusionText.visibility = View.VISIBLE
                    binding.fuseButton.isEnabled = false
                    fusionResult = null
                }
            }
        }
    }

    private fun loadFusionResult(creatureId: Long) {
        lifecycleScope.launch {
            val creature = repository.getCreatureById(creatureId)

            withContext(Dispatchers.Main) {
                if (creature != null) {
                    fusionResult = creature
                    showFusionResult(creature)
                }
            }
        }
    }

    private fun showFusionResult(creature: Creature) {
        binding.resultCard.visibility = View.VISIBLE
        binding.unknownFusionText.visibility = View.GONE
        binding.fuseButton.isEnabled = true

        binding.resultNameText.text = creature.name
        binding.resultTypeText.text = "${creature.type.displayName} Type"

        setGoopImage(binding.resultVisual, creature)
    }

    private fun performFusion() {
        val s1 = slot1Creature ?: return
        val s2 = slot2Creature ?: return

        binding.fuseButton.isEnabled = false

        playFusionAnimation {
            lifecycleScope.launch {
                val result = repository.fuseCreatures(s1.playerCreature.id, s2.playerCreature.id)

                withContext(Dispatchers.Main) {
                    if (result != null) {
                        // Flash the result card in
                        binding.resultCard.alpha = 0f
                        binding.resultCard.scaleX = 0.5f
                        binding.resultCard.scaleY = 0.5f
                        AnimatorSet().apply {
                            playTogether(
                                ObjectAnimator.ofFloat(binding.resultCard, View.ALPHA, 0f, 1f),
                                ObjectAnimator.ofFloat(binding.resultCard, View.SCALE_X, 0.5f, 1f),
                                ObjectAnimator.ofFloat(binding.resultCard, View.SCALE_Y, 0.5f, 1f)
                            )
                            duration = 400
                            start()
                        }

                        Toast.makeText(
                            this@FusionActivity,
                            "Fusion successful! Created ${fusionResult?.name}!",
                            Toast.LENGTH_LONG
                        ).show()

                        // Reset slots
                        slot1Creature = null
                        slot2Creature = null
                        fusionResult = null

                        binding.slot1Visual.visibility = View.INVISIBLE
                        binding.slot1Text.text = "Tap to select"
                        binding.slot2Visual.visibility = View.INVISIBLE
                        binding.slot2Text.text = "Tap to select"
                        binding.resultCard.visibility = View.GONE
                        binding.fuseButton.isEnabled = false
                    } else {
                        // Restore slots on failure
                        binding.slot1Card.alpha = 1f
                        binding.slot2Card.alpha = 1f
                        Toast.makeText(
                            this@FusionActivity,
                            "Fusion failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.fuseButton.isEnabled = true
                    }
                }
            }
        }
    }

    private fun playFusionAnimation(onComplete: () -> Unit) {
        // Both slots scale down and fade out toward centre
        val slot1ScaleX = ObjectAnimator.ofFloat(binding.slot1Card, View.SCALE_X, 1f, 0f)
        val slot1ScaleY = ObjectAnimator.ofFloat(binding.slot1Card, View.SCALE_Y, 1f, 0f)
        val slot1Fade   = ObjectAnimator.ofFloat(binding.slot1Card, View.ALPHA, 1f, 0f)
        val slot2ScaleX = ObjectAnimator.ofFloat(binding.slot2Card, View.SCALE_X, 1f, 0f)
        val slot2ScaleY = ObjectAnimator.ofFloat(binding.slot2Card, View.SCALE_Y, 1f, 0f)
        val slot2Fade   = ObjectAnimator.ofFloat(binding.slot2Card, View.ALPHA, 1f, 0f)

        AnimatorSet().apply {
            playTogether(slot1ScaleX, slot1ScaleY, slot1Fade, slot2ScaleX, slot2ScaleY, slot2Fade)
            duration = 500
            start()
        }

        lifecycleScope.launch {
            delay(500)
            withContext(Dispatchers.Main) {
                // Restore slot cards for next use
                binding.slot1Card.scaleX = 1f
                binding.slot1Card.scaleY = 1f
                binding.slot1Card.alpha  = 1f
                binding.slot2Card.scaleX = 1f
                binding.slot2Card.scaleY = 1f
                binding.slot2Card.alpha  = 1f
                onComplete()
            }
        }
    }
}
