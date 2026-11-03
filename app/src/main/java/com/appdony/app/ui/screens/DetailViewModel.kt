package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.PostMemory
import com.appdony.app.data.repository.AppRepository
import com.appdony.app.util.AppInfo
import com.appdony.app.util.ContentBlock
import com.appdony.app.util.HtmlParser
import com.appdony.app.util.InstallLinkFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class DetailUiState {
    // پیش‌نمایش: اگه پست قبلاً تو لیستی دیده شده باشه، عکس و اسمش همون لحظه نشون داده می‌شه
    data class Loading(val preview: Post? = null) : DetailUiState()
    data class Success(
        val post: Post,
        val info: AppInfo,
        val similar: List<Post>,
        val contentBlocks: List<ContentBlock>
    ) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

class DetailViewModel(
    private val postId: Int,
    private val isBlog: Boolean
) : ViewModel() {

    private val repo = AppRepository()
    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading(PostMemory.get(postId)))
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = DetailUiState.Loading(PostMemory.get(postId))

            val postResult = if (isBlog) repo.getBlogPost(postId) else repo.getPost(postId)

            postResult.onSuccess { post ->
                val rawHtml = post.content?.rendered ?: ""

                // پارس HTML سنگینه؛ خارج از ترد اصلی انجام می‌شه
                val (blocks, info) = withContext(Dispatchers.Default) {
                    Pair(HtmlParser.parseContent(rawHtml), HtmlParser.parsePostContent(rawHtml))
                }

                // اول محتوا نشون داده می‌شه؛ «مشابه‌ها» و لینک «نصب کردن» همزمان و جدا می‌آن
                _state.value = DetailUiState.Success(
                    post, if (isBlog) info else info.copy(linksChecked = false), emptyList(), blocks
                )

                val catId = post.embedded?.terms?.firstOrNull()?.firstOrNull()?.id
                if (!isBlog && catId != null) {
                    launch {
                        val similar = repo.getPostsByCategory(catId, perPage = 8)
                            .getOrNull()?.filter { it.id != post.id }?.take(6) ?: emptyList()
                        _state.update { st -> (st as? DetailUiState.Success)?.copy(similar = similar) ?: st }
                    }
                }
                if (isBlog) return@onSuccess

                launch {
                    val found = InstallLinkFinder.find(post.id, post.link)
                    _state.update { st ->
                        val current = st as? DetailUiState.Success ?: return@update st
                        found.fold(
                            onSuccess = { r ->
                                val all = (r.links + current.info.downloadLinks).distinctBy { it.url.ifBlank { "locked:" + it.title } }
                                val real = all.filter { it.url.isNotBlank() }
                                val locked = all.filter { it.url.isBlank() }
                                // دکمه‌ی قفل فقط وقتی نشون داده می‌شه که لینک واقعی نداریم، یا اسم مشخص داره
                                val merged = real + if (real.isEmpty()) locked else locked.filter { it.title != InstallLinkFinder.LOCKED_TITLE }
                                current.copy(
                                    info = current.info.copy(
                                        downloadLinks = merged,
                                        linksChecked = true,
                                        linkError = null,
                                        linkDebug = r.debug
                                    )
                                )
                            },
                            onFailure = { e ->
                                current.copy(
                                    info = current.info.copy(
                                        linksChecked = true,
                                        linkError = "اتصال به سایت برقرار نشد (${e.message ?: "خطا"})"
                                    )
                                )
                            }
                        )
                    }
                }
            }.onFailure {
                _state.value = DetailUiState.Error(it.message ?: "خطا در دریافت اطلاعات")
            }
        }
    }
}
