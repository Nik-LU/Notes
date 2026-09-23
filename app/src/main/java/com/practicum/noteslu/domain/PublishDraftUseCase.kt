package com.practicum.noteslu.domain

import javax.inject.Inject

class PublishDraftUseCase @Inject constructor(
    private val repository: NotesRepository
) {

    suspend operator fun invoke(note: Note) {
        val publishedNote = note.copy(
            updatedAt = System.currentTimeMillis(),
            isDraft = false
        )

        repository.publishDraft(publishedNote)
    }
}