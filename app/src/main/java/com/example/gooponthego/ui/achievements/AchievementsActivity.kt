package com.example.gooponthego.ui.achievements

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gooponthego.GoopApplication
import com.example.gooponthego.databinding.ActivityAchievementsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AchievementsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAchievementsBinding
    private lateinit var achievementAdapter: AchievementAdapter

    private val repository by lazy {
        (application as GoopApplication).repository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAchievementsBinding.inflate(layoutInflater)
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

        achievementAdapter = AchievementAdapter()
        binding.achievementsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AchievementsActivity)
            adapter = achievementAdapter
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repository.getAllAchievements().collectLatest { achievements ->
                achievementAdapter.submitList(achievements)

                val completed = achievements.count { it.isCompleted }
                val total = achievements.size
                binding.completedCountText.text = "$completed/$total"
            }
        }
    }
}
