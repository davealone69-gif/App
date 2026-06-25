package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_definitions")
data class AppDefinition(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String,
    val description: String,
    val layoutType: String = "list_detail_form",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "app_fields",
    foreignKeys = [
        ForeignKey(
            entity = AppDefinition::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("appId")]
)
data class AppField(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appId: Int,
    val name: String,
    val type: String, // "text", "number", "date", "checkbox", "dropdown"
    val options: String = "" // Comma-separated options
)

@Entity(
    tableName = "app_records",
    foreignKeys = [
        ForeignKey(
            entity = AppDefinition::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("appId")]
)
data class AppRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appId: Int,
    val dataJson: String, // Custom separation like key::value||key2::value2
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface AppMakerDao {
    @Query("SELECT * FROM app_definitions ORDER BY createdAt DESC")
    fun getAllApps(): Flow<List<AppDefinition>>

    @Query("SELECT * FROM app_definitions WHERE id = :id")
    suspend fun getAppById(id: Int): AppDefinition?

    @Query("SELECT * FROM app_fields WHERE appId = :appId")
    fun getFieldsForApp(appId: Int): Flow<List<AppField>>

    @Query("SELECT * FROM app_fields WHERE appId = :appId")
    suspend fun getFieldsForAppSync(appId: Int): List<AppField>

    @Query("SELECT * FROM app_records WHERE appId = :appId ORDER BY createdAt DESC")
    fun getRecordsForApp(appId: Int): Flow<List<AppRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppDefinition): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertField(field: AppField): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AppRecord): Long

    @Update
    suspend fun updateApp(app: AppDefinition)

    @Update
    suspend fun updateRecord(record: AppRecord)

    @Query("DELETE FROM app_definitions WHERE id = :id")
    suspend fun deleteAppById(id: Int)

    @Query("DELETE FROM app_records WHERE id = :id")
    suspend fun deleteRecordById(id: Int)
}

@Database(
    entities = [AppDefinition::class, AppField::class, AppRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppMakerDao
}
