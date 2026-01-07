package com.mengjizhang.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 账单记录实体
 */
@Entity(tableName = "records")
data class Record(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val categoryId: Int,
    val categoryName: String,
    val categoryEmoji: String,
    val isExpense: Boolean = true,  // true=支出, false=收入
    val note: String = "",
    val imagePath: String? = null,  // 图片路径（可选）
    val date: Long = System.currentTimeMillis(),  // 时间戳
    val createdAt: Long = System.currentTimeMillis(),
    val inputMethod: String = "manual"  // manual, voice, camera
)
