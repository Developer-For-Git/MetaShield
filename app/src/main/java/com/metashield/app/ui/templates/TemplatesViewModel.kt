package com.metashield.app.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metashield.app.data.db.entity.TemplateEntity
import com.metashield.app.data.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val templateRepository: TemplateRepository
) : ViewModel() {

    val templates: StateFlow<List<TemplateEntity>> = templateRepository
        .getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createTemplate(name: String, description: String) {
        viewModelScope.launch {
            templateRepository.saveTemplate(
                TemplateEntity(name = name, description = description, fieldsJson = "[]")
            )
        }
    }

    fun deleteTemplate(entity: TemplateEntity) {
        viewModelScope.launch {
            templateRepository.deleteTemplate(entity)
        }
    }
}
