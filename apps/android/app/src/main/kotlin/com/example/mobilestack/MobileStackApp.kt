package com.example.mobilestack

import android.app.Application

class MobileStackApp : Application() {
    val host: AppHost by lazy { AppHost(this) }
}
