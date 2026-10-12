package com.appdony.app.data.model

data class InitialData(
    val banner: Post?,
    val singleFeature: Post?,
    val featured: List<Post>,
    val categories: List<Category>
)
