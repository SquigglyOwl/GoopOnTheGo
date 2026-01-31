package com.example.pokemongoop.ui.evolution

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.pokemongoop.GoopApplication
import com.example.pokemongoop.data.database.dao.PlayerCreatureWithDetails
import com.example.pokemongoop.data.database.entities.Creature
import com.example.pokemongoop.databinding.ActivityEvolutionBinding
import com.example.pokemongoop.ui.collection.CreatureAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EvolutionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEvolutionBinding
    private lateinit var creatureAdapter: CreatureAdapter

    private var selectedCreature: PlayerCreatureWithDetails? = null
    private var evolutionTarget: Creature? = null

    private val repository by lazy {
        (application as GoopApplication).repository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEvolutionBinding.inflate(layoutInflater)
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
            layoutManager = GridLayoutManager(this@EvolutionActivity, 3)
            adapter = creatureAdapter
        }

        binding.evolveButton.setOnClickListener {
            performEvolution()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repository.getAllPlayerCreaturesWithDetails().collectLatest { creatures ->
                // Filter to only show creatures that can evolve
                val evolvable = creatures.filter { details ->
                    details.creature.evolvesToId != null &&
                            details.playerCreature.experience >= details.creature.experienceToEvolve
                }
                creatureAdapter.submitList(evolvable)

                if (evolvable.isEmpty()) {
                    binding.instructionText.text = "No creatures ready to evolve.\nCatch more and gain experience!"
                }
            }
        }
    }

    private fun selectCreature(details: PlayerCreatureWithDetails) {
        selectedCreature = details

        lifecycleScope.launch {
            val evolution = repository.getCreatureById(details.creature.evolvesToId ?: return@launch)
            evolutionTarget = evolution

            withContext(Dispatchers.Main) {
                if (evolution != null) {
                    showEvolutionPreview(details, evolution)
                }
            }
        }
    }

    private fun showEvolutionPreview(current: PlayerCreatureWithDetails, evolution: Creature) {
        binding.evolutionPreviewCard.visibility = View.VISIBLE
        binding.evolveButton.isEnabled = true

        // Current creature
        binding.currentCreatureName.text = current.playerCreature.nickname ?: current.creature.name
        val currentDrawable = binding.currentCreatureVisual.background as? GradientDrawable
            ?: GradientDrawable().also {
                it.shape = GradientDrawable.OVAL
                binding.currentCreatureVisual.background = it
            }
        currentDrawable.setColor(current.creature.type.primaryColor)

        // Evolution result
        binding.evolvedCreatureName.text = evolution.name
        val evolvedDrawable = binding.evolvedCreatureVisual.background as? GradientDrawable
            ?: GradientDrawable().also {
                it.shape = GradientDrawable.OVAL
                binding.evolvedCreatureVisual.background = it
            }
        evolvedDrawable.setColor(evolution.type.primaryColor)
    }

    private fun performEvolution() {
        val creature = selectedCreature ?: return

        binding.evolveButton.isEnabled = false

        // Play evolution animation
        playEvolutionAnimation {
            lifecycleScope.launch {
                val evolved = repository.evolveCreature(creature.playerCreature.id)

                withContext(Dispatchers.Main) {
                    if (evolved != null) {
                        Toast.makeText(
                            this@EvolutionActivity,
                            "Evolution successful! Welcome ${evolutionTarget?.name}!",
                            Toast.LENGTH_LONG
                        ).show()

                        // Reset selection
                        selectedCreature = null
                        evolutionTarget = null
                        binding.evolutionPreviewCard.visibility = View.GONE
                        binding.evolveButton.isEnabled = false
                    } else {
                        Toast.makeText(
                            this@EvolutionActivity,
                            "Evolution failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.evolveButton.isEnabled = true
                    }
                }
            }
        }
    }

    private fun playEvolutionAnimation(onComplete: () -> Unit) {
        // Glow and scale animation on the evolution preview
        val scaleUp = ObjectAnimator.ofFloat(binding.evolutionPreviewCard, View.SCALE_X, 1f, 1.1f, 1f)
        val scaleUpY = ObjectAnimator.ofFloat(binding.evolutionPreviewCard, View.SCALE_Y, 1f, 1.1f, 1f)

        AnimatorSet().apply {
            playTogether(scaleUp, scaleUpY)
            duration = 1000
            start()
        }

        lifecycleScope.launch {
            delay(1000)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
