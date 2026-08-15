package com.nsvault.app.data.database

import androidx.room.TypeConverter
import com.nsvault.app.domain.model.RecordingStatus

class Converters {

    @TypeConverter
    fun fromStatus(status: RecordingStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): RecordingStatus = RecordingStatus.valueOf(value)
}
