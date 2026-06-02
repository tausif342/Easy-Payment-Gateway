package com.example.data.local

import androidx.room.*
import com.example.data.model.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY timestamp DESC")
    fun getAllProjectsFlow(): Flow<List<Project>>

    @Query("SELECT * FROM projects")
    suspend fun getAllProjects(): List<Project>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("DELETE FROM projects")
    suspend fun clearAllProjects()
}
