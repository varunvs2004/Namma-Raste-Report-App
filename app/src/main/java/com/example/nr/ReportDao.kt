package com.example.nr

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: Report)

    @Query("SELECT * FROM reports WHERE userId = :userId ORDER BY timestamp DESC")
    fun getReportsForUser(userId: String): Flow<List<Report>>

    @Query("SELECT * FROM reports WHERE ticketId = :ticketId LIMIT 1")
    suspend fun getReportByTicket(ticketId: String): Report?

    @Query("SELECT * FROM reports WHERE ticketId = :ticketId AND userId = :userId LIMIT 1")
    suspend fun getReportByTicketForUser(ticketId: String, userId: String): Report?

    @Query("UPDATE reports SET status = :status WHERE ticketId = :ticketId")
    suspend fun updateStatus(ticketId: String, status: String)

    @Query("DELETE FROM reports WHERE ticketId = :ticketId AND userId = :userId")
    suspend fun deleteReport(ticketId: String, userId: String)

    @Query("DELETE FROM reports WHERE userId = :userId")
    suspend fun deleteReportsForUser(userId: String)

    @Query("DELETE FROM reports WHERE userId = :userId AND ticketId NOT IN (:ticketIds)")
    suspend fun deleteReportsForUserExcept(userId: String, ticketIds: List<String>)

    @Query("DELETE FROM reports WHERE userId != :userId")
    suspend fun deleteReportsNotOwnedBy(userId: String)
}
