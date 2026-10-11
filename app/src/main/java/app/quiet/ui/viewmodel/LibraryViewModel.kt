package app.quiet.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.quiet.QuietApplication
import app.quiet.data.db.LibraryItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryUiState(
    val items: List<LibraryItemEntity> = emptyList(),
    val typeFilter: String = "All",
    val statusFilter: String? = null,
    val emptyMessage: String? = null,
)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val quietApp = app as QuietApplication
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val all = quietApp.libraryRepository.all()
            val filtered = quietApp.libraryRepository.filterAndSort(
                all,
                _state.value.typeFilter,
                _state.value.statusFilter,
            )
            _state.value = _state.value.copy(
                items = filtered,
                emptyMessage = when {
                    all.isEmpty() -> "Track what you watch and read."
                    filtered.isEmpty() -> "Nothing in this filter."
                    else -> null
                },
            )
        }
    }

    fun setTypeFilter(type: String) {
        _state.value = _state.value.copy(typeFilter = type)
        reload()
    }

    fun setStatusFilter(status: String?) {
        _state.value = _state.value.copy(statusFilter = status)
        reload()
    }

    fun save(item: LibraryItemEntity) {
        viewModelScope.launch {
            if (item.id == 0L) {
                quietApp.libraryRepository.insert(item)
            } else {
                quietApp.libraryRepository.update(item)
            }
            reload()
        }
    }

    fun delete(item: LibraryItemEntity) {
        viewModelScope.launch {
            quietApp.libraryRepository.delete(item)
            reload()
        }
    }
}
