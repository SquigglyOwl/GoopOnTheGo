package com.example.gooponthego.ui.collection

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOption(val displayName: String) {
    NAME("Name"),
    RARITY("Rarity"),
    DATE_CAUGHT("Date Caught")
}

class CollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollectionBinding
    private lateinit var creatureAdapter: CreatureAdapter

    private var allCreatures: List<PlayerCreatureWithDetails> = emptyList()
    private var currentFilter: GoopType? = null
    private var showFavoritesOnly = false
    private var currentSortOption: SortOption = SortOption.DATE_CAUGHT
    private var currentSearchQuery: String = ""

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

        // Setup sort spinner
        val sortAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            SortOption.entries.map { it.displayName }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.sortSpinner.adapter = sortAdapter
        binding.sortSpinner.setSelection(SortOption.entries.indexOf(currentSortOption))
        binding.sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSortOption = SortOption.entries[position]
                applyFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup search bar
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString() ?: ""
                applyFilter()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Bottom buttons
        binding.releaseAllDuplicatesButton.setOnClickListener {
            binding.releaseAllDuplicatesButton.isEnabled = false
            lifecycleScope.launch {
                val released = repository.releaseAllDuplicates()
                withContext(Dispatchers.Main) {
                    binding.releaseAllDuplicatesButton.isEnabled = true
                    val message = if (released > 0) {
                        "Released $released duplicate creature${if (released != 1) "s" else ""}!"
                    } else {
                        "No duplicates to release (max 3 per species kept)"
                    }
                    Toast.makeText(this@CollectionActivity, message, Toast.LENGTH_LONG).show()
                }
            }
        }

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
            val searchMatch = currentSearchQuery.isEmpty() ||
                    details.creature.name.contains(currentSearchQuery, ignoreCase = true) ||
                    details.playerCreature.nickname?.contains(currentSearchQuery, ignoreCase = true) == true
            typeMatch && favoriteMatch && searchMatch
        }

        val sorted = when (currentSortOption) {
            SortOption.NAME -> filtered.sortedBy { it.creature.name.lowercase() }
            SortOption.RARITY -> filtered.sortedByDescending { it.creature.rarity }
            SortOption.DATE_CAUGHT -> filtered.sortedByDescending { it.playerCreature.caughtDate }
        }

        creatureAdapter.submitList(sorted)

        // Show/hide empty state
        binding.emptyStateLayout.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
        binding.collectionRecyclerView.visibility = if (sorted.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateUI(creatures: List<PlayerCreatureWithDetails>) {
        binding.collectionCountText.text = "${creatures.size} Goops"
    }

    companion object {
        const val EXTRA_CREATURE_ID = "creature_id"
    }
}
