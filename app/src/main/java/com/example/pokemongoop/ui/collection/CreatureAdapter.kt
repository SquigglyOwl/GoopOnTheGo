package com.example.pokemongoop.ui.collection

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pokemongoop.data.database.dao.PlayerCreatureWithDetails
import com.example.pokemongoop.databinding.ItemCreatureBinding

class CreatureAdapter(
    private val onItemClick: (PlayerCreatureWithDetails) -> Unit
) : ListAdapter<PlayerCreatureWithDetails, CreatureAdapter.CreatureViewHolder>(CreatureDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreatureViewHolder {
        val binding = ItemCreatureBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CreatureViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: CreatureViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CreatureViewHolder(
        private val binding: ItemCreatureBinding,
        private val onItemClick: (PlayerCreatureWithDetails) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(details: PlayerCreatureWithDetails) {
            val creature = details.creature
            val playerCreature = details.playerCreature

            // Set name (nickname if exists, otherwise creature name)
            binding.creatureNameText.text = playerCreature.nickname ?: creature.name

            // Set type
            binding.creatureTypeText.text = creature.type.displayName
            binding.creatureTypeText.setTextColor(creature.type.primaryColor)

            // Set creature visual color
            val drawable = binding.creatureVisual.background as? GradientDrawable
                ?: GradientDrawable().also {
                    it.shape = GradientDrawable.OVAL
                    binding.creatureVisual.background = it
                }
            drawable.setColor(creature.type.primaryColor)
            drawable.setStroke(4, creature.type.secondaryColor)

            // Set experience progress
            val expProgress = if (creature.experienceToEvolve > 0) {
                (playerCreature.experience * 100) / creature.experienceToEvolve
            } else {
                100 // Max level
            }
            binding.experienceProgress.progress = expProgress.coerceIn(0, 100)

            // Show favorite icon
            binding.favoriteIcon.visibility = if (playerCreature.isFavorite) View.VISIBLE else View.GONE

            // Click listener
            binding.root.setOnClickListener {
                onItemClick(details)
            }
        }
    }

    class CreatureDiffCallback : DiffUtil.ItemCallback<PlayerCreatureWithDetails>() {
        override fun areItemsTheSame(
            oldItem: PlayerCreatureWithDetails,
            newItem: PlayerCreatureWithDetails
        ): Boolean {
            return oldItem.playerCreature.id == newItem.playerCreature.id
        }

        override fun areContentsTheSame(
            oldItem: PlayerCreatureWithDetails,
            newItem: PlayerCreatureWithDetails
        ): Boolean {
            return oldItem == newItem
        }
    }
}
