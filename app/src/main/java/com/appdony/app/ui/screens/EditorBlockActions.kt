package com.appdony.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.api.EditorApi
import com.appdony.app.data.model.BlockType
import com.appdony.app.data.model.ImageData
import com.appdony.app.ui.components.AppBanner
import com.appdony.app.ui.components.BannerKind
import kotlinx.coroutines.launch

/** نوع یک بلوک رو عوض می‌کنه (مثلاً فهرست نقطه‌ای ↔ شماره‌دار) */
fun EditorViewModel.changeType(id: Long, type: BlockType) {
    val i = blocks.indexOfFirst { it.id == id }
    if (i >= 0 && blocks[i].type != type) blocks[i] = blocks[i].copy(type = type)
}

/** تصویر رو آپلود می‌کنه و توی همون بلوک تصویر می‌ذاره (چیزی روی گوشی ذخیره نمی‌شه) */
fun EditorViewModel.uploadBlockImage(context: Context, blockId: Long, uri: Uri) {
    if (uploading) return
    val app = context.applicationContext
    uploading = true
    viewModelScope.launch {
        val result = EditorApi.uploadImage(app, uri)
        val media = result.getOrNull()
        if (media != null && media.url.isNotBlank()) {
            updateText(blockId, ImageData.encode(media.id, media.url))
        } else {
            val message = result.exceptionOrNull()?.message ?: "آدرس تصویر از سایت گرفته نشد"
            AppBanner.show(BannerKind.ERROR, message, 4500L)
        }
        uploading = false
    }
}
