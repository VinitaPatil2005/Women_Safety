package com.example.women_safety.ui.theme

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognizerIntent
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.women_safety.R
import com.example.women_safety.adapters.ContactAdapter
import com.example.women_safety.models.Contact
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.util.Locale
import java.util.UUID

class HomeFragment : Fragment() {

    // Permission constants
    private val PERMISSION_REQUEST_CODE = 123
    private val SPEECH_REQUEST_CODE = 101
    private val CONTACTS_PERMISSION_CODE = 102

    // UI components
    private lateinit var sendButton: Button
    private lateinit var voiceBtn: Button
    private lateinit var addContactFab: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var noContactsText: TextView

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Voice recording
    private var audioFilePath: String = ""
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    // Firebase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var userId: String = ""

    // Contact list
    private lateinit var contactAdapter: ContactAdapter
    private val contactsList = mutableListOf<Contact>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Check if user is signed in
        auth.currentUser?.let {
            userId = it.uid
        } ?: run {
            // For demo purposes, create anonymous user
            createAnonymousUser()
        }

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize UI components
        sendButton = view.findViewById(R.id.sendButton)
        voiceBtn = view.findViewById(R.id.voice_btn)
        addContactFab = view.findViewById(R.id.fab_add_contact)
        recyclerView = view.findViewById(R.id.rv_emergency_contacts)
        noContactsText = view.findViewById(R.id.tv_no_contacts)

        // Set up RecyclerView
        setupRecyclerView()

        // Load contacts from Firebase
        loadContacts()

