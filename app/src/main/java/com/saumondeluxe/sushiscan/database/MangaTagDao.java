package com.saumondeluxe.sushiscan.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * DAO pour gérer les tags de mangas dans la base de données
 */
@Dao
public interface MangaTagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertTag(MangaTagEntity tag);

    @Update
    void updateTag(MangaTagEntity tag);

    @Delete
    void deleteTag(MangaTagEntity tag);

    @Query("SELECT * FROM manga_tags")
    LiveData<List<MangaTagEntity>> getAllTags();

    @Query("SELECT * FROM manga_tags WHERE tagType = :tagType")
    LiveData<List<MangaTagEntity>> getTagsByType(int tagType);

    @Query("SELECT * FROM manga_tags WHERE id = :tagId")
    MangaTagEntity getTagById(long tagId);

    @Query("SELECT * FROM manga_tags WHERE name = :tagName LIMIT 1")
    MangaTagEntity getTagByName(String tagName);
}