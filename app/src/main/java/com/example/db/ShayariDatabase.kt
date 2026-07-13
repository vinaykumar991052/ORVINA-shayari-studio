package com.example.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Entity ---

@Entity(tableName = "saved_shayaris")
data class ShayariEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val shayari: String,
    val transliteration: String,
    val translation: String,
    val mood: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_shayaris")
data class RecentShayariEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val shayari: String,
    val transliteration: String,
    val translation: String,
    val mood: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumberOrEmail: String,
    val transactionId: String,
    val amount: Int = 99,
    val status: String, // "Pending", "Approved", "Rejected"
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// --- Room DAO ---

@Dao
interface ShayariDao {
    @Query("SELECT * FROM saved_shayaris ORDER BY timestamp DESC")
    fun getAllSavedShayaris(): Flow<List<ShayariEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShayari(shayari: ShayariEntity): Long

    @Query("DELETE FROM saved_shayaris WHERE id = :id")
    suspend fun deleteShayari(id: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_shayaris WHERE shayari = :text LIMIT 1)")
    suspend fun isShayariSaved(text: String): Boolean

    @Query("DELETE FROM saved_shayaris WHERE shayari = :text")
    suspend fun deleteShayariByText(text: String)

    // --- Recent Shayaris ---
    @Query("SELECT * FROM recent_shayaris ORDER BY timestamp DESC")
    fun getAllRecentShayaris(): Flow<List<RecentShayariEntity>>

    @Query("SELECT * FROM recent_shayaris ORDER BY timestamp DESC")
    suspend fun getRecentShayarisList(): List<RecentShayariEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentShayari(recent: RecentShayariEntity): Long

    @Query("DELETE FROM recent_shayaris WHERE id = :id")
    suspend fun deleteRecentShayariById(id: Int)

    // --- Subscriptions ---
    @Query("SELECT * FROM subscriptions ORDER BY timestamp DESC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE phoneNumberOrEmail = :user ORDER BY timestamp DESC")
    fun getSubscriptionsForUser(user: String): Flow<List<SubscriptionEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE phoneNumberOrEmail = :user AND status = 'Approved' LIMIT 1)")
    suspend fun isUserSubscribed(user: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: SubscriptionEntity): Long

    @Query("UPDATE subscriptions SET status = :status, reason = :reason WHERE id = :id")
    suspend fun updateSubscriptionStatus(id: Int, status: String, reason: String)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscription(id: Int)
}

// --- Room Database ---

@Database(entities = [ShayariEntity::class, RecentShayariEntity::class, SubscriptionEntity::class], version = 3, exportSchema = false)
abstract class ShayariDatabase : RoomDatabase() {
    abstract fun shayariDao(): ShayariDao

    companion object {
        @Volatile
        private var INSTANCE: ShayariDatabase? = null

        fun getDatabase(context: Context): ShayariDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShayariDatabase::class.java,
                    "shayari_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- Shayari Repository ---

class ShayariRepository(private val shayariDao: ShayariDao) {
    val savedShayaris: Flow<List<ShayariEntity>> = shayariDao.getAllSavedShayaris()
    val recentShayaris: Flow<List<RecentShayariEntity>> = shayariDao.getAllRecentShayaris()
    val allSubscriptions: Flow<List<SubscriptionEntity>> = shayariDao.getAllSubscriptions()

    fun getSubscriptionsForUser(user: String): Flow<List<SubscriptionEntity>> {
        return shayariDao.getSubscriptionsForUser(user)
    }

    suspend fun isUserSubscribed(user: String): Boolean {
        return shayariDao.isUserSubscribed(user)
    }

    suspend fun saveSubscription(sub: SubscriptionEntity): Long {
        return shayariDao.insertSubscription(sub)
    }

    suspend fun updateSubscriptionStatus(id: Int, status: String, reason: String) {
        shayariDao.updateSubscriptionStatus(id, status, reason)
    }

    suspend fun deleteSubscription(id: Int) {
        shayariDao.deleteSubscription(id)
    }

    suspend fun saveShayari(shayari: ShayariEntity): Long {
        return shayariDao.insertShayari(shayari)
    }

    suspend fun deleteShayari(id: Int) {
        shayariDao.deleteShayari(id)
    }

    suspend fun isSaved(text: String): Boolean {
        return shayariDao.isShayariSaved(text)
    }

    suspend fun unsaveShayariByText(text: String) {
        shayariDao.deleteShayariByText(text)
    }

    // --- Recent operations ---
    suspend fun saveRecentShayari(recent: RecentShayariEntity): Long {
        val id = shayariDao.insertRecentShayari(recent)
        // Keep only the last 5
        val list = shayariDao.getRecentShayarisList()
        if (list.size > 5) {
            for (i in 5 until list.size) {
                shayariDao.deleteRecentShayariById(list[i].id)
            }
        }
        return id
    }

    suspend fun deleteRecentShayari(id: Int) {
        shayariDao.deleteRecentShayariById(id)
    }
}
