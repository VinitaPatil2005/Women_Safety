package com.example.women_safety.ui.theme

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.women_safety.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*

class HomeFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    // Replace with valid phone numbers
    private val trustedContacts = listOf(
        Contact("Police", "+91xxxxxxxxxx"), // TODO: Add real number
        Contact("Women's Helpline", "+91xxxxxxxxxx"), // TODO: Add real number
        Contact("Family Member", "+91xxxxxxxxxx") // TODO: Add real number
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d("HomeFragment", "All permissions granted")
            Toast.makeText(context, "Permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            Log.w("HomeFragment", "Permissions denied: $permissions")
            Toast.makeText(context, "Please grant SMS and location permissions.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize location request
        initLocationRequest()

        // Request permissions
        requestPermissionsIfNeeded()

        // Send SMS button
        val sendSmsButton = view.findViewById<Button>(R.id.btn_send_sms)
        sendSmsButton.setOnClickListener {
            Log.d("HomeFragment", "Send SMS button clicked")
            sendSMSWithLiveLocation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            context?.unregisterReceiver(smsSentReceiver)
            context?.unregisterReceiver(smsDeliveredReceiver)
        } catch (e: Exception) {
            Log.w("HomeFragment", "Failed to unregister receivers: ${e.message}")
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (toRequest.isNotEmpty()) {
            Log.d("HomeFragment", "Requesting permissions: ${toRequest.joinToString()}")
            requestPermissionLauncher.launch(toRequest)
        } else {
            Log.d("HomeFragment", "All permissions already granted")
        }
    }

    private fun initLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 2000L
            fastestInterval = 500L
            maxWaitTime = 5000L
        }
    }

    private fun hasSmsPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasTelephonyFeature(): Boolean {
        return requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    private fun hasSimCard(): Boolean {
        val telephonyManager = requireContext().getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return telephonyManager?.simState == TelephonyManager.SIM_STATE_READY
    }

    private fun checkGooglePlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(requireContext())
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("HomeFragment", "Google Play Services unavailable: $resultCode")
            Toast.makeText(context, "Google Play Services required for location.", Toast.LENGTH_LONG).show()
            return false
        }
        Log.d("HomeFragment", "Google Play Services available")
        return true
    }

    @SuppressLint("MissingPermission")
    private fun sendSMSWithLiveLocation() {
        // Check SMS permission
        if (!hasSmsPermission()) {
            Log.e("HomeFragment", "SMS permission missing")
            Toast.makeText(context, "SMS permission missing! Please grant it.", Toast.LENGTH_LONG).show()
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.SEND_SMS))
            return
        }

        // Check location permission
        if (!hasLocationPermission()) {
            Log.e("HomeFragment", "Location permission missing")
            Toast.makeText(context, "Location permission missing! Please grant it.", Toast.LENGTH_LONG).show()
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            return
        }

        // Check telephony feature
        if (!hasTelephonyFeature()) {
            Log.e("HomeFragment", "Device does not support SMS")
            Toast.makeText(context, "This device does not support SMS.", Toast.LENGTH_LONG).show()
            return
        }

        // Check SIM state
        if (!hasSimCard()) {
            Log.e("HomeFragment", "No SIM card detected")
            Toast.makeText(context, "No SIM card detected. Please insert a SIM card.", Toast.LENGTH_LONG).show()
            return
        }

        // Check location providers
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        ) {
            Log.w("HomeFragment", "No location providers enabled")
            Toast.makeText(context, "Enable GPS or Wi-Fi for location.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            sendSMSWithoutLocation()
            return
        }

        Log.d("HomeFragment", "GPS enabled: ${locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)}")
        Log.d("HomeFragment", "Network enabled: ${locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)}")

        // Check Google Play Services
        if (!checkGooglePlayServices()) {
            Log.w("HomeFragment", "Falling back to LocationManager")
            tryLastKnownLocationFromManager()
            return
        }

        // Try to get last known location
        Log.d("HomeFragment", "Requesting last location")
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("HomeFragment", "Last location: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}")
                    sendSMSWithLocation(location.latitude, location.longitude)
                } else {
                    Log.w("HomeFragment", "Last location is null")
                    Toast.makeText(context, "Location unavailable. Requesting new location...", Toast.LENGTH_SHORT).show()
                    requestNewLocation()
                }
            }.addOnFailureListener { e ->
                Log.e("HomeFragment", "Last location error: ${e.message}")
                Toast.makeText(context, "Location error: ${e.message}", Toast.LENGTH_LONG).show()
                tryLastKnownLocationFromManager()
            }
        } catch (e: SecurityException) {
            Log.e("HomeFragment", "SecurityException in lastLocation: ${e.message}")
            Toast.makeText(context, "Location error. Trying fallback...", Toast.LENGTH_LONG).show()
            tryLastKnownLocationFromManager()
        }
    }

    private val smsSentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val phoneNumber = intent?.getStringExtra("phoneNumber") ?: "unknown"
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.d("HomeFragment", "SMS sent successfully to $phoneNumber")
                    Toast.makeText(context, "SMS sent to $phoneNumber", Toast.LENGTH_SHORT).show()
                }
                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                    Log.e("HomeFragment", "SMS failed to send to $phoneNumber: Generic failure")
                    Toast.makeText(context, "SMS failed to $phoneNumber: Generic error", Toast.LENGTH_LONG).show()
                }
                SmsManager.RESULT_ERROR_NO_SERVICE -> {
                    Log.e("HomeFragment", "SMS failed to send to $phoneNumber: No service")
                    Toast.makeText(context, "SMS failed to $phoneNumber: No service", Toast.LENGTH_LONG).show()
                }
                SmsManager.RESULT_ERROR_NULL_PDU -> {
                    Log.e("HomeFragment", "SMS failed to send to $phoneNumber: Null PDU")
                    Toast.makeText(context, "SMS failed to $phoneNumber: Null PDU", Toast.LENGTH_LONG).show()
                }
                SmsManager.RESULT_ERROR_RADIO_OFF -> {
                    Log.e("HomeFragment", "SMS failed to send to $phoneNumber: Radio off")
                    Toast.makeText(context, "SMS failed to $phoneNumber: Radio off", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val smsDeliveredReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val phoneNumber = intent?.getStringExtra("phoneNumber") ?: "unknown"
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.d("HomeFragment", "SMS delivered to $phoneNumber")
                    Toast.makeText(context, "SMS delivered to $phoneNumber", Toast.LENGTH_SHORT).show()
                }
                Activity.RESULT_CANCELED -> {
                    Log.e("HomeFragment", "SMS not delivered to $phoneNumber")
                    Toast.makeText(context, "SMS not delivered to $phoneNumber", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sendSMSWithLocation(lat: Double, lon: Double) {
        if (!hasSmsPermission() || !hasTelephonyFeature() || !hasSimCard()) {
            Log.e("HomeFragment", "Cannot send SMS: Missing permission, telephony, or SIM")
            Toast.makeText(context, "Cannot send SMS. Check permissions or SIM.", Toast.LENGTH_LONG).show()
            return
        }

        val smsManager: SmsManager? = try {
            SmsManager.getDefault()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to get SmsManager: ${e.message}")
            null
        }

        if (smsManager == null) {
            Log.e("HomeFragment", "SmsManager is null")
            Toast.makeText(context, "Unable to access SMS service.", Toast.LENGTH_LONG).show()
            return
        }

        // Register receivers
        try {
            ContextCompat.registerReceiver(
                requireContext(),
                smsSentReceiver,
                IntentFilter("SMS_SENT"),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            ContextCompat.registerReceiver(
                requireContext(),
                smsDeliveredReceiver,
                IntentFilter("SMS_DELIVERED"),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            Log.d("HomeFragment", "Registered SMS receivers")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to register SMS receivers: ${e.message}")
            Toast.makeText(context, "Failed to track SMS delivery.", Toast.LENGTH_LONG).show()
            return
        }

        val link = "https://maps.google.com/?q=$lat,$lon"
        val message = "Emergency! I need help. My location: $link"

        var sentAny = false
        for (contact in trustedContacts) {
            if (!isValidPhoneNumber(contact.phoneNumber)) {
                Log.w("HomeFragment", "Invalid phone number: ${contact.phoneNumber}")
                Toast.makeText(context, "Invalid number: ${contact.name}", Toast.LENGTH_SHORT).show()
                continue
            }
            try {
                val sentIntent = Intent("SMS_SENT").apply {
                    putExtra("phoneNumber", contact.phoneNumber)
                }
                val deliveredIntent = Intent("SMS_DELIVERED").apply {
                    putExtra("phoneNumber", contact.phoneNumber)
                }
                val sentPI = PendingIntent.getBroadcast(
                    requireContext(), 0, sentIntent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
                )
                val deliveredPI = PendingIntent.getBroadcast(
                    requireContext(), 0, deliveredIntent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
                )
                smsManager.sendTextMessage(contact.phoneNumber, null, message, sentPI, deliveredPI)
                Log.d("HomeFragment", "SMS queued to ${contact.phoneNumber}")
                sentAny = true
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to send SMS to ${contact.phoneNumber}: ${e.message}")
                Toast.makeText(context, "Failed SMS to ${contact.name}", Toast.LENGTH_LONG).show()
            }
        }
        if (sentAny) {
            Toast.makeText(context, "Emergency SMS sent with location!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to send any SMS!", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendSMSWithoutLocation() {
        if (!hasSmsPermission() || !hasTelephonyFeature() || !hasSimCard()) {
            Log.e("HomeFragment", "Cannot send SMS: Missing permission, telephony, or SIM")
            Toast.makeText(context, "Cannot send SMS. Check permissions or SIM.", Toast.LENGTH_LONG).show()
            return
        }

        val smsManager: SmsManager? = try {
            SmsManager.getDefault()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to get SmsManager: ${e.message}")
            null
        }

        if (smsManager == null) {
            Log.e("HomeFragment", "SmsManager is null")
            Toast.makeText(context, "Unable to access SMS service.", Toast.LENGTH_LONG).show()
            return
        }

        // Register receivers
        try {
            ContextCompat.registerReceiver(
                requireContext(),
                smsSentReceiver,
                IntentFilter("SMS_SENT"),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            ContextCompat.registerReceiver(
                requireContext(),
                smsDeliveredReceiver,
                IntentFilter("SMS_DELIVERED"),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            Log.d("HomeFragment", "Registered SMS receivers")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to register SMS receivers: ${e.message}")
            Toast.makeText(context, "Failed to track SMS delivery.", Toast.LENGTH_LONG).show()
            return
        }

        val message = "Emergency! I need help. Unable to get location."

        var sentAny = false
        for (contact in trustedContacts) {
            if (!isValidPhoneNumber(contact.phoneNumber)) {
                Log.w("HomeFragment", "Invalid phone number: ${contact.phoneNumber}")
                Toast.makeText(context, "Invalid number: ${contact.name}", Toast.LENGTH_SHORT).show()
                continue
            }
            try {
                val sentIntent = Intent("SMS_SENT").apply {
                    putExtra("phoneNumber", contact.phoneNumber)
                }
                val deliveredIntent = Intent("SMS_DELIVERED").apply {
                    putExtra("phoneNumber", contact.phoneNumber)
                }
                val sentPI = PendingIntent.getBroadcast(
                    requireContext(), 0, sentIntent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
                )
                val deliveredPI = PendingIntent.getBroadcast(
                    requireContext(), 0, deliveredIntent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
                )
                smsManager.sendTextMessage(contact.phoneNumber, null, message, sentPI, deliveredPI)
                Log.d("HomeFragment", "SMS queued to ${contact.phoneNumber}")
                sentAny = true
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to send SMS to ${contact.phoneNumber}: ${e.message}")
                Toast.makeText(context, "Failed SMS to ${contact.name}", Toast.LENGTH_LONG).show()
            }
        }
        if (sentAny) {
            Toast.makeText(context, "Emergency SMS sent without location!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to send any SMS!", Toast.LENGTH_LONG).show()
        }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.matches(Regex("^\\+?[1-9]\\d{1,14}\$"))
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocation() {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                Log.d("HomeFragment", "Location result received with ${result.locations.size} locations")
                for (loc in result.locations) {
                    Log.d("HomeFragment", "Location update: ${loc.latitude}, ${loc.longitude}, accuracy: ${loc.accuracy}")
                }
                val loc = result.lastLocation
                if (loc != null) {
                    Log.d("HomeFragment", "New location: ${loc.latitude}, ${loc.longitude}, accuracy: ${loc.accuracy}")
                    sendSMSWithLocation(loc.latitude, loc.longitude)
                    fusedLocationClient.removeLocationUpdates(this)
                } else {
                    Log.w("HomeFragment", "New location is null")
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                Log.d("HomeFragment", "Location availability: ${availability.isLocationAvailable}")
                if (!availability.isLocationAvailable) {
                    Toast.makeText(
                        context,
                        "No location signal. Try moving to an open area or enabling Wi-Fi.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        val networkLocationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            interval = 2000L
            fastestInterval = 500L
            maxWaitTime = 5000L
        }

        Log.d("HomeFragment", "Requesting new location updates (high accuracy)")
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("HomeFragment", "SecurityException in requestLocationUpdates: ${e.message}")
            Toast.makeText(context, "Location service error. Trying fallback...", Toast.LENGTH_LONG).show()
            tryLastKnownLocationFromManager()
            return
        }

        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("HomeFragment", "Switching to network location request")
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                fusedLocationClient.requestLocationUpdates(
                    networkLocationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                Log.e("HomeFragment", "SecurityException in network location request: ${e.message}")
                tryLastKnownLocationFromManager()
            }
        }, 5000)

        Handler(Looper.getMainLooper()).postDelayed({
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.w("HomeFragment", "Location request timed out")
            Toast.makeText(context, "Location timeout. Trying last known location...", Toast.LENGTH_LONG).show()
            tryLastKnownLocationFromManager()
        }, 120000)
    }

    @SuppressLint("MissingPermission")
    private fun tryLastKnownLocationFromManager() {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        var bestLocation: Location? = null
        var bestTime: Long = 0

        for (provider in providers) {
            try {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null && location.time > bestTime) {
                    Log.d("HomeFragment", "Last known $provider location: ${location.latitude}, ${location.longitude}, time: ${location.time}, accuracy: ${location.accuracy}")
                    bestLocation = location
                    bestTime = location.time
                }
            } catch (e: Exception) {
                Log.w("HomeFragment", "Failed to get location from $provider: ${e.message}")
            }
        }

        if (bestLocation != null && (System.currentTimeMillis() - bestTime) < 90 * 60 * 1000) {
            Log.d("HomeFragment", "Using last known location: ${bestLocation.latitude}, ${bestLocation.longitude}")
            sendSMSWithLocation(bestLocation.latitude, bestLocation.longitude)
        } else {
            Log.w("HomeFragment", "No valid last known location from manager")
            Toast.makeText(context, "No recent location. Sending SMS without location.", Toast.LENGTH_LONG).show()
            sendSMSWithoutLocation()
        }
    }
}

// Contact Model
data class Contact(val name: String, val phoneNumber: String)