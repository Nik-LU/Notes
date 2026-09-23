package com.practicum.noteslu.presentation.screens.creation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicum.noteslu.domain.ContentItem
import com.practicum.noteslu.domain.ContentItem.*
import com.practicum.noteslu.domain.GetOrCreateDraftUseCase
import com.practicum.noteslu.domain.Note
import com.practicum.noteslu.domain.PublishDraftUseCase
import com.practicum.noteslu.domain.SaveDraftUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class CreateNoteViewModel @Inject constructor(
    private val saveDraftUseCase: SaveDraftUseCase,
    private val publishDraftUseCase: PublishDraftUseCase,
    private val getOrCreateDraftUseCase: GetOrCreateDraftUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<CreateNoteState>(
        CreateNoteState.Loading
    )
    val state = _state.asStateFlow()

    private var draftAutosaveJob: Job? = null

    private val draftSaveMutex = Mutex()

    private var draftChangeVersion = 0L

    private companion object {
        const val AUTOSAVE_DELAY_MILLIS = 500L
    }

    init {
        viewModelScope.launch {
            val draft = getOrCreateDraftUseCase()

            _state.value = CreateNoteState.Creation(
                note = draft
            )
        }
    }

    fun processCommand(command: CreateNoteCommand) {
        when (command) {

            CreateNoteCommand.Back -> {
                val creationState = _state.value as? CreateNoteState.Creation ?: return

                if (creationState.isSaving) return

                _state.update { currentState ->
                    if (currentState is CreateNoteState.Creation) {
                        currentState.copy(isSaving = true)
                    } else {
                        currentState
                    }
                }

                viewModelScope.launch {
                    try {
                        draftSaveMutex.withLock {
                            val currentState = _state.value as? CreateNoteState.Creation
                                ?: return@withLock

                            saveDraftUseCase(currentState.note)

                            _state.value = CreateNoteState.Finished
                        }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        _state.update { currentState ->
                            if (currentState is CreateNoteState.Creation) {
                                currentState.copy(isSaving = false)
                            } else {
                                currentState
                            }
                        }
                    }
                }
            }

            is CreateNoteCommand.InputContent -> {
                _state.update { previousState ->
                    if (previousState is CreateNoteState.Creation) {
                        val newContent = previousState.note.content
                            .mapIndexed { index, contentItem ->
                                if (index == command.index && contentItem is ContentItem.Text) {
                                    contentItem.copy(content = command.content)
                                } else {
                                    contentItem
                                }
                            }

                        previousState.copy(
                            note = previousState.note.copy(
                                content = newContent
                            )
                        )
                    } else {
                        previousState
                    }
                }
                scheduleDraftAutosave()
            }

            is CreateNoteCommand.InputTitle -> {
                _state.update { previousState ->
                    if (previousState is CreateNoteState.Creation) {
                        previousState.copy(
                            note = previousState.note.copy(
                                title = command.title
                            )
                        )
                    } else {
                        previousState
                    }
                }
                scheduleDraftAutosave()
            }

            CreateNoteCommand.Save -> {
                val creationState = _state.value as? CreateNoteState.Creation ?: return

                if (!creationState.isSaveEnabled || creationState.isSaving) return

                _state.update { currentState ->
                    if (currentState is CreateNoteState.Creation) {
                        currentState.copy(isSaving = true)
                    } else {
                        currentState
                    }
                }

                viewModelScope.launch {
                    try {
                        draftSaveMutex.withLock {
                            val currentState = _state.value as? CreateNoteState.Creation
                                ?: return@withLock

                            val noteToPublish = currentState.note.copy(
                                content = currentState.note.content.filter { contentItem ->
                                    contentItem !is ContentItem.Text || contentItem.content.isNotBlank()
                                }
                            )

                            val savedDraft = saveDraftUseCase(noteToPublish)
                                ?: return@withLock

                            publishDraftUseCase(savedDraft)

                            _state.value = CreateNoteState.Finished
                        }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        _state.update { currentState ->
                            if (currentState is CreateNoteState.Creation) {
                                currentState.copy(isSaving = false)
                            } else {
                                currentState
                            }
                        }
                    }
                }
            }

            is CreateNoteCommand.AddImage -> {
                _state.update { previousState ->
                    if (previousState is CreateNoteState.Creation) {
                        val newContent = previousState.note.content
                            .toMutableList()
                            .apply {
                                val lastItem = last()

                                if (lastItem is ContentItem.Text && lastItem.content.isBlank()) {
                                    removeAt(lastIndex)
                                }

                                add(Image(command.uri.toString()))
                                add(Text(""))
                            }

                        previousState.copy(
                            note = previousState.note.copy(
                                content = newContent
                            )
                        )
                    } else {
                        previousState
                    }
                }
                scheduleDraftAutosave()
            }

            is CreateNoteCommand.DeleteImage -> {
                _state.update { previousState ->
                    if (previousState is CreateNoteState.Creation) {
                        val newContent = previousState.note.content
                            .toMutableList()
                            .apply {
                                removeAt(command.index)
                            }

                        previousState.copy(
                            note = previousState.note.copy(
                                content = newContent
                            )
                        )
                    } else {
                        previousState
                    }
                }
                scheduleDraftAutosave()
            }
        }
    }

    private fun scheduleDraftAutosave() {
        draftChangeVersion += 1

        if (draftAutosaveJob?.isActive == true) return

        draftAutosaveJob = viewModelScope.launch {
            while (true) {
                val versionToSave = draftChangeVersion

                delay(AUTOSAVE_DELAY_MILLIS)

                if (versionToSave != draftChangeVersion) continue

                draftSaveMutex.withLock {
                    val creationState = _state.value as? CreateNoteState.Creation
                        ?: return@withLock

                    if (creationState.isSaving) return@withLock

                    val savedDraft = saveDraftUseCase(creationState.note)

                    _state.update { latestState ->
                        if (latestState !is CreateNoteState.Creation) {
                            latestState
                        } else if (savedDraft == null) {
                            latestState.copy(
                                note = latestState.note.copy(id = 0)
                            )
                        } else if (latestState.note == creationState.note) {
                            latestState.copy(note = savedDraft)
                        } else if (latestState.note.id == 0) {
                            latestState.copy(
                                note = latestState.note.copy(id = savedDraft.id)
                            )
                        } else {
                            latestState
                        }
                    }
                }

                if (versionToSave == draftChangeVersion) return@launch
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
            val note: Note,
            val isSaving: Boolean = false
        ) : CreateNoteState {
            val isSaveEnabled: Boolean
                get() = note.title.isNotBlank() &&
                        note.content.any { item ->
                            when (item) {
                                is ContentItem.Text -> item.content.isNotBlank()
                                is ContentItem.Image -> true
                            }
                        }
        }

        data object Finished : CreateNoteState
        data object Loading : CreateNoteState
    }

}