package com.directline.feature.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.directline.core.template.TemplateProcessResult
import com.directline.core.template.TemplateProcessor
import com.directline.domain.model.MessageTemplate
import com.directline.domain.model.TemplateCategory
import com.directline.domain.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplatesUiState(
  val templates: List<MessageTemplate> = emptyList(),
  val categories: List<TemplateCategory> = emptyList(),
  val searchQuery: String = "",
  val selectedCategoryId: String? = null,
  val feedbackMessage: String? = null,
)

private data class TemplateFilterState(
  val searchQuery: String = "",
  val selectedCategoryId: String? = null,
  val feedbackMessage: String? = null,
)

@HiltViewModel
class TemplatesViewModel @Inject constructor(
  private val templateRepository: TemplateRepository,
  private val templateProcessor: TemplateProcessor,
) : ViewModel() {

  private val filterState = MutableStateFlow(TemplateFilterState())

  val uiState: StateFlow<TemplatesUiState> = combine(
    templateRepository.observeTemplates(),
    templateRepository.observeCategories(),
    filterState,
  ) { templates, categories, filter ->
    val query = filter.searchQuery.trim()
    val filtered = templates.filter { template ->
      val matchesCategory = filter.selectedCategoryId == null ||
        template.categoryId == filter.selectedCategoryId
      val matchesQuery = query.isEmpty() ||
        template.title.contains(query, ignoreCase = true) ||
        template.content.contains(query, ignoreCase = true) ||
        (template.categoryName?.contains(query, ignoreCase = true) == true)
      matchesCategory && matchesQuery
    }
    TemplatesUiState(
      templates = filtered,
      categories = categories,
      searchQuery = filter.searchQuery,
      selectedCategoryId = filter.selectedCategoryId,
      feedbackMessage = filter.feedbackMessage,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = TemplatesUiState(),
  )

  fun onSearchQueryChanged(query: String) {
    filterState.update { it.copy(searchQuery = query) }
  }

  fun onSelectCategory(categoryId: String?) {
    filterState.update {
      it.copy(selectedCategoryId = if (it.selectedCategoryId == categoryId) null else categoryId)
    }
  }

  fun dismissFeedback() {
    filterState.update { it.copy(feedbackMessage = null) }
  }

  fun saveTemplate(
    id: String?,
    title: String,
    content: String,
    categoryName: String?,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) {
    if (title.trim().isEmpty()) {
      onError("Template title is required.")
      return
    }
    if (content.trim().isEmpty()) {
      onError("Template content is required.")
      return
    }

    viewModelScope.launch {
      templateRepository.saveTemplate(
        id = id,
        title = title,
        content = content,
        categoryName = categoryName,
      )
      filterState.update {
        it.copy(feedbackMessage = if (id == null) "Template created." else "Template updated.")
      }
      onSuccess()
    }
  }

  fun duplicateTemplate(templateId: String) {
    viewModelScope.launch {
      templateRepository.duplicateTemplate(templateId)
      filterState.update { it.copy(feedbackMessage = "Template duplicated.") }
    }
  }

  fun deleteTemplate(templateId: String) {
    viewModelScope.launch {
      templateRepository.deleteTemplate(templateId)
      filterState.update { it.copy(feedbackMessage = "Template deleted.") }
    }
  }

  fun extractVariables(content: String): List<String> =
    templateProcessor.extractVariableKeys(content)

  fun processTemplate(content: String, values: Map<String, String>): TemplateProcessResult =
    templateProcessor.process(content, values)
}
