package com.example.women_safety.ui.theme

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
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
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
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

    private fun requestPermissionsIfNeeded() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS
        )

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (toRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(toRequest)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendSOSMessage() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "SMS permission missing!", Toast.LENGTH_SHORT).show()
            return
        }

        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(context, "Enable GPS to send location!", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                sendSMSWithLocation(location.latitude, location.longitude)
            } else {
                requestNewLocation()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "❌ Location error: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendSMSWithLocation(lat: Double, lon: Double) {
        val link = "https://maps.google.com/?q=$lat,$lon"
        val message = "🚨 Emergency! I need help. My location is: $link"
        val smsManager: SmsManager = requireContext().getSystemService(SmsManager::class.java)

        for (contact in trustedContacts) {
            smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null)
        }
        Toast.makeText(context, "🚨 SOS message sent!", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocation() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation
                    if (loc != null) {
                        sendSMSWithLocation(loc.latitude, loc.longitude)
                        fusedLocationClient.removeLocationUpdates(this)
                    } else {
                        Toast.makeText(context, "❌ Still unable to get location.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            Looper.getMainLooper()
        )
    }

    private fun initLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(2000)
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
