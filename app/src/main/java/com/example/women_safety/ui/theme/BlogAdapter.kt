package com.example.women_safety.ui.theme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.women_safety.R

class BlogAdapter(
    private val blogs: List<BlogPost>,
    private val onItemClick: (BlogPost) -> Unit
) : RecyclerView.Adapter<BlogAdapter.BlogViewHolder>() {

    class BlogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.tv_blog_title)
        val descriptionTextView: TextView = itemView.findViewById(R.id.tv_blog_description)
        val dateTextView: TextView = itemView.findViewById(R.id.tv_blog_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_blog, parent, false)
        return BlogViewHolder(view)
    }

    override fun onBindViewHolder(holder: BlogViewHolder, position: Int) {
        val blog = blogs[position]
        holder.titleTextView.text = blog.title
        holder.descriptionTextView.text = blog.description
        holder.dateTextView.text = blog.date

        holder.itemView.setOnClickListener {
            onItemClick(blog)
        }
    }

    override fun getItemCount() = blogs.size
}