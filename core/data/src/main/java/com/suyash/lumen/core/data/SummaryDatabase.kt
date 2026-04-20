package com.suyash.lumen.core.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Entity(tableName = "summaries")
data class SummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "source_text") val sourceText: String,
    @ColumnInfo(name = "summary") val summary: String,
    @ColumnInfo(name = "engine_id") val engineId: String,
    @ColumnInfo(name = "mode") val mode: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Dao
interface SummaryDao {
    @Query("SELECT * FROM summaries ORDER BY created_at DESC")
    fun observeAll(): Flow<List<SummaryEntity>>

    @Insert
    suspend fun insert(entity: SummaryEntity): Long

    @Query("DELETE FROM summaries WHERE id = :id")
    suspend fun delete(id: Long): Int
}

@Database(entities = [SummaryEntity::class], version = 1, exportSchema = false)
abstract class LumenDatabase : RoomDatabase() {
    abstract fun summaryDao(): SummaryDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LumenDatabase =
        Room.databaseBuilder(context, LumenDatabase::class.java, "lumen.db").build()

    @Provides
    fun provideSummaryDao(db: LumenDatabase): SummaryDao = db.summaryDao()
}
