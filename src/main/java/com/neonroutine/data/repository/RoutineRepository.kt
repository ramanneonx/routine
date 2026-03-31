package com.neonroutine.data.repository

import com.neonroutine.data.db.EntryDao
import com.neonroutine.data.db.TaskDao
import com.neonroutine.data.model.Entry
import com.neonroutine.data.model.Task
import kotlinx.coroutines.flow.Flow

class RoutineRepository(
    private val taskDao: TaskDao,
    private val entryDao: EntryDao
) {
    // -- Tasks --
    fun getAllActiveTasks(): Flow<List<Task>> = taskDao.getAllActiveTasks()
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()
    suspend fun getTaskById(id: String): Task? = taskDao.getTaskById(id)
    suspend fun insertTask(task: Task) = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun archiveTask(id: String) = taskDao.archiveTask(id)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    suspend fun getAllActiveTasksOnce(): List<Task> = taskDao.getAllActiveTasksOnce()

    // -- Entries --
    suspend fun getEntry(taskId: String, date: String): Entry? = entryDao.getEntry(taskId, date)
    fun getEntriesForDate(date: String): Flow<List<Entry>> = entryDao.getEntriesForDate(date)
    fun getEntriesForTask(taskId: String): Flow<List<Entry>> = entryDao.getEntriesForTask(taskId)
    fun getEntriesInRange(startDate: String, endDate: String): Flow<List<Entry>> =
        entryDao.getEntriesInRange(startDate, endDate)
    suspend fun getEntriesInRangeOnce(startDate: String, endDate: String): List<Entry> =
        entryDao.getEntriesInRangeOnce(startDate, endDate)
    suspend fun insertEntry(entry: Entry) = entryDao.insertEntry(entry)
    suspend fun updateEntry(entry: Entry) = entryDao.updateEntry(entry)
    suspend fun deleteEntry(entry: Entry) = entryDao.deleteEntry(entry)
    suspend fun getAllEntries(): List<Entry> = entryDao.getAllEntries()
    suspend fun deleteEntriesForTask(taskId: String) = entryDao.deleteEntriesForTask(taskId)

    // -- App Data --
    suspend fun clearAllData() {
        taskDao.deleteAllTasks()
        entryDao.deleteAllEntries()
    }
}
