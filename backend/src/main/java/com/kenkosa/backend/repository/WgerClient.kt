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

@Serializable
private data class WgerCategoryListResponse(
    val results: List<WgerCategory> = emptyList()
)

@Serializable
private data class WgerCategory(
    val id: Int,
    val name: String
)

@Serializable
private data class WgerExerciseListResponse(
    val results: List<WgerExerciseInfo> = emptyList()
)

@Serializable
private data class WgerMuscle(
    val id: Int,
    val name: String? = null
)

@Serializable
private data class WgerEquipmentItem(
    val id: Int,
    val name: String? = null
)


@Serializable
private data class WgerExerciseInfo(
    val id: Int,
    val category: WgerCategoryRef,
    val equipment: List<WgerEquipmentItem> = emptyList(),
    val muscles: List<WgerMuscle> = emptyList(),
    val translations: List<WgerTranslation> = emptyList()
)


@Serializable
private data class WgerCategoryRef(
    val id: Int,
    val name: String
)

@Serializable
private data class WgerTranslation(
    val name: String? = null,
    val description: String? = null,
    val language: Int? = null
)

class WgerClient {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getCategories(): List<ExerciseCategoryResponse> {
        val response: WgerCategoryListResponse = client
            .get("https://wger.de/api/v2/exercisecategory/?limit=50")
            .body()

        return response.results.map { ExerciseCategoryResponse(id = it.id, name = it.name) }
    }

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

            val name = translation.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

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

    fun close() = client.close()
}