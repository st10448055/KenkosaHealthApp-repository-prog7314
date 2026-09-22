package com.example.kenkosahealthapp_prog7314.data.repository


// Sealed class representing the possible outcomes of an API operation.
// Using a single result type allows repositories and UI components
// to handle successful requests, validation problems, network errors
// and server errors in a consistent way.
// 'out T' makes ApiResult covariant, allowing a result containing
// a subtype to be used where a result containing its parent type
// is expected.

sealed class ApiResult<out T> {


     // Represents a successful API operation.
     // @param data The data returned by the API.

    data class Success<T>(
        val data: T
    ) : ApiResult<T>()


     // Represents a validation error.
     //
     // This is used when input fails validation before the request
     // needs to be sent to the backend.
     // @param message Description of the validation problem.
    data class ValidationError(
        val message: String
    ) : ApiResult<Nothing>()


     // Represents a network-related error.
     // This can occur when the application cannot reach the backend,
     // for example because of a connection or internet problem.
     // @param message User-readable description of the network error.
    data class NetworkError(
        val message: String
    ) : ApiResult<Nothing>()


     // Represents an error returned by the backend/server.
     // @param code HTTP status code returned by the server.
     // @param message Description of the server error.

    data class ServerError(
        val code: Int,
        val message: String
    ) : ApiResult<Nothing>()
}

// ------------------ END OF FILE ------------------ //