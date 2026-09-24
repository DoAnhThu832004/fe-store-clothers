package org.example.project.domain.model

import org.jetbrains.compose.resources.DrawableResource

data class IntroPageData(
    val image: DrawableResource,
    val title: String,
    val subtitle: String
)
