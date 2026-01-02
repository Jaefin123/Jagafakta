
package com.jagafakta.jagafakta.ui.apk.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jagafakta.jagafakta.R
import com.jagafakta.jagafakta.ui.apk.history.HistoryItem

class HistoryAdapter(
    private val items: List<HistoryItem>,
    private val onView: (HistoryItem) -> Unit,
    private val onDelete: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryVH>() {

    inner class HistoryVH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvDate    = v.findViewById<TextView>(R.id.tvDate)
        private val tvSnippet = v.findViewById<TextView>(R.id.tvSnippet)
        private val tvLabel   = v.findViewById<TextView>(R.id.LabelResult)
        private val btnView   = v.findViewById<Button>(R.id.btnView)
        private val btnDelete = v.findViewById<Button>(R.id.btnDelete)

        fun bind(item: HistoryItem) {
            tvDate.text    = item.date
            tvSnippet.text = item.snippet
            tvLabel.text   = item.label.uppercase()
            tvLabel.setBackgroundResource(
                if (item.label.equals("Hoax", true)) R.drawable.red else R.drawable.green
            )
            btnView.setOnClickListener { onView(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryVH(v)
    }

    override fun onBindViewHolder(holder: HistoryVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
