package com.neonroutine.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.neonroutine.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Query("UPDATE tasks SET isArchived = 1, updatedAt = :now WHERE id = :id")
    suspend fun archiveTask(id: String, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE isArchived = 0")
    suspend fun getAllActiveTasksOnce(): List<Task>

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}
