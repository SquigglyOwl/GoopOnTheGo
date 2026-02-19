package com.example.gooponthego.ui.collection

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gooponthego.GoopApplication
import com.example.gooponthego.R
import com.example.gooponthego.data.database.dao.PlayerCreatureWithDetails
import com.example.gooponthego.databinding.ActivityCollectionBinding
import com.example.gooponthego.models.GoopType
import com.example.gooponthego.ui.evolution.EvolutionActivity
import com.example.gooponthego.ui.evolution.FusionActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollectionBinding
    private lateinit var creatureAdapter: CreatureAdapter

    private var allCreatures: List<PlayerCreatureWithDetails> = emptyList()
    private var currentFilter: GoopType? = null
    private var showFavoritesOnly = false

    private val repository by lazy {
        (application as GoopApplication).repository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCollectionBinding.inflate(layoutInflater)
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

        // Setup RecyclerView
        creatureAdapter = CreatureAdapter { playerCreatureWithDetails ->
            // Open creature detail
            val intent = Intent(this, CreatureDetailActivity::class.java).apply {
                putExtra(EXTRA_CREATURE_ID, playerCreatureWithDetails.playerCreature.id)
            }
            startActivity(intent)
        }

        binding.collectionRecyclerView.apply {
            layoutManager = GridLayoutManager(this@CollectionActivity, 3)
            adapter = creatureAdapter
        }

        // Setup filter chips
        binding.chipAll.setOnClickListener {
            currentFilter = null
            showFavoritesOnly = false
            applyFilter()
        }

        binding.chipWater.setOnClickListener {
            currentFilter = GoopType.WATER
            showFavoritesOnly = false
            applyFilter()
        }

        binding.chipFire.setOnClickListener {
            currentFilter = GoopType.FIRE
            showFavoritesOnly = false
            applyFilter()
        }

        binding.chipNature.setOnClickListener {
            currentFilter = GoopType.NATURE
            showFavoritesOnly = false
            applyFilter()
        }

        binding.chipElectric.setOnClickListener {
            currentFilter = GoopType.ELECTRIC
            showFavoritesOnly = false
            applyFilter()
        }

        binding.chipShadow.setOnClickListener {
            currentFilter = GoopType.SHADOW
            showFavoritesOnly = false
            applyFilter()
        }

        binding.chipFavorites.setOnClickListener {
            currentFilter = null
            showFavoritesOnly = true
            applyFilter()
        }

        // Bottom buttons
        binding.evolutionButton.setOnClickListener {
            startActivity(Intent(this, EvolutionActivity::class.java))
        }

        binding.fusionButton.setOnClickListener {
            startActivity(Intent(this, FusionActivity::class.java))
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repository.getAllPlayerCreaturesWithDetails().collectLatest { creatures ->
                allCreatures = creatures
                applyFilter()
                updateUI(creatures)
            }
        }
    }

    private fun applyFilter() {
        val filtered = allCreatures.filter { details ->
            val typeMatch = currentFilter == null || details.creature.type == currentFilter
            val favoriteMatch = !showFavoritesOnly || details.playerCreature.isFavorite
            typeMatch && favoriteMatch
        }
        creatureAdapter.submitList(filtered)

        // Show/hide empty state
        binding.emptyStateLayout.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.collectionRecyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateUI(creatures: List<PlayerCreatureWithDetails>) {
        binding.collectionCountText.text = "${creatures.size} Goops"
    }

    companion object {
        const val EXTRA_CREATURE_ID = "creature_id"
    }
}
