package com.example.gooponthego.ui.home

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gooponthego.GoopApplication
import com.example.gooponthego.data.database.entities.DailyChallenge
import com.example.gooponthego.databinding.ActivityHomeBinding
import com.example.gooponthego.ui.ar.ARScanActivity
import com.example.gooponthego.ui.collection.CollectionActivity
import com.example.gooponthego.ui.achievements.AchievementsActivity
import com.example.gooponthego.ui.map.HabitatMapActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var challengeAdapter: ChallengeAdapter

    private val repository by lazy {
        (application as GoopApplication).repository
    }

    // Track last known completed count to detect when all 3 finish
    private var lastCompletedCount = 0
    private var bonusAlreadyShown = false

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

        binding.achievementsButton.setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
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

        // Show owned creature count inside Collection button
        lifecycleScope.launch {
            repository.getAllPlayerCreatures().collectLatest { creatures ->
                binding.collectionButton.text = "Collection (${creatures.size})"
            }
        }

        // Observe daily challenges
        lifecycleScope.launch {
            repository.getActiveChallenges().collectLatest { challenges ->
                challengeAdapter.submitList(challenges)
                checkForBonusReward(challenges)
            }
        }
    }

    private fun checkDailyLogin() {
        lifecycleScope.launch {
            val streakBonus = repository.checkAndUpdateDailyStreak()
            if (streakBonus > 0) {
                Toast.makeText(
                    this@HomeActivity,
                    "Daily login bonus! +${streakBonus} XP (streak reward)",
                    Toast.LENGTH_LONG
                ).show()
            }
            repository.generateDailyChallenges()
        }
    }

    private fun checkForBonusReward(challenges: List<DailyChallenge>) {
        if (challenges.isEmpty()) return
        val completedCount = challenges.count { it.isCompleted }
        val allDone = completedCount == challenges.size && challenges.size == 3

        // Show the bonus dialog exactly once when all 3 flip to completed
        if (allDone && !bonusAlreadyShown && completedCount > lastCompletedCount) {
            bonusAlreadyShown = true
            lifecycleScope.launch {
                val bonusGranted = repository.checkAllChallengesBonus()
                if (bonusGranted) {
                    AlertDialog.Builder(this@HomeActivity)
                        .setTitle("All Challenges Complete!")
                        .setMessage("Amazing! You finished all 3 daily challenges.\n\nBonus reward: 2 random Goops added to your collection!")
                        .setPositiveButton("Sweet!") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }
        }
        lastCompletedCount = completedCount
    }
}
