package com.kenkosa.backend.repository

import com.kenkosa.backend.models.ExerciseCategoryResponse
import com.kenkosa.backend.models.ExerciseResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

// --- Raw shapes matching wger's actual API responses ---

// Represents the response containing a list of exercise categories.
@Serializable
private data class WgerCategoryListResponse(
    val results: List<WgerCategory> = emptyList()
)

// Represents a single exercise category from wger.
@Serializable
private data class WgerCategory(
    val id: Int,
    val name: String
)

// Represents the response containing a list of exercises.
@Serializable
private data class WgerExerciseListResponse(
    val results: List<WgerExerciseInfo> = emptyList()
)

// Represents a muscle returned by the wger API.
@Serializable
private data class WgerMuscle(
    val id: Int,
    val name: String? = null
)

// Represents equipment linked to an exercise.
@Serializable
private data class WgerEquipmentItem(
    val id: Int,
    val name: String? = null
)


// Represents the main exercise information returned by wger.
@Serializable
private data class WgerExerciseInfo(
    val id: Int,
    val category: WgerCategoryRef,
    val equipment: List<WgerEquipmentItem> = emptyList(),
    val muscles: List<WgerMuscle> = emptyList(),
    val translations: List<WgerTranslation> = emptyList()
)

// Represents the category information linked to an exercise.
@Serializable
private data class WgerCategoryRef(
    val id: Int,
    val name: String
)

// Represents translated exercise names and descriptions.
@Serializable
private data class WgerTranslation(
    val name: String? = null,
    val description: String? = null,
    val language: Int? = null
)

class WgerClient {

    // HTTP client used to communicate with the wger API.
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    // Gets all available exercise categories from wger.
    suspend fun getCategories(): List<ExerciseCategoryResponse> {
        val response: WgerCategoryListResponse = client
            .get("https://wger.de/api/v2/exercisecategory/?limit=50")
            .body()

        // Converts wger categories into the application's response model.
        return response.results.map { ExerciseCategoryResponse(id = it.id, name = it.name) }
    }

    // Gets exercises belonging to a specific category.
    suspend fun getExercisesByCategory(categoryId: Int): List<ExerciseResponse> {
        val response: WgerExerciseListResponse = client
            .get("https://wger.de/api/v2/exerciseinfo/") {
                url {
                    parameters.append("category", categoryId.toString())
                    parameters.append("limit", "50")
                }
            }
            .body()

        //val categories = getCategories().associateBy { it.id }

        return response.results.mapNotNull { exercise ->
            // English translation is language id 2 on wger; fall back to the
            // first available translation if English isn't present for this exercise.
            val translation = exercise.translations.firstOrNull { it.language == 2 }
                ?: exercise.translations.firstOrNull()
                ?: return@mapNotNull null

            // Skips exercises that do not have a usable name.
            val name = translation.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            // Converts the wger exercise into the application's response model.
            ExerciseResponse(
                id = exercise.id,
                name = name,
                description = translation.description.orEmpty(),
                categoryId = exercise.category.id,
                categoryName = exercise.category.name,
                equipment = exercise.equipment.mapNotNull { it.name }, // wger returns equipment as IDs; resolving names is a further lookup,
                muscles = exercise.muscles.mapNotNull { it.name }
            )
        }
    }

    // Closes the HTTP client when it is no longer needed.
    fun close() = client.close()
}

// ------------------ END OF FILE ------------------ //