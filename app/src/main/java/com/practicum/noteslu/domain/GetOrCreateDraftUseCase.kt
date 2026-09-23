package com.practicum.noteslu.domain

import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetOrCreateDraftUseCase @Inject constructor(
    private val repository: NotesRepository
) {

    suspend operator fun invoke(): Note {

        return repository.observeDraft().first()

            ?: Note(
                id = 0,
                title = "",
                content = listOf(ContentItem.Text("")),
                updatedAt = System.currentTimeMillis(),
                isPinned = false,
                isDraft = true
            )
    }
}