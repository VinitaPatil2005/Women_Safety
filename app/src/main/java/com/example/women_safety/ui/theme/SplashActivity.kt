package com.example.women_safety

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        val splashText = findViewById<TextView>(R.id.splashText)

        // Animate the text to fade in
        splashText.alpha = 0f // Initially invisible
        ObjectAnimator.ofFloat(splashText, "alpha", 0f, 1f).apply {
            duration = 4000 // Duration for the fade-in
            startDelay = 500 // Optional delay to sync with Lottie
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // Proceed to MainActivity after 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3500)
    }
}
