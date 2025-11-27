package de.zemki.metagcompose.util

import java.util.UUID

actual fun generateUUID(): String = UUID.randomUUID().toString()
actual fun currentTimeMillis(): Long = System.currentTimeMillis()