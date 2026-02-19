package com.example.gooponthego.ui.achievements

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gooponthego.data.database.entities.Achievement
import com.example.gooponthego.data.database.entities.AchievementCategory
import com.example.gooponthego.databinding.ItemAchievementBinding

class AchievementAdapter : ListAdapter<Achievement, AchievementAdapter.AchievementViewHolder>(AchievementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val binding = ItemAchievementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AchievementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AchievementViewHolder(
        private val binding: ItemAchievementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(achievement: Achievement) {
            binding.achievementTitleText.text = achievement.title
            binding.achievementDescText.text = achievement.description
            binding.rewardText.text = "+${achievement.rewardExperience} XP"

            // Set progress
            val progressPercent = if (achievement.targetProgress > 0) {
                (achievement.currentProgress * 100) / achievement.targetProgress
            } else 0
            binding.achievementProgress.progress = progressPercent.coerceIn(0, 100)
            binding.progressText.text = "${achievement.currentProgress}/${achievement.targetProgress}"

            // Set icon color based on category
            val color = when (achievement.category) {
                AchievementCategory.COLLECTION -> 0xFF4FC3F7.toInt()
                AchievementCategory.EVOLUTION -> 0xFF81C784.toInt()
                AchievementCategory.EXPLORATION -> 0xFFFFD54F.toInt()
                AchievementCategory.DAILY -> 0xFFFF7043.toInt()
                AchievementCategory.SPECIAL -> 0xFFBA68C8.toInt()
            }

            val drawable = binding.achievementIcon.background as? GradientDrawable
                ?: GradientDrawable().also {
                    it.shape = GradientDrawable.OVAL
                    binding.achievementIcon.background = it
                }
            drawable.setColor(if (achievement.isCompleted) color else Color.GRAY)

            // Show completed state
            if (achievement.isCompleted) {
                binding.completedIcon.visibility = View.VISIBLE
                binding.achievementProgress.progress = 100
                binding.root.alpha = 1f
            } else {
                binding.completedIcon.visibility = View.GONE
                binding.root.alpha = 0.85f
            }
        }
    }

    class AchievementDiffCallback : DiffUtil.ItemCallback<Achievement>() {
        override fun areItemsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem == newItem
        }
    }
}
