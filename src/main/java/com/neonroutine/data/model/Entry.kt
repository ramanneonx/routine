package com.neonroutine.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "entries", primaryKeys = ["taskId", "date"])
data class Entry(
    val taskId: String,
    val date: String, // ISO format: yyyy-MM-dd
    val valuesJson: String = "{}",
    val completionState: CompletionState = CompletionState.NONE, // for grid UI
    val photoPath: String? = null, // for Photographic Memory
    val updatedAt: Long = System.currentTimeMillis()
)
