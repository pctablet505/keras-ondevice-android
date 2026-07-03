package com.example.kerasondevice.domain.task

import android.graphics.Bitmap

sealed class InputType<I> {
    data object Camera : InputType<Bitmap>()
    data object Gallery : InputType<Bitmap>()
}
