package com.practicum.noteslu.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDraftUseCase @Inject constructor(
    private val repository: NotesRepository
) {

    operator fun invoke(): Flow<Note?> {
        return repository.observeDraft()
    }
}