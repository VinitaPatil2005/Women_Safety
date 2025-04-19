package com.example.women_safety.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.women_safety.R
import com.example.women_safety.adapter.PoliceStationAdapter
import com.example.women_safety.model.PoliceStation
import com.google.android.gms.location.LocationServices
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class NearbyPoliceFragment : Fragment() {

    private val LOCATION_PERMISSION_CODE = 101
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PoliceStationAdapter
    private val stationList = mutableListOf<PoliceStation>()
    private val apiKey = "fsq3AGLKs2y7ADi0znOEL744pdSW03ozgZvEa+ZdI/RB3yc="

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nearbypolice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.policeRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = PoliceStationAdapter(stationList)
        recyclerView.adapter = adapter

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_CODE
            )
        } else {
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        Log.d("NearbyPoliceFragment", "Location found: ${location.latitude}, ${location.longitude}")
                        fetchNearbyPoliceStations(location.latitude, location.longitude)
                    } else {
                        Log.d("NearbyPoliceFragment", "Location is null")
                        Toast.makeText(requireContext(), "Location is null", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: SecurityException) {
            Log.e("NearbyPoliceFragment", "Location permission is missing: ${e.message}")
            Toast.makeText(requireContext(), "Location permission is missing.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchNearbyPoliceStations(latitude: Double, longitude: Double) {
        val url = "https://api.foursquare.com/v3/places/search?ll=$latitude,$longitude&radius=5000&categories=12072&limit=10"

        Log.d("NearbyPoliceFragment", "Fetching nearby police stations from URL: $url")

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", apiKey)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("NearbyPoliceFragment", "Failed to fetch data: ${e.message}")
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string() ?: return
                Log.d("NearbyPoliceFragment", "Response data: $responseData")

                val json = JSONObject(responseData)
                val results = json.optJSONArray("results") ?: return

                stationList.clear()

                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val fsqId = item.getString("fsq_id")
                    val name = item.getString("name")
                    val locationObj = item.getJSONObject("location")
                    val address = locationObj.optString("formatted_address", "No address")

                    Log.d("NearbyPoliceFragment", "Found station: $name, Address: $address")
                    fetchPlaceDetails(fsqId, name, address)
                }
            }
        })
    }

    private fun fetchPlaceDetails(fsqId: String, name: String, address: String) {
        val detailUrl = "https://api.foursquare.com/v3/places/$fsqId"
        val client = OkHttpClient()

        Log.d("NearbyPoliceFragment", "Fetching place details for fsqId: $fsqId")

        val detailRequest = Request.Builder()
            .url(detailUrl)
            .addHeader("Authorization", apiKey)
            .build()

        client.newCall(detailRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("NearbyPoliceFragment", "Detail fetch failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val detailData = response.body?.string() ?: return
                Log.d("NearbyPoliceFragment", "Detail response data: $detailData")

                val json = JSONObject(detailData)
                val contacts = json.optJSONObject("contacts")
                val phone = contacts?.optString("phone", "Phone not available") ?: "Phone not available"

                Log.d("NearbyPoliceFragment", "Fetched phone number: $phone")

                val station = PoliceStation(
                    name = name,
                    address = address,
                    phone = phone
                )

                requireActivity().runOnUiThread {
                    stationList.add(station)
                    adapter.notifyDataSetChanged()
                }
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getLastLocation()
        } else {
            Log.d("NearbyPoliceFragment", "Location permission denied")
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}