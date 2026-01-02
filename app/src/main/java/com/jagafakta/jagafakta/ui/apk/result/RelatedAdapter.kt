package com.jagafakta.jagafakta.ui.apk.result

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jagafakta.jagafakta.R

class RelatedAdapter : ListAdapter<RelatedNews, RelatedAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RelatedNews>() {
            override fun areItemsTheSame(a: RelatedNews, b: RelatedNews) = a.url == b.url
            override fun areContentsTheSame(a: RelatedNews, b: RelatedNews) = a == b
        }
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iv = itemView.findViewById<ImageView>(R.id.ivThumbnail)
        private val tvT = itemView.findViewById<TextView>(R.id.tvTitle)
        private val tvS = itemView.findViewById<TextView>(R.id.tvSource)

        fun bind(item: RelatedNews) {
            tvT.text = item.title

            val host = runCatching { item.url.toUri().host?.removePrefix("www.") ?: "" }.getOrDefault("")
            tvS.text = item.sourceName.ifBlank { host }

            Glide.with(iv)
                .load(item.urlToImage.ifBlank { null })
                .placeholder(R.drawable.image)
                .error(R.drawable.image)
                .centerCrop()
                .into(iv)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_related, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
