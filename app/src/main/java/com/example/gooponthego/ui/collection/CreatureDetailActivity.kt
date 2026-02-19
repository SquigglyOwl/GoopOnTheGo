package com.example.gooponthego.ui.collection

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.gooponthego.GoopApplication
import com.example.gooponthego.R
import com.example.gooponthego.data.database.dao.PlayerCreatureWithDetails
import com.example.gooponthego.databinding.ActivityCreatureDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreatureDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatureDetailBinding
    private var creatureDetails: PlayerCreatureWithDetails? = null

    private val repository by lazy {
        (application as GoopApplication).repository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreatureDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val creatureId = intent.getLongExtra(CollectionActivity.EXTRA_CREATURE_ID, -1)
        if (creatureId == -1L) {
            finish()
            return
        }

        setupUI()
        loadCreature(creatureId)
    }

    private fun setupUI() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.favoriteButton.setOnClickListener {
            toggleFavorite()
        }

        binding.creatureNameEdit.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveNickname(v.text.toString())
                true
            } else false
        }

        binding.evolveButton.setOnClickListener {
            evolveCreature()
        }

        binding.releaseButton.setOnClickListener {
            showReleaseConfirmation()
        }
    }

    private fun loadCreature(id: Long) {
        lifecycleScope.launch {
            val details = repository.getPlayerCreatureWithDetails(id)
            details?.let {
                creatureDetails = it
                withContext(Dispatchers.Main) {
                    displayCreature(it)
                }
            } ?: run {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreatureDetailActivity, "Creature not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun displayCreature(details: PlayerCreatureWithDetails) {
        val creature = details.creature
        val playerCreature = details.playerCreature

        // Set visual - use image if available
        val resName = creature.imageResName
        val resId = if (resName != null) {
            resources.getIdentifier(resName, "drawable", packageName)
        } else 0

        if (resId != 0) {
            binding.creatureVisual.background = ContextCompat.getDrawable(this, resId)
        } else {
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(creature.type.primaryColor)
                setStroke(8, creature.type.secondaryColor)
            }
            binding.creatureVisual.background = drawable
        }

        // Set name
        binding.creatureNameEdit.setText(playerCreature.nickname ?: creature.name)
        binding.creatureNameEdit.hint = creature.name

        // Set type
        binding.typeText.text = creature.type.displayName
        binding.typeText.setTextColor(creature.type.primaryColor)

        // Set stats
        binding.healthBar.progress = creature.baseHealth
        binding.healthText.text = creature.baseHealth.toString()

        binding.attackBar.progress = creature.baseAttack
        binding.attackText.text = creature.baseAttack.toString()

        binding.defenseBar.progress = creature.baseDefense
        binding.defenseText.text = creature.baseDefense.toString()

        // Set evolution progress (need 3 to evolve)
        lifecycleScope.launch {
            val count = repository.getEvolveCount(creature.id)
            val canEvolve = creature.evolvesToId != null
            withContext(Dispatchers.Main) {
                if (canEvolve) {
                    val progress = (count * 100) / 3
                    binding.expBar.progress = progress.coerceIn(0, 100)
                    binding.expText.text = "You have $count/3 - Merge 3 to evolve!"
                    binding.evolveButton.isEnabled = count >= 3
                } else {
                    binding.expBar.progress = 100
                    binding.expText.text = "Max Evolution"
                    binding.evolveButton.isEnabled = false
                }
            }
        }

        // Set description
        binding.descriptionText.text = creature.description

        // Set favorite button
        updateFavoriteButton(playerCreature.isFavorite)
    }

    private fun updateFavoriteButton(isFavorite: Boolean) {
        val iconRes = if (isFavorite) {
            android.R.drawable.btn_star_big_on
        } else {
            android.R.drawable.btn_star_big_off
        }
        binding.favoriteButton.setImageResource(iconRes)
    }

    private fun toggleFavorite() {
        val details = creatureDetails ?: return
        lifecycleScope.launch {
            repository.toggleFavorite(details.playerCreature.id)
            loadCreature(details.playerCreature.id)
        }
    }

    private fun saveNickname(nickname: String) {
        val details = creatureDetails ?: return
        val newNickname = nickname.trim().ifEmpty { null }

        lifecycleScope.launch {
            repository.updateNickname(details.playerCreature.id, newNickname)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@CreatureDetailActivity, "Nickname saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun evolveCreature() {
        val details = creatureDetails ?: return

        lifecycleScope.launch {
            val evolved = repository.evolveCreature(details.playerCreature.id)
            withContext(Dispatchers.Main) {
                if (evolved != null) {
                    Toast.makeText(this@CreatureDetailActivity, "Evolution successful!", Toast.LENGTH_SHORT).show()
                    // Reload with evolved creature
                    loadCreature(evolved.id)
                } else {
                    Toast.makeText(this@CreatureDetailActivity, "Cannot evolve yet", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showReleaseConfirmation() {
        val details = creatureDetails ?: return
        val name = details.playerCreature.nickname ?: details.creature.name

        AlertDialog.Builder(this)
            .setTitle("Release $name?")
            .setMessage("Are you sure you want to release this creature? This cannot be undone.")
            .setPositiveButton("Release") { _, _ ->
                releaseCreature()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun releaseCreature() {
        val details = creatureDetails ?: return

        lifecycleScope.launch {
            val database = (application as GoopApplication).database
            database.playerCreatureDao().delete(details.playerCreature)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@CreatureDetailActivity, "Goodbye, ${details.creature.name}!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
