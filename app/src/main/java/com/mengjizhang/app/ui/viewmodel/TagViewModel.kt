package com.mengjizhang.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mengjizhang.app.data.local.AppDatabase
import com.mengjizhang.app.data.model.Tag
import com.mengjizhang.app.data.model.tagColors
import com.mengjizhang.app.data.repository.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TagRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TagRepository(database.tagDao())
    }

    // 所有标签
    val allTags: StateFlow<List<Tag>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 当前编辑的标签
    private val _editingTag = MutableStateFlow<Tag?>(null)
    val editingTag: StateFlow<Tag?> = _editingTag.asStateFlow()

    // 是否显示添加/编辑对话框
    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    // 搜索关键词
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 搜索结果
    private val _searchResults = MutableStateFlow<List<Tag>>(emptyList())
    val searchResults: StateFlow<List<Tag>> = _searchResults.asStateFlow()

    fun showAddDialog() {
        _editingTag.value = null
        _showDialog.value = true
    }

    fun showEditDialog(tag: Tag) {
        _editingTag.value = tag
        _showDialog.value = true
    }

    fun hideDialog() {
        _showDialog.value = false
        _editingTag.value = null
    }

    fun addTag(name: String, color: String = tagColors.first()) {
        viewModelScope.launch {
            val tag = Tag(name = name, color = color)
            repository.addTag(tag)
            hideDialog()
        }
    }

    fun updateTag(id: Long, name: String, color: String) {
        viewModelScope.launch {
            val existing = repository.getTagById(id) ?: return@launch
            repository.updateTag(existing.copy(name = name, color = color))
            hideDialog()
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            repository.deleteTag(tag)
        }
    }

    fun searchTags(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            repository.searchTags(query).collect { results ->
                _searchResults.value = results
            }
        }
    }

    // 获取记录的标签
    fun getTagsForRecord(recordId: Long): StateFlow<List<Tag>> {
        return repository.getTagsForRecord(recordId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // 设置记录的标签
    fun setTagsForRecord(recordId: Long, tagIds: List<Long>) {
        viewModelScope.launch {
            repository.setTagsForRecord(recordId, tagIds)
        }
    }

    // 添加标签到记录
    fun addTagToRecord(recordId: Long, tagId: Long) {
        viewModelScope.launch {
            repository.addTagToRecord(recordId, tagId)
        }
    }

    // 从记录移除标签
    fun removeTagFromRecord(recordId: Long, tagId: Long) {
        viewModelScope.launch {
            repository.removeTagFromRecord(recordId, tagId)
        }
    }
}
