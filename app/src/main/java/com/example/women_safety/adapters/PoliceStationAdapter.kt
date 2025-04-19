package com.example.women_safety.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.women_safety.R
import com.example.women_safety.model.PoliceStation
import org.w3c.dom.Text

class PoliceStationAdapter(private val stationList: List<PoliceStation>) :
    RecyclerView.Adapter<PoliceStationAdapter.StationViewHolder>() {

    class StationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stationName: TextView = itemView.findViewById(R.id.stationName)
        val stationAddress: TextView = itemView.findViewById(R.id.stationAddress)
        val stationPhoneNumber: TextView=itemView.findViewById(R.id.stationPhone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_police_station, parent, false)
        return StationViewHolder(view)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val station = stationList[position]
        holder.stationName.text = station.name
        holder.stationAddress.text = station.address
        holder.stationPhoneNumber.text=station.phone
    }

    override fun getItemCount(): Int = stationList.size
}