package com.example.core.data.local.converter

import androidx.room.TypeConverter
import com.example.core.model.CallGrade
import com.example.core.model.OfficiatingTier
import com.example.core.model.OfficialPosition

class Converters {
    @TypeConverter
    fun fromTier(tier: OfficiatingTier?): String? = tier?.name

    @TypeConverter
    fun toTier(name: String?): OfficiatingTier? = name?.let {
        try {
            OfficiatingTier.valueOf(it)
        } catch (_: Exception) {
            OfficiatingTier.YOUTH
        }
    }

    @TypeConverter
    fun fromPosition(position: OfficialPosition?): String? = position?.name

    @TypeConverter
    fun toPosition(name: String?): OfficialPosition? = name?.let {
        try {
            OfficialPosition.valueOf(it)
        } catch (_: Exception) {
            OfficialPosition.DOWN_JUDGE
        }
    }

    @TypeConverter
    fun fromCallGrade(grade: CallGrade?): String? = grade?.name

    @TypeConverter
    fun toCallGrade(name: String?): CallGrade? = name?.let {
        try {
            CallGrade.valueOf(it)
        } catch (_: Exception) {
            CallGrade.CORRECT_NON_CALL
        }
    }
}
