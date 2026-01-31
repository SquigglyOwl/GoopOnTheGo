package com.example.pokemongoop.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pokemongoop.GoopApplication
import com.example.pokemongoop.databinding.ActivityHomeBinding
import com.example.pokemongoop.ui.ar.ARScanActivity
import com.example.pokemongoop.ui.collection.CollectionActivity
import com.example.pokemongoop.ui.map.HabitatMapActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var challengeAdapter: ChallengeAdapter

    private val repository by lazy {
        (application as GoopApplication).repository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        observeData()
        checkDailyLogin()
    }

    private fun setupUI() {
        // Setup challenges RecyclerView
        challengeAdapter = ChallengeAdapter()
        binding.challengesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = challengeAdapter
        }

        // Navigation buttons
        binding.scanButton.setOnClickListener {
            startActivity(Intent(this, ARScanActivity::class.java))
        }

        binding.collectionButton.setOnClickListener {
            startActivity(Intent(this, CollectionActivity::class.java))
        }

        binding.mapButton.setOnClickListener {
            startActivity(Intent(this, HabitatMapActivity::class.java))
        }
    }

    private fun observeData() {
        // Observe player stats
        lifecycleScope.launch {
            repository.getPlayerStats().collectLatest { stats ->
                stats?.let {
                    binding.playerNameText.text = it.playerName
                    binding.playerLevelText.text = "Level ${it.calculateLevel()}"
                    binding.totalCaughtText.text = it.totalCaught.toString()
                    binding.totalEvolvedText.text = it.totalEvolved.toString()
                    binding.dailyStreakText.text = it.dailyStreak.toString()
                }
            }
        }

        // Observe daily challenges
        lifecycleScope.launch {
            repository.getActiveChallenges().collectLatest { challenges ->
                challengeAdapter.submitList(challenges)
            }
        }
    }

    private fun checkDailyLogin() {
        lifecycleScope.launch {
            repository.checkAndUpdateDailyStreak()
            repository.generateDailyChallenges()
        }
    }
}
