package com.mengjizhang.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 标签实体
 */
@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: String = "#FF6B9D",  // 标签颜色（十六进制）
    val usageCount: Int = 0,        // 使用次数（用于排序推荐）
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 记录-标签关联表（多对多关系）
 */
@Entity(
    tableName = "record_tags",
    primaryKeys = ["recordId", "tagId"]
)
data class RecordTag(
    val recordId: Long,
    val tagId: Long
)

/**
 * 预定义的标签颜色
 */
val tagColors = listOf(
    "#FF6B9D",  // 粉色
    "#FF8A65",  // 橙色
    "#FFD54F",  // 黄色
    "#81C784",  // 绿色
    "#4FC3F7",  // 蓝色
    "#7986CB",  // 靛蓝
    "#BA68C8",  // 紫色
    "#F06292",  // 玫红
    "#A1887F",  // 棕色
    "#90A4AE"   // 灰色
)
