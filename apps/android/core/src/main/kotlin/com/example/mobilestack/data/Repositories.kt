package com.example.mobilestack.data

import com.example.mobilestack.core.network.ApiClient
import com.example.mobilestack.core.query.QueryKey
import com.example.mobilestack.core.system.MinimumVersion

object QueryKeys {
    fun items() = QueryKey("items")

    fun item(id: Int? = null) = QueryKey("items", id?.toString())
}

class ItemsRepository(private val api: ApiClient) {
    suspend fun items(): List<Item> = api.get("items")

    suspend fun item(id: Int): Item = api.get("items/$id")
}

class SystemRepository(private val api: ApiClient) {
    suspend fun minimumVersion(): MinimumVersion = api.get("min-version")
}
