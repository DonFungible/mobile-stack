package com.example.mobilestack.data

import kotlinx.serialization.Serializable

@Serializable
data class Item(val id: Int, val title: String, val summary: String, val updatedAt: String)
