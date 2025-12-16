package com.example.homesavvy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.ImageView
import androidx.cardview.widget.CardView
class FeatureAdapter(private val features: List<FeatureItem>, private val onClick: (FeatureItem) -> Unit) :
    RecyclerView.Adapter<FeatureAdapter.FeatureViewHolder>() {

    inner class FeatureViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tv_feature_title)
        private val description: TextView = itemView.findViewById(R.id.tv_feature_description)
        private val icon: ImageView = itemView.findViewById(R.id.iv_feature_icon)
        private val cardView: CardView = itemView.findViewById(R.id.card_view)

        fun bind(feature: FeatureItem) {
            title.text = feature.title
            description.text = feature.description
            icon.setImageResource(feature.iconResId)

            itemView.setOnClickListener {
                onClick(feature)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feature_card, parent, false)
        return FeatureViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        holder.bind(features[position])
    }

    override fun getItemCount(): Int = features.size
}