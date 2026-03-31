package com.neonroutine.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.neonroutine.data.model.Entry
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE taskId = :taskId AND date = :date LIMIT 1")
    suspend fun getEntry(taskId: String, date: String): Entry?

    @Query("SELECT * FROM entries WHERE date = :date")
    fun getEntriesForDate(date: String): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE taskId = :taskId")
    fun getEntriesForTask(taskId: String): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE date BETWEEN :startDate AND :endDate")
    fun getEntriesInRange(startDate: String, endDate: String): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getEntriesInRangeOnce(startDate: String, endDate: String): List<Entry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: Entry)

    @Update
    suspend fun updateEntry(entry: Entry)

    @Delete
    suspend fun deleteEntry(entry: Entry)

    @Query("SELECT * FROM entries")
    suspend fun getAllEntries(): List<Entry>

    @Query("DELETE FROM entries WHERE taskId = :taskId")
    suspend fun deleteEntriesForTask(taskId: String)

    @Query("DELETE FROM entries")
    suspend fun deleteAllEntries()
}
