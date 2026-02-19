package com.example.gooponthego.ui.home

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gooponthego.data.database.entities.ChallengeType
import com.example.gooponthego.data.database.entities.DailyChallenge
import com.example.gooponthego.databinding.ItemChallengeBinding
import com.example.gooponthego.models.GoopType

class ChallengeAdapter : ListAdapter<DailyChallenge, ChallengeAdapter.ChallengeViewHolder>(ChallengeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val binding = ItemChallengeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChallengeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChallengeViewHolder(
        private val binding: ItemChallengeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(challenge: DailyChallenge) {
            binding.challengeTitleText.text = challenge.title
            // Show progress in description
            val progressText = if (challenge.isCompleted) {
                "${challenge.description} (Completed!)"
            } else {
                "${challenge.description} (${challenge.currentProgress}/${challenge.targetCount})"
            }
            binding.challengeDescText.text = progressText
            binding.challengeRewardText.text = "+${challenge.rewardExperience} XP"

            // Set progress
            val progressPercent = if (challenge.targetCount > 0) {
                (challenge.currentProgress * 100) / challenge.targetCount
            } else 0
            binding.challengeProgress.progress = progressPercent

            // Set icon color based on challenge type
            val color = when (challenge.challengeType) {
                ChallengeType.CATCH_ANY -> 0xFF4FC3F7.toInt()
                ChallengeType.CATCH_TYPE -> challenge.targetType?.primaryColor ?: 0xFF4FC3F7.toInt()
                ChallengeType.EVOLVE -> 0xFF81C784.toInt()
                ChallengeType.FUSE -> 0xFFBA68C8.toInt()
                ChallengeType.SCAN_LOCATIONS -> 0xFFFFD54F.toInt()
            }

            (binding.challengeIcon.background as? GradientDrawable)?.setColor(color)
                ?: run {
                    val drawable = GradientDrawable()
                    drawable.shape = GradientDrawable.OVAL
                    drawable.setColor(color)
                    binding.challengeIcon.background = drawable
                }

            // Completed visual state
            if (challenge.isCompleted) {
                binding.challengeProgress.progress = 100
                binding.challengeRewardText.text = "Done!"
                binding.root.alpha = 0.6f
            } else {
                binding.root.alpha = 1f
            }
        }
    }

    class ChallengeDiffCallback : DiffUtil.ItemCallback<DailyChallenge>() {
        override fun areItemsTheSame(oldItem: DailyChallenge, newItem: DailyChallenge): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DailyChallenge, newItem: DailyChallenge): Boolean {
            return oldItem == newItem
        }
    }
}
