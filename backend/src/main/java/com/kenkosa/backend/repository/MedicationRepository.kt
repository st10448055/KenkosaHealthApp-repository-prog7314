package com.kenkosa.backend.repository

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.FieldValue
import com.google.firebase.cloud.FirestoreClient
import com.kenkosa.backend.models.*
import java.util.UUID

// Firestore collection used to store medication records.
private const val MEDICATIONS_COLLECTION = "medications"

class MedicationRepository {

    // Gets the Firestore database instance.
    private val db get() = FirestoreClient.getFirestore()

    // Retrieves all medications belonging to a specific user.
    suspend fun getMedicationsForUser(userId: String): List<MedicationResponse> {
        val snapshot = db.collection(MEDICATIONS_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .await()

        // Convert the documents and sort medications by reminder time.
        return snapshot.documents
            .mapNotNull { it.toMedicationResponse() }
            .sortedBy { it.reminderTime }
    }

    // Retrieves a specific medication by its ID.
    suspend fun getMedication(medicationId: String): MedicationResponse? =
        db.collection(MEDICATIONS_COLLECTION).document(medicationId).get().await().toMedicationResponse()

    // Creates and stores a new medication record.
    suspend fun createMedication(userId: String, request: CreateMedicationRequest): MedicationResponse {
        val medicationId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val data = mapOf(
            "medicationId" to medicationId,
            "userId" to userId,
            "category" to request.category.name,
            "name" to request.name.trim(),
            "dosage" to request.dosage.trim(),
            "frequency" to request.frequency.name,
            "remindersEnabled" to request.remindersEnabled,
            "reminderTime" to request.reminderTime,
            "startDate" to request.startDate,
            "takenDates" to emptyList<String>(),
            "createdAt" to now,
            "updatedAt" to now
        )

        db.collection(MEDICATIONS_COLLECTION).document(medicationId).set(data).await()
        return getMedication(medicationId)!!
    }

    // Updates only the medication fields provided in the request.
    suspend fun updateMedication(medicationId: String, request: UpdateMedicationRequest): MedicationResponse? {
        val docRef = db.collection(MEDICATIONS_COLLECTION).document(medicationId)
        val updates = mutableMapOf<String, Any>("updatedAt" to System.currentTimeMillis())
        request.category?.let { updates["category"] = it.name }
        request.name?.let { updates["name"] = it.trim() }
        request.dosage?.let { updates["dosage"] = it.trim() }
        request.frequency?.let { updates["frequency"] = it.name }
        request.remindersEnabled?.let { updates["remindersEnabled"] = it }
        request.reminderTime?.let { updates["reminderTime"] = it }
        request.startDate?.let { updates["startDate"] = it }
        docRef.update(updates).await()
        return getMedication(medicationId)
    }

    // Marks or un-marks a medication dose for a specific date.
    suspend fun setTaken(medicationId: String, date: String, taken: Boolean): MedicationResponse? {
        val docRef = db.collection(MEDICATIONS_COLLECTION).document(medicationId)

        // Add or remove the date from the taken dates list.
        val change = if (taken) FieldValue.arrayUnion(date) else FieldValue.arrayRemove(date)
        docRef.update(
            mapOf(
                "takenDates" to change,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()

        return getMedication(medicationId)
    }

    // Deletes a medication record from Firestore.
    suspend fun deleteMedication(medicationId: String) {
        db.collection(MEDICATIONS_COLLECTION).document(medicationId).delete().await()
    }

    // Converts a Firestore document into a MedicationResponse.
    private fun DocumentSnapshot.toMedicationResponse(): MedicationResponse? {
        if (!exists()) return null
        return MedicationResponse(
            medicationId = getString("medicationId") ?: id,
            userId = getString("userId") ?: return null,
            category = runCatching { MedicationCategory.valueOf(getString("category") ?: "MEDICATION") }
                .getOrDefault(MedicationCategory.MEDICATION),
            name = getString("name") ?: "",
            dosage = getString("dosage") ?: "",
            frequency = runCatching { MedicationFrequency.valueOf(getString("frequency") ?: "DAILY") }
                .getOrDefault(MedicationFrequency.DAILY),
            remindersEnabled = getBoolean("remindersEnabled") ?: false,
            reminderTime = getString("reminderTime") ?: "08:00",
            startDate = getString("startDate") ?: "1970-01-01",
            takenDates = (get("takenDates") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            createdAt = getLong("createdAt") ?: 0L,
            updatedAt = getLong("updatedAt") ?: 0L
        )
    }
}

// ------------------ END OF FILE ------------------ //