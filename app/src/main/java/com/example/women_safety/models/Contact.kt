package com.example.women_safety.models

data class Contact(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val relation: String = "",
    val userId: String = ""  // To link contacts with specific users
) {
    // Empty constructor for Firebase
    constructor() : this("", "", "", "", "")
}