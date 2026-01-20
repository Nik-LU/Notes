package com.practicum.noteslu.presentation.screens.creation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicum.noteslu.domain.AddNoteUseCase
import com.practicum.noteslu.domain.ContentItem
import com.practicum.noteslu.domain.ContentItem.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateNoteViewModel @Inject constructor(
    private val addNoteUseCase: AddNoteUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<CreateNoteState>(
        CreateNoteState.Creation()
    )
    val state = _state.asStateFlow()

    fun processCommand(command: CreateNoteCommand) {
        when (command) {

            CreateNoteCommand.Back -> {
                _state.update { CreateNoteState.Finished }
            }

            is CreateNoteCommand.InputContent -> {
                _state.update { previousState ->
                    if (previousState is CreateNoteState.Creation) {
                        val newContent = previousState.content
                            .mapIndexed { index, contentItem ->
                                if (index == command.index && contentItem is ContentItem.Text) {
                                    contentItem.copy(content = command.content)
                                } else {
                                    contentItem
                                }
                            }
                        previousState.copy(
                            content = newContent
                        )
                    } else {
                        previousState
                    }
                }
            }

            is CreateNoteCommand.InputTitle -> {
                _state.update { previousState ->
                    if (previousState is CreateNoteState.Creation) {
                        previousState.copy(
                            title = command.title
                        )
                    } else {
                        previousState
                    }
                }
            }

            CreateNoteCommand.Save -> {
                viewModelScope.launch {
                    _state.update { previousState ->
                        if (previousState is CreateNoteState.Creation) {
                            val title = previousState.title
                            val content = previousState.content.filter {
                                it !is ContentItem.Text || it.content.isNotBlank()
                            }
                            addNoteUseCase(title, content)
                            CreateNoteState.Finished
                        } else {
                            previousState
                        }
                    }
                }
            }

            is CreateNoteCommand.AddImage -> {
                _state.update { previousState ->
                    if (previousState is CreateNoteState.Creation) {
                        previousState.content.toMutableList().apply {
                            val lastItem = last()
                            if (lastItem is ContentItem.Text && lastItem.content.isBlank()) {
                                removeAt(lastIndex)
                            }
                            add(Image(command.uri.toString()))
                            add(Text(""))
                        }.let {
                            previousState.copy(content = it)
                        }
                    } else {
                        previousState
                    }
                }
            }

            is CreateNoteCommand.DeleteImage -> {
                _state.update { previousState ->
                    if (previousState is CreateNoteState.Creation) {
                        previousState.content.toMutableList().apply {
                           removeAt(command.index)
                        }.let {
                            previousState.copy(content = it)
                        }
                    } else {
                        previousState
                    }
                }
            }
        }

    }


    sealed interface CreateNoteCommand {

        data class InputTitle(val title: String) : CreateNoteCommand

        data class InputContent(val content: String, val index: Int) : CreateNoteCommand

        data class AddImage(val uri: Uri) : CreateNoteCommand

        data class DeleteImage(val index: Int) : CreateNoteCommand

        data object Save : CreateNoteCommand

        data object Back : CreateNoteCommand

    }


    sealed interface CreateNoteState {

        data class Creation(
            val title: String = "",
            val content: List<ContentItem> = listOf(ContentItem.Text(""))
        ) : CreateNoteState {
            val isSaveEnabled: Boolean
                get() {
                    return when {
                        title.isBlank() -> false
                        content.isEmpty() -> false
                        else -> {
                            content.any {
                                it !is ContentItem.Text || it.content.isNotBlank()
                            }
                        }
                    }
                }
        }

        data object Finished : CreateNoteState
    }

}