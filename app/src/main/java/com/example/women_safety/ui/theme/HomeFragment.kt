package com.example.women_safety.ui.theme

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.women_safety.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File
import java.util.Locale

class HomeFragment : Fragment() {

    private val PERMISSION_REQUEST_CODE = 123
    private val SPEECH_REQUEST_CODE = 101
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var sendButton: Button
    private lateinit var voicbtn: Button

    private var audioFilePath: String = ""
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        sendButton = view.findViewById(R.id.sendButton)
        voicbtn = view.findViewById(R.id.voice_btn)

        sendButton.setOnClickListener {
            if (hasSmsPermission() && hasLocationPermission()) {
                sendSmsWithLocation("+919112270961", "Emergency I am in Danger!!")
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

        voicbtn.setOnClickListener {
            if (hasMicrophonePermission()) {
                if (isRecording) {
                    stopRecording()
                    Toast.makeText(requireContext(), "Recording stopped  a popup will occur just say help or hii", Toast.LENGTH_SHORT).show()
                    startSpeechToText()
                } else {
                    startRecording()
                    Toast.makeText(requireContext(), "Recording has started ", Toast.LENGTH_SHORT).show()
                }
                isRecording = !isRecording
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSION_REQUEST_CODE
                )
            }
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

    @SuppressLint("MissingPermission")
    private fun sendSmsWithLocation(phoneNumber: String, message: String) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val locationMessage =
                        "$message\nLocation: https://maps.google.com/?q=${location.latitude},${location.longitude}"
                    sendSms(phoneNumber, locationMessage)
                } else {
                    Toast.makeText(context, "Unable to fetch location", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to fetch location: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager: SmsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(context, "SMS sent successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to send SMS: ${e.message}", Toast.LENGTH_LONG).show()
        }
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

            sendWhatsAppMessage(spokenText)
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
            for (i in permissions.indices) {
                val permission = permissions[i]
                val granted = grantResults[i] == PackageManager.PERMISSION_GRANTED

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
        }
    }
}