package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.Post
import com.appdony.app.data.repository.HomeRepository
import com.appdony.app.data.repository.HomeSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SectionState(
    val section: HomeSection,
    val posts: List<Post>? = null // null یعنی هنوز لود نشده
)

class HomeViewModel(
    private val repository: HomeRepository = HomeRepository()
) : ViewModel() {

    // بلاک بالای صفحه: «اپدونی پلاس»
    private val _plus = MutableStateFlow<List<Post>?>(null)
    val plus: StateFlow<List<Post>?> = _plus.asStateFlow()

    private val _sections = MutableStateFlow<List<SectionState>>(emptyList())
    val sections: StateFlow<List<SectionState>> = _sections.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    private var rootId: Int? = null
    private var loadedTab: Boolean? = null
    private val requested = mutableSetOf<Int>()

    fun load(isGames: Boolean, force: Boolean = false) {
        if (!force && loadedTab == isGames) return
        loadedTab = isGames
        requested.clear()
        _plus.value = null
        _sections.value = emptyList()
        _isLoading.value = true
        _hasError.value = false

        viewModelScope.launch {
            try {
                val structure = repository.loadStructure(isGames)
                rootId = structure.rootId
                // اول فقط عنوان ردیف‌ها نشون داده می‌شه (متن اول، عکس بعد)
                _sections.value = structure.sections.map { SectionState(it) }
                _isLoading.value = false

                launch {
                    _plus.value = try {
                        repository.loadPlus(isGames)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadedTab = null
                _hasError.value = true
                _isLoading.value = false
            }
        }
    }

    // هر ردیف فقط وقتی به صفحه نزدیک شد صدا زده می‌شه
    fun loadSection(id: Int) {
        if (!requested.add(id)) return
        val section = _sections.value.firstOrNull { it.section.id == id }?.section ?: return

        viewModelScope.launch {
            try {
                val posts = repository.loadSection(section, rootId)
                _sections.update { list ->
                    if (posts.isEmpty()) {
                        list.filterNot { it.section.id == id }
                    } else {
                        list.map { if (it.section.id == id) it.copy(posts = posts) else it }
                    }
                }
            } catch (e: Exception) {
                requested.remove(id) // دفعه‌ی بعد که ردیف دوباره دیده شد، دوباره تلاش می‌کنه
            }
        }
    }
}
