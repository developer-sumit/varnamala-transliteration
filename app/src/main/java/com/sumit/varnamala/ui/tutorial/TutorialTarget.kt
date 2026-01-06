package com.sumit.varnamala.ui.tutorial

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

fun Modifier.captureBounds(
    key: String,
    onCaptured: (key: String, rect: Rect) -> Unit,
): Modifier = this.then(
    Modifier.onGloballyPositioned { coordinates ->
        onCaptured(key, coordinates.boundsInRoot())
    }
)
