package com.example.women_safety.ui.theme

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.women_safety.R
import com.example.women_safety.models.User
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    // UI Components
    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var addressTextView: TextView
    private lateinit var editProfileButton: Button
    private lateinit var logoutButton: Button
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var profileImageLoading: ProgressBar

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // User data
    private var currentUser: User? = null

    // Image picker
    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(requireContext())

        // Initialize image picker
        imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let {
                    selectedImageUri = it
                    updateLocalProfileImage(it)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        profileImageView = view.findViewById(R.id.iv_profile_image)
        nameTextView = view.findViewById(R.id.tv_user_name)
        emailTextView = view.findViewById(R.id.tv_user_email)
        phoneTextView = view.findViewById(R.id.tv_user_phone)
        addressTextView = view.findViewById(R.id.tv_user_address)
        editProfileButton = view.findViewById(R.id.btn_edit_profile)
        logoutButton = view.findViewById(R.id.btn_logout)
        loadingOverlay = view.findViewById(R.id.loading_overlay)
        profileImageLoading = view.findViewById(R.id.profile_image_loading)

        // Set click listeners
        editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }

        logoutButton.setOnClickListener {
            signOut()
            findNavController().navigate(R.id.action_profile_to_loginFragment)
        }

        profileImageView.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Load user data
        loadUserData()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            showToast("User not authenticated")
            return
        }

        showLoading(true)

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)

                if (document != null && document.exists()) {
                    currentUser = document.toObject(User::class.java)
                    updateUI(currentUser)
                } else {
                    val email = auth.currentUser?.email ?: ""
                    val name = auth.currentUser?.displayName ?: "User"

                    currentUser = User(userId, name, email, "", "", "")
                    saveUserToFirestore(currentUser!!)
                    updateUI(currentUser)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showToast("Error loading profile: ${e.message}")
            }
    }

    private fun updateUI(user: User?) {
        user?.let {
            nameTextView.text = it.name.ifEmpty { "Not set" }
            emailTextView.text = it.email.ifEmpty { "Not set" }
            phoneTextView.text = it.phone.ifEmpty { "Not set" }
            addressTextView.text = it.address.ifEmpty { "Not set" }

            if (selectedImageUri != null) {
                profileImageView.setImageURI(selectedImageUri)
            } else {
                profileImageView.setImageResource(R.drawable.profile) // Default profile image
            }
        }
    }

    private fun updateLocalProfileImage(imageUri: Uri) {
        // Directly update the local profile image without uploading to Firebase Storage
        profileImageView.setImageURI(imageUri)
        showToast("Profile image updated locally")
    }

    private fun showEditProfileDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_profile)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val nameEditText = dialog.findViewById<EditText>(R.id.et_edit_name)
        val phoneEditText = dialog.findViewById<EditText>(R.id.et_edit_phone)
        val addressEditText = dialog.findViewById<EditText>(R.id.et_edit_address)
        val saveButton = dialog.findViewById<Button>(R.id.btn_save_profile)
        val cancelButton = dialog.findViewById<Button>(R.id.btn_cancel_edit)

        currentUser?.let {
            nameEditText.setText(it.name)
            phoneEditText.setText(it.phone)
            addressEditText.setText(it.address)
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val address = addressEditText.text.toString().trim()

            updateUserProfile(name, phone, address)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateUserProfile(name: String, phone: String, address: String) {
        val userId = auth.currentUser?.uid ?: return
        showLoading(true)

        val userUpdates = hashMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "address" to address
        )

        firestore.collection("users").document(userId)
            .update(userUpdates)
            .addOnSuccessListener {
                showLoading(false)
                showToast("Profile updated successfully")

                currentUser = currentUser?.copy(
                    name = name,
                    phone = phone,
                    address = address
                )

                updateUI(currentUser)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showToast("Failed to update profile: ${e.message}")
            }
    }

    private fun saveUserToFirestore(user: User) {
        firestore.collection("users").document(user.id)
            .set(user)
            .addOnFailureListener { e ->
                showToast("Failed to save user data: ${e.message}")
            }
    }

    private fun signOut() {
        auth.signOut()
//        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        showToast("Logged out successfully")
    }

    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}