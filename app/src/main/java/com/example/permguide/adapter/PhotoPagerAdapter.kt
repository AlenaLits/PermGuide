package com.example.permguide.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.permguide.R
import com.squareup.picasso.Picasso
import android.widget.ImageView

class PhotoPagerAdapter(private val photos: List<String>) :
    RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false) as ImageView
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        Picasso.get()
            .load(photos[position])
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.imageView)
    }

    override fun getItemCount() = photos.size
}