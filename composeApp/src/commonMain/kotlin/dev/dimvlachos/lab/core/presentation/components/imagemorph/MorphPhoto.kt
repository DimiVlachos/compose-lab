package dev.dimvlachos.lab.core.presentation.components.imagemorph

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource

@Immutable
data class MorphPhoto(val image: DrawableResource, val title: String, val caption: String)
