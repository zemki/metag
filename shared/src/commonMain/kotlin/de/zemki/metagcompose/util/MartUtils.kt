package de.zemki.metagcompose.util

import de.zemki.metagcompose.data.model.ProjectData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import de.zemki.metagcompose.util.AppLogger

/**
 * Utility object for detecting and handling MART projects
 */
object MartUtils {
    
    /**
     * Checks if a project is a MART project based on its inputs structure
     * According to backend documentation, MART projects have inputs with type "mart"
     */
    fun isMartProject(projectData: ProjectData): Boolean {
        return try {
            AppLogger.d("Checking project inputs: ${projectData.inputs}", tag = "MartUtils")
            val json = Json { ignoreUnknownKeys = true }
            val inputsArray = json.parseToJsonElement(projectData.inputs).jsonArray
            
            // Check if first element has type "mart"
            val firstInput = inputsArray.firstOrNull()?.jsonObject
            val inputType = firstInput?.get("type")?.jsonPrimitive?.content
            AppLogger.d("First input type: $inputType", tag = "MartUtils")
            
            val isMart = inputType == "mart"
            AppLogger.d("Is MART project: $isMart", tag = "MartUtils")
            return isMart
        } catch (e: Exception) {
            AppLogger.d("Error checking MART project status: ${e.message}", tag = "MartUtils")
            false
        }
    }
}