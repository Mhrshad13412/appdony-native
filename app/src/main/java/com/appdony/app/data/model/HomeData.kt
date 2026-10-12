package com.appdony.app.data.model

data class HomeData(
    val banner: Post?,
    val singleFeature: Post?,
    val sections: List<HomeSection>
)

data class HomeSection(
    val categoryId: Int,
    val title: String,
    val posts: List<Post>
)
