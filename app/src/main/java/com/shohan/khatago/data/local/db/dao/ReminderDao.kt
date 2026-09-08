package com.shohan.khatago.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shohan.khatago.data.local.db.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE refType = :refType AND refId = :refId LIMIT 1")
    suspend fun find(refType: String, refId: Long): ReminderEntity?

    @Query("SELECT * FROM reminders ORDER BY dueDateEpochDay ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT COUNT(*) FROM reminders")
    suspend fun count(): Int

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE refType = :refType AND refId = :refId")
    suspend fun delete(refType: String, refId: Long)

    @Query("DELETE FROM reminders")
    suspend fun clear()
}
