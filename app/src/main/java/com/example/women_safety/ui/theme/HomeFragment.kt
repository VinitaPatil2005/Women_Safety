package com.example.women_safety.ui.theme

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.women_safety.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class HomeFragment : Fragment() {

    private val SMS_PERMISSION_CODE = 123
    private val LOCATION_PERMISSION_CODE = 124
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var phoneNumberEditText: EditText
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

//        phoneNumberEditText = view.findViewById(R.id.phoneNumberEditText)
//        messageEditText = view.findViewById(R.id.messageEditText)
        sendButton = view.findViewById(R.id.sendButton)

        sendButton.setOnClickListener {
//            val phoneNumber = phoneNumberEditText.text.toString()
//            val message = messageEditText.text.toString()

//            if (phoneNumber.isNotEmpty() && message.isNotEmpty()) {
                if (hasSmsPermission() && hasLocationPermission()) {
                    sendSmsWithLocation("+919075259313", "Emergency I am in Danger!!")
                } else {
                    requestPermissions()
                }
//            } else {
//                Toast.makeText(context, "Please enter both phone number and message", Toast.LENGTH_SHORT).show()
//            }
        }
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        requestPermissions(
            arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION),
            SMS_PERMISSION_CODE
        )
    }

    private fun sendSmsWithLocation(phoneNumber: String, message: String) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val locationMessage = "$message\nLocation: https://maps.google.com/?q=${location.latitude},${location.longitude}"
                    sendSms(phoneNumber, locationMessage)
                } else {
                    Toast.makeText(context, "Unable to fetch location", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "SMS sent successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to send SMS: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Permission granted! Try sending the SMS again.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permission denied. Cannot send SMS.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}