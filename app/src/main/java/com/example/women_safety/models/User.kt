package com.example.women_safety.models

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val profileImageUrl: String = ""
) {
    // Empty constructor for Firebase
    constructor() : this("", "", "", "", "", "")
}