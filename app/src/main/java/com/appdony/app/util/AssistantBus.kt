package com.appdony.app.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class SayKind { TIP, SEARCH, DOWNLOAD, DONE, FAIL, PLUS, SUGGEST }

data class AssistantSay(val text: String, val kind: SayKind = SayKind.TIP)

/** پل بین بخش‌های اپ (مثل دانلود) و دکتر طاعون؛ و حافظه‌ی «چیزهایی که دکتر گفته» برای هوش مصنوعی */
object AssistantBus {

    private val _events = MutableSharedFlow<AssistantSay>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<AssistantSay> = _events.asSharedFlow()

    private val shown = ArrayDeque<String>()

    /** از هر جای اپ (حتی سرویس دانلود) صدا بزن تا دکتر خبر بده */
    fun say(say: AssistantSay) {
        noteShown(say.text)
        _events.tryEmit(say)
    }

    @Synchronized
    fun noteShown(text: String) {
        shown.addLast(text)
        while (shown.size > 6) shown.removeFirst()
    }

    @Synchronized
    fun recentNotes(): String = shown.joinToString("\n") { "- $it" }
}
