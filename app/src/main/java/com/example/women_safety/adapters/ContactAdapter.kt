package com.example.women_safety.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.women_safety.R
import com.example.women_safety.models.Contact

class ContactAdapter(
    private var contacts: List<Contact>,
    private val onDeleteClickListener: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_contact_name)
        val numberTextView: TextView = itemView.findViewById(R.id.tv_contact_number)
        val relationTextView: TextView = itemView.findViewById(R.id.tv_contact_relation)
        val deleteButton: ImageView = itemView.findViewById(R.id.iv_delete_contact)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]

        holder.nameTextView.text = contact.name
        holder.numberTextView.text = contact.phoneNumber
        holder.relationTextView.text = contact.relation

        holder.deleteButton.setOnClickListener {
            onDeleteClickListener(contact)
        }
    }

    override fun getItemCount() = contacts.size

    fun updateContacts(newContacts: List<Contact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }
}