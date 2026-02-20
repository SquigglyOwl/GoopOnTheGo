package com.example.gooponthego.ui.collection

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gooponthego.data.database.entities.Creature
import com.example.gooponthego.databinding.ItemGoopdexBinding

data class GoopdexEntry(
    val creature: Creature,
    val discovered: Boolean
)

class GoopdexAdapter : ListAdapter<GoopdexEntry, GoopdexAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemGoopdexBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: GoopdexEntry) {
            val creature = entry.creature
            val context = binding.root.context

            binding.goopdexNumber.text = "#%03d".format(creature.id)

            if (entry.discovered) {
                // Show full colour image
                binding.goopdexName.text = creature.name
                binding.goopdexType.text = creature.type.displayName
                binding.goopdexType.setTextColor(creature.type.primaryColor)

                val resId = if (creature.imageResName != null) {
                    context.resources.getIdentifier(creature.imageResName, "drawable", context.packageName)
                } else 0

                if (resId != 0) {
                    binding.goopdexVisual.setImageDrawable(ContextCompat.getDrawable(context, resId))
                    binding.goopdexVisual.colorFilter = null
                    binding.goopdexVisual.background = null
                } else {
                    binding.goopdexVisual.setImageDrawable(null)
                    binding.goopdexVisual.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(creature.type.primaryColor)
                    }
                    binding.goopdexVisual.colorFilter = null
                }

                binding.root.alpha = 1f
            } else {
                // Show silhouette — black filled image or dark blob
                binding.goopdexName.text = "???"
                binding.goopdexName.setTextColor(Color.parseColor("#AAAAAA"))
                binding.goopdexType.text = "???"
                binding.goopdexType.setTextColor(Color.parseColor("#AAAAAA"))

                val resId = if (creature.imageResName != null) {
                    context.resources.getIdentifier(creature.imageResName, "drawable", context.packageName)
                } else 0

                if (resId != 0) {
                    binding.goopdexVisual.setImageDrawable(ContextCompat.getDrawable(context, resId))
                    // Apply black silhouette filter
                    val colorMatrix = ColorMatrix().apply {
                        setSaturation(0f)
                        val scale = floatArrayOf(
                            0f, 0f, 0f, 0f, 0f,
                            0f, 0f, 0f, 0f, 0f,
                            0f, 0f, 0f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                        set(scale)
                    }
                    binding.goopdexVisual.colorFilter = ColorMatrixColorFilter(colorMatrix)
                    binding.goopdexVisual.background = null
                } else {
                    binding.goopdexVisual.setImageDrawable(null)
                    binding.goopdexVisual.colorFilter = null
                    binding.goopdexVisual.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#2a2a2a"))
                    }
                }

                binding.root.alpha = 0.6f
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGoopdexBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<GoopdexEntry>() {
        override fun areItemsTheSame(oldItem: GoopdexEntry, newItem: GoopdexEntry) =
            oldItem.creature.id == newItem.creature.id
        override fun areContentsTheSame(oldItem: GoopdexEntry, newItem: GoopdexEntry) =
            oldItem == newItem
    }
}
