package com.mengjizhang.app.data.repository

import com.mengjizhang.app.data.local.TagDao
import com.mengjizhang.app.data.model.RecordTag
import com.mengjizhang.app.data.model.Tag
import kotlinx.coroutines.flow.Flow

class TagRepository(private val dao: TagDao) {

    fun getAllTags(): Flow<List<Tag>> = dao.getAllTags()

    fun searchTags(query: String): Flow<List<Tag>> = dao.searchTags(query)

    fun getTagsForRecord(recordId: Long): Flow<List<Tag>> = dao.getTagsForRecord(recordId)

    suspend fun getTagsForRecordSync(recordId: Long): List<Tag> = dao.getTagsForRecordSync(recordId)

    suspend fun getTagById(id: Long): Tag? = dao.getTagById(id)

    suspend fun addTag(tag: Tag): Long = dao.insertTag(tag)

    suspend fun updateTag(tag: Tag) = dao.updateTag(tag)

    suspend fun deleteTag(tag: Tag) = dao.deleteTag(tag)

    suspend fun addTagToRecord(recordId: Long, tagId: Long) {
        dao.insertRecordTag(RecordTag(recordId, tagId))
        dao.incrementUsageCount(tagId)
    }

    suspend fun removeTagFromRecord(recordId: Long, tagId: Long) {
        dao.deleteRecordTag(RecordTag(recordId, tagId))
    }

    suspend fun setTagsForRecord(recordId: Long, tagIds: List<Long>) {
        // 先删除所有关联
        dao.deleteAllTagsForRecord(recordId)
        // 再添加新的关联
        tagIds.forEach { tagId ->
            dao.insertRecordTag(RecordTag(recordId, tagId))
            dao.incrementUsageCount(tagId)
        }
    }

    fun getRecordIdsForTag(tagId: Long): Flow<List<Long>> = dao.getRecordIdsForTag(tagId)
}
