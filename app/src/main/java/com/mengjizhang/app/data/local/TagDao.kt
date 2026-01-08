package com.mengjizhang.app.data.local

import androidx.room.*
import com.mengjizhang.app.data.model.RecordTag
import com.mengjizhang.app.data.model.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY usageCount DESC, createdAt DESC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: Long): Tag?

    @Query("SELECT * FROM tags WHERE name LIKE '%' || :query || '%' ORDER BY usageCount DESC")
    fun searchTags(query: String): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Update
    suspend fun updateTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("UPDATE tags SET usageCount = usageCount + 1 WHERE id = :tagId")
    suspend fun incrementUsageCount(tagId: Long)

    // 记录-标签关联操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordTag(recordTag: RecordTag)

    @Delete
    suspend fun deleteRecordTag(recordTag: RecordTag)

    @Query("DELETE FROM record_tags WHERE recordId = :recordId")
    suspend fun deleteAllTagsForRecord(recordId: Long)

    @Query("SELECT t.* FROM tags t INNER JOIN record_tags rt ON t.id = rt.tagId WHERE rt.recordId = :recordId")
    fun getTagsForRecord(recordId: Long): Flow<List<Tag>>

    @Query("SELECT t.* FROM tags t INNER JOIN record_tags rt ON t.id = rt.tagId WHERE rt.recordId = :recordId")
    suspend fun getTagsForRecordSync(recordId: Long): List<Tag>

    @Query("SELECT recordId FROM record_tags WHERE tagId = :tagId")
    fun getRecordIdsForTag(tagId: Long): Flow<List<Long>>
}
