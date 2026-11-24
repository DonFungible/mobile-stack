package com.example.mobilestack.core.auth

class SessionStore(private val tokens: TokenStore) {
    enum class Status(val value: String) {
        SignedOut("signedOut"),
        SignedIn("signedIn"),
        Expired("expired"),
    }

    private val cleanups = mutableListOf<() -> Unit>()

    internal var onChange: ((Status) -> Unit)? = null

    var status: Status = if (tokens.token == null) Status.SignedOut else Status.SignedIn
        private set(value) {
            val changed = field != value
            field = value
            if (changed) onChange?.invoke(value)
        }

    fun signIn(token: String) {
        tokens.save(token)
        status = Status.SignedIn
    }

    /** Called by the API layer on a 401; the app root reacts by signing out. */
    fun markExpired() {
        if (status == Status.SignedIn) status = Status.Expired
    }

    fun signOut() {
        tokens.clear()
        cleanups.forEach { it() }
        status = Status.SignedOut
    }

    /** Every cache or store holding user data registers its reset here, so sign-out cannot miss one. */
    fun registerSignOutCleanup(cleanup: () -> Unit) {
        cleanups += cleanup
    }
}
