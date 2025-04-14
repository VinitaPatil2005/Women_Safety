package com.example.women_safety

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.women_safety.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val navController = findNavController(R.id.nav_host_fragment)
        val bottomNavView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Set up bottom navigation with nav controller
        bottomNavView.setupWithNavController(navController)

        // Hide bottom navigation on login and register screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    bottomNavView.visibility = View.GONE
                }
                else -> {
                    bottomNavView.visibility = View.VISIBLE
                }
            }
        }

        // Handle bottom navigation item selection
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.navigation_chatbot -> {
                    navController.navigate(R.id.chatbotFragment)
                    true
                }
                R.id.navigation_blogs -> {
                    navController.navigate(R.id.blogsFragment)
                    true
                }
                else -> false
            }
        }
    }
}
