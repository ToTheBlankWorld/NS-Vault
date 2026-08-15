package com.nsvault.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nsvault.app.domain.model.RecordingStatus
import kotlinx.coroutines.flow.Flow

data class VaultStatsRow(
    val recordingCount: Int,
    val totalDurationMs: Long?,
    val totalSizeBytes: Long?,
)

@Dao
interface RecordingDao {

    @Insert
    suspend fun insert(recording: RecordingEntity): Long

    @Query("SELECT * FROM recordings WHERE uuid = :uuid LIMIT 1")
    suspend fun findByUuid(uuid: String): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): RecordingEntity?

    @Query("UPDATE recordings SET status = :status WHERE uuid = :uuid")
    suspend fun updateStatus(uuid: String, status: RecordingStatus)

    @Query(
        """
        UPDATE recordings
        SET duration_ms = :durationMs,
            size_bytes = :sizeBytes,
            relative_path = :relativePath,
            container = :container,
            status = :status,
            waveform = :waveform
        WHERE uuid = :uuid
        """,
    )
    suspend fun finalize(
        uuid: String,
        durationMs: Long,
        sizeBytes: Long,
        relativePath: String,
        container: String,
        status: RecordingStatus,
        waveform: ByteArray?,
    )

    @Query("SELECT * FROM recordings WHERE status = 'RECORDING'")
    suspend fun inProgress(): List<RecordingEntity>

    @Query(
        """
        SELECT * FROM recordings
        WHERE relative_path != '' AND relative_path NOT LIKE '%.enc'
        """,
    )
    suspend fun legacyPlaintext(): List<RecordingEntity>

    @Query(
        """
        UPDATE recordings
        SET relative_path = :relativePath, size_bytes = :sizeBytes
        WHERE uuid = :uuid
        """,
    )
    suspend fun updateFile(uuid: String, relativePath: String, sizeBytes: Long)

    @Query(
        """
        SELECT COUNT(*) AS recordingCount,
               SUM(duration_ms) AS totalDurationMs,
               SUM(size_bytes) AS totalSizeBytes
        FROM recordings
        WHERE status IN ('COMPLETED', 'RECOVERED')
        """,
    )
    fun observeStats(): Flow<VaultStatsRow>

    @Query(
        """
        SELECT * FROM recordings
        WHERE status IN ('COMPLETED', 'RECOVERED')
        ORDER BY created_at DESC
        LIMIT 1
        """,
    )
    fun observeLatest(): Flow<RecordingEntity?>

    @Query(
        """
        SELECT * FROM recordings
        WHERE status IN ('COMPLETED', 'RECOVERED')
        ORDER BY created_at DESC
        """,
    )
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("UPDATE recordings SET title = :title WHERE uuid = :uuid")
    suspend fun rename(uuid: String, title: String)

    @Query("UPDATE recordings SET resume_position_ms = :positionMs WHERE uuid = :uuid")
    suspend fun updateResumePosition(uuid: String, positionMs: Long)

    @Query("UPDATE recordings SET is_favorite = :favorite WHERE uuid = :uuid")
    suspend fun setFavorite(uuid: String, favorite: Boolean)

    @Query("DELETE FROM recordings WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)
}
