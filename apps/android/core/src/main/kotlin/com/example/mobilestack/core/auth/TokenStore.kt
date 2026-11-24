package com.example.mobilestack.core.auth

/** Where the session token lives. The app passes a Keystore-backed store; the CLI passes [InMemoryTokenStore]. */
interface TokenStore {
    val token: String?

    fun save(token: String)

    fun clear()
}

class InMemoryTokenStore(@Volatile override var token: String? = null) : TokenStore {
    override fun save(token: String) {
        this.token = token
    }

    override fun clear() {
        token = null
    }
}
