package com.saumondeluxe.sushiscan.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

/**
 * DAO pour gérer les préférences utilisateur dans la base de données
 */
@Dao
public interface UserPreferencesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPreferences(UserPreferencesEntity preferences);

    @Update
    void updatePreferences(UserPreferencesEntity preferences);

    @Query("SELECT * FROM user_preferences WHERE id = :id")
    LiveData<UserPreferencesEntity> getPreferencesById(long id);

    @Query("SELECT * FROM user_preferences WHERE id = :id")
    UserPreferencesEntity getPreferencesByIdSync(long id);

    @Query("SELECT * FROM user_preferences LIMIT 1")
    LiveData<UserPreferencesEntity> getPreferences();

    @Query("SELECT * FROM user_preferences LIMIT 1")
    UserPreferencesEntity getPreferencesSync();
}