package com.example.women_safety.ui.theme

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.women_safety.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    private lateinit var welcomeTextView: TextView
    private lateinit var sosButton: Button
    private lateinit var emergencyContactsRecyclerView: RecyclerView
    private lateinit var safetyTipTextView: TextView

    private val safetyTips = arrayOf(
        "Safety Tip: Share your location with trusted contacts when traveling alone.",
        "Safety Tip: Always keep emergency numbers on speed dial.",
        "Safety Tip: Trust your instincts. If something feels wrong, leave the situation.",
        "Safety Tip: Stay in well-lit areas when walking at night.",
        "Safety Tip: Let someone know your plans when meeting someone new."
    )

    private val trustedContacts = listOf(
        Contact("Police", "+917058935505"),
        Contact("Women's Helpline", "+917058935505"),
        Contact("Family Member", "+917058935505")
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(context, "Permissions granted. SOS is ready.", Toast.LENGTH_SHORT).show()
            checkBackgroundLocationPermission()
        } else {
            Toast.makeText(context, "Please grant all permissions for SOS feature.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        welcomeTextView = view.findViewById(R.id.tv_welcome)
        safetyTipTextView = view.findViewById(R.id.tv_safety_tip)
        sosButton = view.findViewById(R.id.btn_sos)
        emergencyContactsRecyclerView = view.findViewById(R.id.rv_emergency_contacts)

        db.collection("users").document(auth.currentUser?.uid ?: "").get()
            .addOnSuccessListener {
                welcomeTextView.text = "Welcome, ${it.getString("name") ?: "User"}"
            }

        safetyTipTextView.text = safetyTips.random()
        safetyTipTextView.setOnClickListener {
            Toast.makeText(context, "💪 You are stronger than you know! 💖", Toast.LENGTH_LONG).show()
        }

        emergencyContactsRecyclerView.layoutManager = LinearLayoutManager(context)
        emergencyContactsRecyclerView.adapter = ContactAdapter(trustedContacts)

        checkGooglePlayServices()
        requestPermissionsIfNeeded()
        initLocationRequest()

        var isSOSActive = false
        var sosHandler: Handler? = null
        var sosRunnable: Runnable? = null

        sosButton.setOnClickListener {
            if (!isSOSActive) {
                isSOSActive = true
                sosButton.text = "SOS ACTIVE"
                sosButton.setBackgroundResource(R.drawable.sos_button_background)
                Toast.makeText(context, "SOS Activated! Sending alerts...", Toast.LENGTH_SHORT).show()
                sendSOSMessage()

                sosHandler = Handler(Looper.getMainLooper())
                sosRunnable = object : Runnable {
                    override fun run() {
                        sendSOSMessage()
                        sosHandler?.postDelayed(this, 60000)
                    }
                }
                sosHandler?.postDelayed(sosRunnable!!, 60000)
            } else {
                isSOSActive = false
                sosButton.text = "SOS"
                sosHandler?.removeCallbacks(sosRunnable!!)
                sosButton.setBackgroundResource(R.drawable.sos_button_background)
                Toast.makeText(context, "SOS Deactivated", Toast.LENGTH_SHORT).show()
            }
        }

        sosButton.setOnLongClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:911")))
            true
        }
    }

    private fun checkGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(requireContext())
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("HomeFragment", "Google Play Services unavailable: $resultCode")
            Toast.makeText(context, "Google Play Services is required for location. Please update it.", Toast.LENGTH_LONG).show()
        } else {
            Log.d("HomeFragment", "Google Play Services available")
        }
    }

    private fun checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("HomeFragment", "Background location permission missing")
                Toast.makeText(
                    context,
                    "Background location permission is recommended for reliable SOS.",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (toRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(toRequest)
        } else {
            val allGranted = permissions.all {
                ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
            }
            if (!allGranted) {
                Toast.makeText(context, "Some permissions are missing. Please enable them in settings.", Toast.LENGTH_LONG).show()
            } else {
                Log.d("HomeFragment", "All permissions granted")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendSOSMessage() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("HomeFragment", "SMS permission missing")
            Toast.makeText(context, "SMS permission missing!", Toast.LENGTH_SHORT).show()
            return
        }

        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        ) {
            Log.w("HomeFragment", "No location providers enabled")
            Toast.makeText(context, "Location services are disabled. Please enable GPS or Wi-Fi.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        Log.d("HomeFragment", "Requesting last location")
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                Log.d("HomeFragment", "Last location: ${location.latitude}, ${location.longitude}")
                sendSMSWithLocation(location.latitude, location.longitude)
            } else {
                Log.w("HomeFragment", "Last location is null")
                Toast.makeText(context, "Last location unavailable. Requesting new location...", Toast.LENGTH_SHORT).show()
                requestNewLocation()
            }
        }.addOnFailureListener {
            Log.e("HomeFragment", "Last location error: ${it.message}")
            Toast.makeText(context, "❌ Location error: ${it.message}", Toast.LENGTH_LONG).show()
            requestNewLocation()
        }
    }

    private fun sendSMSWithLocation(lat: Double, lon: Double) {
        val link = "https://maps.google.com/?q=$lat,$lon"
        val message = "🚨 Emergency! I need help. My location is: $link"
        val smsManager: SmsManager = requireContext().getSystemService(SmsManager::class.java)

        for (contact in trustedContacts) {
            try {
                smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null)
                Log.d("HomeFragment", "SMS sent to ${contact.phoneNumber}")
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to send SMS to ${contact.phoneNumber}: ${e.message}")
                Toast.makeText(context, "Failed to send SMS to ${contact.name}", Toast.LENGTH_SHORT).show()
            }
        }
        Toast.makeText(context, "🚨 SOS message sent!", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocation() {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    Log.d("HomeFragment", "New location: ${loc.latitude}, ${loc.longitude}")
                    sendSMSWithLocation(loc.latitude, loc.longitude)
                    fusedLocationClient.removeLocationUpdates(this)
                } else {
                    Log.w("HomeFragment", "New location is null")
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                Log.d("HomeFragment", "Location availability: ${availability.isLocationAvailable}")
                if (!availability.isLocationAvailable) {
                    Toast.makeText(context, "No location signal. Try moving to an open area or enabling Wi-Fi.", Toast.LENGTH_LONG).show()
                }
            }
        }

        Log.d("HomeFragment", "Requesting new location updates")
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // Timeout after 45 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.w("HomeFragment", "Location request timed out")
            Toast.makeText(context, "❌ Location timeout. Sending SMS without location.", Toast.LENGTH_LONG).show()
            tryLastKnownLocationFromManager()
        }, 45000)
    }

    @SuppressLint("MissingPermission")
    private fun tryLastKnownLocationFromManager() {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        var bestLocation: Location? = null
        var bestTime: Long = 0

        for (provider in providers) {
            try {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null && location.time > bestTime) {
                    bestLocation = location
                    bestTime = location.time
                }
            } catch (e: Exception) {
                Log.w("HomeFragment", "Failed to get location from $provider: ${e.message}")
            }
        }

        if (bestLocation != null) {
            Log.d("HomeFragment", "Found last known location: ${bestLocation.latitude}, ${bestLocation.longitude}")
            sendSMSWithLocation(bestLocation.latitude, bestLocation.longitude)
        } else {
            Log.w("HomeFragment", "No last known location from manager")
            sendSMSWithoutLocation()
        }
    }

    private fun sendSMSWithoutLocation() {
        val message = "🚨 Emergency! I need help. Unable to get my location."
        val smsManager: SmsManager = requireContext().getSystemService(SmsManager::class.java)

        for (contact in trustedContacts) {
            try {
                smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null)
                Log.d("HomeFragment", "SMS sent to ${contact.phoneNumber}")
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to send SMS to ${contact.phoneNumber}: ${e.message}")
                Toast.makeText(context, "Failed to send SMS to ${contact.name}", Toast.LENGTH_SHORT).show()
            }
        }
        Toast.makeText(context, "🚨 SOS message sent without location!", Toast.LENGTH_SHORT).show()
    }

    private fun initLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdateDelayMillis(5000)
            .build()
    }
}

// --- Contact Model and Adapter ---

data class Contact(val name: String, val phoneNumber: String)

class ContactAdapter(private val contacts: List<Contact>) :
    RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_contact_name)
        val phone: TextView = view.findViewById(R.id.tv_contact_phone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.name.text = contact.name
        holder.phone.text = contact.phoneNumber
        holder.itemView.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Selected: ${contact.name}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = contacts.size
}