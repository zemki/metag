package de.zemki.metagcompose.util
import kotlinx.datetime.*
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun getCurrentTimeSeconds(): Long = kotlin.time.Clock.System.now().epochSeconds

/**
 * Parses a date string in DD.MM.YYYY or YYYY-MM-DD format to LocalDate
 */
fun parseDate(dateString: String): LocalDate? {
    return try {
        // First, try DD.MM.YYYY format (European format)
        if (dateString.contains(".")) {
            val parts = dateString.split(".")
            if (parts.size == 3) {
                val day = parts[0].toInt()
                val month = parts[1].toInt()
                val year = parts[2].toInt()
                return LocalDate(year, month, day)
            }
        }

        // Fallback: try ISO format (YYYY-MM-DD)
        LocalDate.parse(dateString)
    } catch (e: Exception) {
        AppLogger.e("Failed to parse date: $dateString - ${e.message}", tag = "TimeUtil")
        null
    }
}

/**
 * Gets current date in the system timezone
 */
@OptIn(ExperimentalTime::class)
fun getCurrentDate(): LocalDate {
    return kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}

/**
 * Checks if current date is past the given date
 */
fun isDatePast(dateString: String?): Boolean {
    if (dateString.isNullOrEmpty()) return false
    val targetDate = parseDate(dateString) ?: return false
    val currentDate = getCurrentDate()
    return currentDate > targetDate
}

/**
 * Checks if current date is before the given date
 */
fun isDateBefore(dateString: String?): Boolean {
    if (dateString.isNullOrEmpty()) return false
    val targetDate = parseDate(dateString) ?: return false
    val currentDate = getCurrentDate()
    return currentDate < targetDate
}

/**
 * Formats a date string to a more readable format
 */
fun formatDateForDisplay(dateString: String?): String {
    if (dateString.isNullOrEmpty()) return ""
    val date = parseDate(dateString) ?: return dateString
    return "${date.dayOfMonth}.${date.monthNumber}.${date.year}"
}

/**
 * Parses a timestamp string (either Unix timestamp or datetime string) to Long
 * Handles both old format "2024-03-15 10:30:00.000000" and new format "1710497400"
 */
@OptIn(ExperimentalTime::class)
fun parseTimestamp(timestamp: String): Long? {
    return try {
        // Try parsing as Long first (new format)
        timestamp.toLongOrNull() ?: run {
            // Fallback: parse as datetime string (old format)
            val datePart = timestamp.substringBefore(" ")
            val timePart = timestamp.substringAfter(" ").substringBefore(".")

            val dateComponents = datePart.split("-")
            val timeComponents = timePart.split(":")

            if (dateComponents.size == 3 && timeComponents.size >= 2) {
                val year = dateComponents[0].toInt()
                val month = dateComponents[1].toInt()
                val day = dateComponents[2].toInt()
                val hour = timeComponents[0].toInt()
                val minute = timeComponents[1].toInt()
                val second = timeComponents.getOrNull(2)?.toInt() ?: 0

                val localDateTime = LocalDateTime(year, month, day, hour, minute, second)
                localDateTime.toInstant(TimeZone.currentSystemDefault()).epochSeconds
            } else {
                null
            }
        }
    } catch (e: Exception) {
        AppLogger.e("Failed to parse timestamp: $timestamp - ${e.message}", tag = "TimeUtil")
        null
    }
}

/**
 * Creates a Unix timestamp (seconds since epoch) from LocalDateTime
 * Backend expects Unix timestamp as integer/long
 */
@OptIn(ExperimentalTime::class)
fun createTimestamp(dateTime: LocalDateTime): Long {
    return dateTime.toInstant(TimeZone.currentSystemDefault()).epochSeconds
}

/**
 * Gets current timestamp as Unix timestamp (seconds since epoch)
 */
@OptIn(ExperimentalTime::class)
fun getCurrentTimestamp(): Long {
    return kotlin.time.Clock.System.now().epochSeconds
}

/**
 * Gets timestamp one hour from now as Unix timestamp (seconds since epoch)
 */
@OptIn(ExperimentalTime::class)
fun getTimestampOneHourFromNow(): Long {
    val now = kotlin.time.Clock.System.now()
    val oneHourLater = now.plus(1, DateTimeUnit.HOUR, TimeZone.currentSystemDefault())
    return oneHourLater.epochSeconds
}

/**
 * Formats a Unix timestamp (Long) to DD.MM.YYYY HH:mm format
 */
@OptIn(ExperimentalTime::class)
fun formatTimestampForDisplay(timestamp: Long): String {
    return try {
        val instant = Instant.fromEpochSeconds(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
        val month = localDateTime.monthNumber.toString().padStart(2, '0')
        val year = localDateTime.year
        val hour = localDateTime.hour.toString().padStart(2, '0')
        val minute = localDateTime.minute.toString().padStart(2, '0')

        "$day.$month.$year $hour:$minute"
    } catch (e: Exception) {
        AppLogger.e("Failed to format timestamp: $timestamp - ${e.message}", tag = "TimeUtil")
        timestamp.toString()
    }
}

/**
 * Formats a backend timestamp string to DD.MM.YYYY HH:mm format
 * Handles both old datetime strings and new Unix timestamps
 */
fun formatTimestampForDisplay(timestamp: String): String {
    return try {
        // Try parsing as Long first (new format)
        timestamp.toLongOrNull()?.let {
            formatTimestampForDisplay(it)
        } ?: run {
            // Fallback: parse as datetime string (old format)
            val datePart = timestamp.substringBefore(" ")
            val timePart = timestamp.substringAfter(" ").substringBefore(".")

            val dateComponents = datePart.split("-")
            if (dateComponents.size == 3) {
                val year = dateComponents[0]
                val month = dateComponents[1]
                val day = dateComponents[2]
                val time = timePart.substring(0, 5) // HH:mm

                "$day.$month.$year $time"
            } else {
                timestamp
            }
        }
    } catch (e: Exception) {
        timestamp
    }
}