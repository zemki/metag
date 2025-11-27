package de.zemki.metagcompose

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform