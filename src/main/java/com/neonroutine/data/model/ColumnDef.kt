package com.neonroutine.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ColumnType {
    TICK, SCORE, GRADE, NOTE
}

@Serializable
data class ColumnDef(
    val id: String,
    val label: String,
    val type: ColumnType,
    val maxScore: Int? = null
)
