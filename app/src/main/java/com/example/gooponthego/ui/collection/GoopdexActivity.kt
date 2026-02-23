package com.example.gooponthego.ui.collection

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gooponthego.GoopApplication
import com.example.gooponthego.databinding.ActivityGoopdexBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GoopdexActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoopdexBinding
    private lateinit var goopdexAdapter: GoopdexAdapter

    private val repository by lazy {
        (application as GoopApplication).repository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGoopdexBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.backButton.setOnClickListener { finish() }

        goopdexAdapter = GoopdexAdapter()
        binding.goopdexRecyclerView.apply {
            layoutManager = GridLayoutManager(this@GoopdexActivity, 3)
            adapter = goopdexAdapter
        }

        observeData()
    }

    private fun observeData() {
        lifecycleScope.launch {
            // isDiscovered is stored persistently on the creature — never resets on evolve/fuse
            repository.getAllCreatures().collectLatest { allCreatures ->
                val entries = allCreatures
                    .sortedBy { it.id }
                    .map { creature ->
                        GoopdexEntry(
                            creature = creature,
                            discovered = creature.isDiscovered
                        )
                    }

                val discovered = entries.count { it.discovered }
                binding.progressText.text = "$discovered / ${allCreatures.size}"
                goopdexAdapter.submitList(entries)
            }
        }
    }
}
