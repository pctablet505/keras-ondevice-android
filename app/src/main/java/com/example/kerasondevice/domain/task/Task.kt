package com.example.kerasondevice.domain.task

import com.example.kerasondevice.domain.model.ModelHandle

interface Task<I, O> {
    val id: String
    val name: String
    val description: String
    val inputType: InputType<I>
    val outputType: OutputType<O>
    val preferredModels: List<String>

    suspend fun run(model: ModelHandle, input: I): TaskResult<O>
}
