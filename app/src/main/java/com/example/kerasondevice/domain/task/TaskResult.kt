package com.example.kerasondevice.domain.task

sealed class TaskResult<O> {
    data class Success<O>(val output: O, val latencyMs: Long) : TaskResult<O>()
    data class Error<O>(val message: String) : TaskResult<O>()
}