        // Button click listeners
        setupClickListeners()
    }

    private fun createAnonymousUser() {
        auth.signInAnonymously().addOnSuccessListener {
            userId = it.user?.uid ?: ""
            loadContacts()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        contactAdapter = ContactAdapter(contactsList) { contact ->
            deleteContact(contact)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = contactAdapter
        }
    }

    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            if (hasSmsPermission() && hasLocationPermission()) {
                sendSmsToAllContacts("Emergency I am in Danger!!")
            } else {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    PERMISSION_REQUEST_CODE
                )
            }
        }

        voiceBtn.setOnClickListener {
            if (hasMicrophonePermission()) {
                if (isRecording) {
                    stopRecording()
                    Toast.makeText(requireContext(), "Recording stopped - say help or emergency", Toast.LENGTH_SHORT).show()
                    startSpeechToText()
                } else {
                    startRecording()
                    Toast.makeText(requireContext(), "Recording started", Toast.LENGTH_SHORT).show()
                }
                isRecording = !isRecording
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSION_REQUEST_CODE
                )
            }
        }

        addContactFab.setOnClickListener {
            showAddContactDialog()
        }
    }

    private fun loadContacts() {
        if (userId.isEmpty()) return

        firestore.collection("contacts")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Error loading contacts: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                contactsList.clear()
                snapshot?.documents?.forEach { doc ->
                    val contact = doc.toObject(Contact::class.java)
                    contact?.let { contactsList.add(it) }
                }

                // Update UI based on contacts list
                updateContactsUI()
            }
    }

    private fun updateContactsUI() {
        contactAdapter.updateContacts(contactsList)

        if (contactsList.isEmpty()) {
            noContactsText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noContactsText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showAddContactDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_contact)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val nameEditText = dialog.findViewById<EditText>(R.id.et_contact_name)
        val numberEditText = dialog.findViewById<EditText>(R.id.et_contact_number)
        val relationEditText = dialog.findViewById<EditText>(R.id.et_contact_relation)
        val saveButton = dialog.findViewById<Button>(R.id.btn_save)
        val cancelButton = dialog.findViewById<Button>(R.id.btn_cancel)

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val number = numberEditText.text.toString().trim()
            val relation = relationEditText.text.toString().trim()

            if (name.isEmpty() || number.isEmpty()) {
                Toast.makeText(context, "Name and number are required", Toast.LENGTH_SHORT).show()
            } else {
                saveContact(name, number, relation)
                dialog.dismiss()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveContact(name: String, phoneNumber: String, relation: String) {
        if (userId.isEmpty()) {
            Toast.makeText(context, "User authentication error", Toast.LENGTH_SHORT).show()
            return
        }

        val contactId = UUID.randomUUID().toString()
        val contact = Contact(contactId, name, phoneNumber, relation, userId)

        firestore.collection("contacts")
            .document(contactId)
            .set(contact)
            .addOnSuccessListener {
                Toast.makeText(context, "Contact saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save contact: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteContact(contact: Contact) {
        firestore.collection("contacts")
            .document(contact.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to delete contact: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("MissingPermission")
    private fun sendSmsToAllContacts(message: String) {
        if (contactsList.isEmpty()) {
            Toast.makeText(context, "No emergency contacts added", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val locationMessage =
                        "$message\nLocation: https://maps.google.com/?q=${location.latitude},${location.longitude}"

                    // Send SMS to all contacts
                    contactsList.forEach { contact ->
                        sendSms(contact.phoneNumber, locationMessage)
                    }
                } else {
                    Toast.makeText(context, "Unable to fetch location", Toast.LENGTH_SHORT).show()

                    // Send SMS without location
                    contactsList.forEach { contact ->
                        sendSms(contact.phoneNumber, message)
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to fetch location: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager: SmsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(context, "SMS sent to $phoneNumber", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to send SMS: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startRecording() {
        val outputFile = File(requireContext().getExternalFilesDir(null), "recorded_audio.m4a")
        audioFilePath = outputFile.absolutePath

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFilePath)

            try {
                prepare()
                start()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to start recording: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Recording error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "Speech recognition not supported", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.get(0) ?: ""
            Toast.makeText(requireContext(), "Received Text: $spokenText", Toast.LENGTH_LONG).show()

            // Check for emergency keywords
            val emergencyKeywords = listOf("help", "emergency", "danger", "sos")
            if (emergencyKeywords.any { keyword -> spokenText.lowercase().contains(keyword) }) {
                // Send emergency SMS with location
                sendSmsToAllContacts("EMERGENCY: Voice command detected. I need help!")

                // Also share via WhatsApp if audio file exists
                sendWhatsAppMessage(spokenText)
            }
        }
    }

    private fun sendWhatsAppMessage(spokenText: String) {
        val file = File(audioFilePath)
        if (!file.exists()) {
            Toast.makeText(context, "Audio file not found!", Toast.LENGTH_SHORT).show()
            return
        }

        val audioUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val message = "Emergency! I need help!\n$spokenText"

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "audio/*"
        intent.putExtra(Intent.EXTRA_STREAM, audioUri)
        intent.putExtra(Intent.EXTRA_TEXT, message)
        intent.setPackage("com.whatsapp")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allPermissionsGranted = true

            for (i in permissions.indices) {
                val permission = permissions[i]
                val granted = grantResults[i] == PackageManager.PERMISSION_GRANTED
                allPermissionsGranted = allPermissionsGranted && granted

                when (permission) {
                    Manifest.permission.SEND_SMS -> {
                        val msg = if (granted) "SMS permission granted!" else "SMS permission denied."
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }

                    Manifest.permission.ACCESS_FINE_LOCATION -> {
                        val msg = if (granted) "Location permission granted!" else "Location permission denied."
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }

                    Manifest.permission.RECORD_AUDIO -> {
                        val msg = if (granted) "Microphone permission granted!" else "Microphone permission denied."
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // If all permissions granted and this was from the SOS button
            if (allPermissionsGranted && permissions.contains(Manifest.permission.SEND_SMS)) {
                sendSmsToAllContacts("Emergency I am in Danger!!")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up MediaRecorder if it's still active
        if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
            } catch (e: Exception) {
                // Ignore exceptions during cleanup
            }
        }
    }
}