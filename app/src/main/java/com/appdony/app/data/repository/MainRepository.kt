package com.appdony.app.data.repository

import com.appdony.app.data.api.WordPressApi
import com.appdony.app.data.model.Post

class MainRepository(private val api: WordPressApi) {
    
    suspend fun getPosts(page: Int, searchQuery: String? = null): List<Post> {
        return api.getPosts(page = page, search = searchQuery)
    }
}
