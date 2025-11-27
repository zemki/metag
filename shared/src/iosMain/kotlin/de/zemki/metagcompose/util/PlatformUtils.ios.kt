package de.zemki.metagcompose.util

import platform.Foundation.NSUUID
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun generateUUID(): String = NSUUID().UUIDString()
actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()