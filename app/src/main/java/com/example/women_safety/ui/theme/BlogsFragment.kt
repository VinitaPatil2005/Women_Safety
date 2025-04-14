package com.example.women_safety.ui.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.women_safety.R
import com.google.firebase.firestore.FirebaseFirestore

class BlogsFragment : Fragment() {

    private lateinit var blogsRecyclerView: RecyclerView
    private lateinit var db: FirebaseFirestore
    private val blogPosts = mutableListOf<BlogPost>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_blogs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        blogsRecyclerView = view.findViewById(R.id.rv_blogs)
        db = FirebaseFirestore.getInstance()

        // Set up RecyclerView
        blogsRecyclerView.layoutManager = LinearLayoutManager(context)

        // In a real app, blogs would be fetched from Firestore
        // For this demo, we're using static data
        loadSampleBlogPosts()

        blogsRecyclerView.adapter = BlogAdapter(blogPosts) { blogPost ->
            // Handle blog post click - could navigate to detail view
            Toast.makeText(context, "Selected: ${blogPost.title}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSampleBlogPosts() {
        blogPosts.clear()
        blogPosts.add(
            BlogPost(
                "Safety Tips When Using Public Transportation",
                "Learn essential safety strategies when using buses, trains, and rideshares.",
                "April 10, 2025",
                "https://example.com/blog1"
            )
        )
        blogPosts.add(
            BlogPost(
                "Understanding Digital Privacy and Security",
                "Protect your online presence and personal information with these key steps.",
                "April 5, 2025",
                "https://example.com/blog2"
            )
        )
        blogPosts.add(
            BlogPost(
                "Self-Defense Basics Every Woman Should Know",
                "Simple techniques that could help you in dangerous situations.",
                "March 28, 2025",
                "https://example.com/blog3"
            )
        )
        blogPosts.add(
            BlogPost(
                "Creating a Personal Safety Plan",
                "How to prepare for emergencies and ensure your wellbeing.",
                "March 15, 2025",
                "https://example.com/blog4"
            )
        )
        blogPosts.add(
            BlogPost(
                "Safety Apps Review",
                "A comparison of the best personal safety applications available today.",
                "March 10, 2025",
                "https://example.com/blog5"
            )
        )
    }
}


