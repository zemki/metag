package de.zemki.metagcompose

object GlobalConfig {
    const val USE_DEV_VALUES = false // Set to false for production

    val SERVER_URL: String
        get() = if (USE_DEV_VALUES) DevConfig.DEV_SERVER_URL else "https://www.mesoftware.org/metag"

    object DevConfig {
        const val DEV_EMAIL = ""
        const val DEV_PASSWORD = ""
        const val DEV_SERVER_URL = "https://metagtest.uni-bremen.de/"
    }
}