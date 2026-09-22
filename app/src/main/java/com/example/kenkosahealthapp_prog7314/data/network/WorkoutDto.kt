package com.example.kenkosahealthapp_prog7314.data.network


 // Data Transfer Object (DTO) representing an exercise category.
 // Categories are used to group exercises together, such as
 // strength, cardio, flexibility, or other workout types.

data class ExerciseCategoryDto(

    // Unique identifier for the exercise category.
    val id: Int,

    // Display name of the exercise category.
    val name: String
)


// Data Transfer Object (DTO) representing an individual exercise.
// This information is returned by the backend and can be used
// by the application to display exercises to the user.
data class ExerciseDto(

    // Unique identifier for the exercise.
    val id: Int,

    // Name of the exercise.
    val name: String,

    // Description explaining how the exercise is performed
    // or what the exercise involves.
    val description: String,

    // ID of the category that the exercise belongs to.
    val categoryId: Int,

    // Name of the exercise category.
    // This allows the application to display the category
    // without having to look it up separately.
    val categoryName: String,

    // List of equipment required for the exercise.
    // Defaults to an empty list if no equipment is provided.
    val equipment: List<String> = emptyList(),

    // List of muscles targeted by the exercise.
    // Defaults to an empty list if no muscles are provided.
    val muscles: List<String> = emptyList()
)

// ------------------ END OF FILE ------------------ //