package com.example.permguide.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.permguide.R
import com.example.permguide.model.Attraction

@Suppress("SpellCheckingInspection")
class AttractionAdapter(
    private var attractions: List<Attraction>,
    private val onItemClick: (Attraction) -> Unit
) : RecyclerView.Adapter<AttractionAdapter.AttractionViewHolder>() {
    fun updateList(newList: List<Attraction>) {
        this.attractions = newList
        notifyDataSetChanged() // Говорим списку перерисоваться
    }
    inner class AttractionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.textName)
        val image: ImageView = itemView.findViewById(R.id.imageAttractionSmall)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttractionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attraction, parent, false)
        return AttractionViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttractionViewHolder, position: Int) {
        val attraction = attractions[position]

        // Устанавливаем название
        holder.name.text = attraction.nameAttraction

        // 1. Берем ссылку напрямую из объекта (которую мы получили из API)
        val imageUrl = attraction.photo
        // 2. Загружаем её через Picasso
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .into(holder.image)
        } else {
            // Если ссылки нет, ставим заглушку
            holder.image.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Обработка нажатия
        holder.itemView.setOnClickListener { onItemClick(attraction) }
    }

    override fun getItemCount() = attractions.size
}