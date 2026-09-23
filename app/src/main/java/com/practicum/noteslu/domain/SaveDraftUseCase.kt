package com.practicum.noteslu.domain

import javax.inject.Inject

class SaveDraftUseCase @Inject constructor(
    private val repository: NotesRepository
) {

    suspend operator fun invoke(note: Note): Note? {
        if (!note.hasMeaningfulContent()) {
            if (note.id != 0) {
                repository.deleteDraft(note.id)
            }
            return null
        }

        val draft = note.copy(
            updatedAt = System.currentTimeMillis(),
            isDraft = true
        )

        return repository.saveDraft(draft)
    }

    private fun Note.hasMeaningfulContent(): Boolean {
        return title.isNotBlank() ||
                content.any { item ->
                    when (item) {
                        is ContentItem.Text -> item.content.isNotBlank()
                        is ContentItem.Image -> true
                    }
                }
    }
}