package com.appdony.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "cached_posts")
data class CachedPost(
    @PrimaryKey val id: Int,
    val jsonData: String,
    val categoryId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface PostDao {
    @Query("SELECT * FROM cached_posts WHERE categoryId = :categoryId ORDER BY id DESC")
    suspend fun getPostsByCategory(categoryId: Int): List<CachedPost>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<CachedPost>)

    @Query("DELETE FROM cached_posts WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Int)
}

@Database(entities = [CachedPost::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "appdony_database"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
