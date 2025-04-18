package com.example.women_safety.ui.theme

import android.content.Intent
import android.net.Uri
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
        return inflater.inflate(R.layout.fragment_blogs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        blogsRecyclerView = view.findViewById(R.id.rv_blogs)
        db = FirebaseFirestore.getInstance()

        // Set up RecyclerView
        blogsRecyclerView.layoutManager = LinearLayoutManager(context)

        // Load sample blog posts
        loadSampleBlogPosts()

        // Set adapter with click listener to open URL
        blogsRecyclerView.adapter = BlogAdapter(blogPosts) { blogPost ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(blogPost.url))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening URL: ${blogPost.url}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSampleBlogPosts() {
        blogPosts.clear()
        blogPosts.add(
            BlogPost(
                "Safety Tips When Using Public Transportation",
                "Learn essential safety strategies when using buses, trains, and rideshares.",
                "April 10, 2025",
                "https://www.womenshealth.gov/blog/public-transportation-safety-tips"
            )
        )
        blogPosts.add(
            BlogPost(
                "Understanding Digital Privacy and Security",
                "Protect your online presence and personal information with these key steps.",
                "April 5, 2025",
                "https://www.consumer.ftc.gov/articles/how-protect-your-privacy-online"
            )
        )
        blogPosts.add(
            BlogPost(
                "Self-Defense Basics Every Woman Should Know",
                "Simple techniques that could help you in dangerous situations.",
                "March 28, 2025",
                "https://www.self.com/story/self-defense-moves-every-woman-should-know"
            )
        )
        blogPosts.add(
            BlogPost(
                "Creating a Personal Safety Plan",
                "How to prepare for emergencies and ensure your wellbeing.",
                "March 15, 2025",
                "https://www.redcross.org/get-help/how-to-prepare-for-emergencies/make-a-plan.html"
            )
        )
        blogPosts.add(
            BlogPost(
                "Safety Apps Review",
                "A comparison of the best personal safety applications available today.",
                "March 10, 2025",
                "https://www.safewise.com/best-personal-safety-apps/"
            )
        )
    }
}