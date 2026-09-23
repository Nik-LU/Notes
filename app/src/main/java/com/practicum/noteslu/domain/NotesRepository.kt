package com.practicum.noteslu.domain

import kotlinx.coroutines.flow.Flow

interface NotesRepository {

    suspend fun addNote(
        title: String,
        content: List<ContentItem>,
        isPinned: Boolean,
        updateAt: Long
    )

    suspend fun deleteNote(noteId: Int)

    suspend fun editNote(note: Note)

    fun getAllNotes(): Flow<List<Note>>

    suspend fun getNote(noteId: Int): Note

    fun searchNotes(query: String): Flow<List<Note>>

    suspend fun switchPinnedStatus(noteId: Int)

    fun observeDraft(): Flow<Note?>

    suspend fun saveDraft(note: Note): Note

    suspend fun publishDraft(note: Note)

    suspend fun deleteDraft(noteId: Int)

}