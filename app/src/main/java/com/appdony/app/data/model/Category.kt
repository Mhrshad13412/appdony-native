package com.appdony.app.data.model

data class Category(
    val id: Int,
    val name: String,
    val slug: String,
    val count: Int = 0,
    val parent: Int = 0
)
