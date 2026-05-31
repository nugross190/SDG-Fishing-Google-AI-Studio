package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_history")
data class RunHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val scenarioKey: String,
    val scenarioLabel: String,
    val durationMonths: Int,
    val finalPopPct: Int,
    val finalPopTon: Int,
    val cumulativeProfit: Double,
    val npv: Double,
    val adminRatio: Double,
    val reflection: String,
    val peakFleet: Int,
    val finalFleet: Int,
    val collapsed: Boolean,
    val popSeriesJson: String, // Stringified list of Float or Int representing yearly/monthly population data
    val eventsJson: String,    // Stringified actions (e.g. "Year 1 Month 3: Buy 4")
    val style: String          // Classified run archetype (Greedy, Balanced, etc.)
)
