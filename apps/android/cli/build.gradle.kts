// A host that constructs the core with fixtures instead of platform services. Runs on the JVM, no emulator.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        allWarningsAsErrors = true
    }
}

application {
    mainClass = "com.example.mobilestack.cli.MainKt"
    applicationDefaultJvmArgs = listOf("-Xshare:auto", "-XX:TieredStopAtLevel=1")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":introspection"))
}
