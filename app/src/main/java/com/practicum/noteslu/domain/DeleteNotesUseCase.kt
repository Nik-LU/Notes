package com.practicum.noteslu.domain

import javax.inject.Inject

class DeleteNotesUseCase @Inject constructor(
    private val repository: NotesRepository
) {

    suspend operator fun invoke(noteId: Int){
        repository.deleteNote(noteId)
    }

}