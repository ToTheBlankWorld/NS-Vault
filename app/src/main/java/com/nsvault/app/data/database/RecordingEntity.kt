package com.nsvault.app.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nsvault.app.domain.model.RecordingStatus

@Entity(
    tableName = "recordings",
    indices = [
        Index(value = ["created_at"]),
        Index(value = ["uuid"], unique = true),
    ],
)
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "uuid")
    val uuid: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "created_at")
    val createdAtEpochMs: Long,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,
    @ColumnInfo(name = "relative_path")
    val relativePath: String,
    @ColumnInfo(name = "container")
    val container: String,
    @ColumnInfo(name = "status")
    val status: RecordingStatus,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "waveform", typeAffinity = ColumnInfo.BLOB)
    val waveform: ByteArray? = null,
    @ColumnInfo(name = "resume_position_ms", defaultValue = "0")
    val resumePositionMs: Long = 0,
)
