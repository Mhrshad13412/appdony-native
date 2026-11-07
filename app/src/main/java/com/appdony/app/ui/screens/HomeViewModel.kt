package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.TopChartsData
import com.appdony.app.data.repository.HomeRepository
import com.appdony.app.data.repository.HomeSection
import com.appdony.app.util.LatestOnly
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SectionState(
    val section: HomeSection,
    val posts: List<Post>? = null, // null یعنی هنوز لود نشده
    val plusStyle: Boolean = false // true یعنی با کارت‌های «اپدونی پلاس ساده» نشون داده بشه
)

/** تا کدوم ردیف‌ها (از اول) یکی‌درمیان با استایل «پلاس ساده» نشون داده بشن */
private const val PLUS_SIMPLE_SPAN = 6

class HomeViewModel(
    private val repository: HomeRepository = HomeRepository()
) : ViewModel() {

    // بلاک بالای صفحه: «اپدونی پلاس»
    private val _plus = MutableStateFlow<List<Post>?>(null)
    val plus: StateFlow<List<Post>?> = _plus.asStateFlow()

    // بلاک «برترین‌ها»: null یعنی هنوز لود نشده
    private val _topCharts = MutableStateFlow<TopChartsData?>(null)
    val topCharts: StateFlow<TopChartsData?> = _topCharts.asStateFlow()

    private val _sections = MutableStateFlow<List<SectionState>>(emptyList())
    val sections: StateFlow<List<SectionState>> = _sections.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    private var rootId: Int? = null
    /** آی‌دی دسته‌ی اصلی تب؛ برای دکمه‌ی «بیشتر» ردیف‌های ویژه */
    val rootCategoryId: Int? get() = rootId
    private var loadedTab: Boolean? = null
    private val requested = mutableSetOf<Int>()

    // پست‌های خام هر ردیف؛ نمایش از روی این‌ها ساخته می‌شه تا یه اپ توی چند ردیف تکرار نشه
    private val raw = mutableMapOf<Int, List<Post>>()
    private var base: List<HomeSection> = emptyList()

    // ردیف‌های اول به‌صورت یکی‌درمیان «پلاس ساده» (۱، ۳، ۵) و بینشون ردیف ساده‌ی معمولی میاد؛
    // استایل از جای ردیف توی لیست اصلی (base) حساب می‌شه، پس با لود شدن یا خالی شدن ردیف‌ها عوض نمی‌شه
    private fun plusStyleOf(sectionId: Int): Boolean {
        val i = base.indexOfFirst { it.id == sectionId }
        return i in 0 until PLUS_SIMPLE_SPAN && i % 2 == 0
    }

    private fun rebuild() {
        // از هر اپ فقط بهترین پست (بالاترین نسخه) توی کل صفحه می‌مونه، و فقط توی اولین ردیفش
        val keep = LatestOnly.latestPerApp(base.filter { it.id > 0 }.flatMap { raw[it.id].orEmpty() }).mapTo(HashSet<Int>()) { it.id }
        val seen = HashSet<Int>()
        _sections.value = base.mapNotNull { s ->
            val posts = raw[s.id] ?: return@mapNotNull SectionState(s, plusStyle = plusStyleOf(s.id))
            // ردیف‌های ویژه‌ی بالا (پربازدید/جدید/به‌روز) تکراری‌گیری نمی‌شن
            if (s.id < 0) return@mapNotNull if (posts.isEmpty()) null else SectionState(s, posts, plusStyleOf(s.id))
            val unique = posts.filter { it.id in keep && seen.add(it.id) }
            if (unique.isEmpty()) null else SectionState(s, unique, plusStyleOf(s.id))
        }
    }

    fun load(isGames: Boolean, force: Boolean = false) {
        if (!force && loadedTab == isGames) return
        loadedTab = isGames
        requested.clear()
        raw.clear()
        base = emptyList()
        _plus.value = null
        _topCharts.value = null
        _sections.value = emptyList()
        _isLoading.value = true
        _hasError.value = false

        viewModelScope.launch {
            try {
                val structure = repository.loadStructure(isGames)
                rootId = structure.rootId
                // اول فقط عنوان ردیف‌ها نشون داده می‌شه (متن اول، عکس بعد)
                base = structure.sections
                _sections.value = base.map { SectionState(it, plusStyle = plusStyleOf(it.id)) }
                _isLoading.value = false

                launch {
                    _plus.value = try {
                        repository.loadPlus(isGames)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                launch {
                    _topCharts.value = try {
                        repository.loadTopCharts(isGames, structure.rootId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        TopChartsData(emptyList(), emptyList())
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
    // اگه شبکه ضعیف بود و ردیف نیومد، خودش چند بار با فاصله‌ی کم دوباره تلاش می‌کنه تا کاربر روی اسکلت گیر نکنه
    fun loadSection(id: Int) {
        if (!requested.add(id)) return
        val section = base.firstOrNull { it.id == id } ?: return

        viewModelScope.launch {
            var attempt = 0
            while (true) {
                try {
                    val posts = repository.loadSection(section, rootId)
                    raw[id] = posts
                    rebuild()
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    attempt++
                    if (attempt >= 4) {
                        requested.remove(id) // دفعه‌ی بعد که ردیف دوباره دیده شد، دوباره تلاش می‌کنه
                        return@launch
                    }
                    delay(1_200L * attempt)
                }
            }
        }
    }
}
