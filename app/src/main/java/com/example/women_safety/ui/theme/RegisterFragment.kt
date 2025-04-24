package com.example.women_safety.ui.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.women_safety.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var nameEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var registerButton: Button
    private lateinit var loginPrompt: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        nameEditText = view.findViewById(R.id.et_name)
        phoneEditText = view.findViewById(R.id.et_phone)
        emailEditText = view.findViewById(R.id.et_register_email)
        passwordEditText = view.findViewById(R.id.et_register_password)
        registerButton = view.findViewById(R.id.btn_register)
        loginPrompt = view.findViewById(R.id.tv_login_prompt)

        // Set click listeners
        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Register with Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Store additional user info in Firestore
                        val user = hashMapOf(
                            "name" to name,
                            "phone" to phone,
                            "email" to email
                        )

                        db.collection("users")
                            .document(auth.currentUser!!.uid)
                            .set(user)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                auth.signOut()
                                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        loginPrompt.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }
}