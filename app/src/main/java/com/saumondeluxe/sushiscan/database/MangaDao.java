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
 * DAO pour accéder aux données des mangas dans la base de données
 */
@Dao
public interface MangaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertManga(MangaEntity manga);

    @Update
    void updateManga(MangaEntity manga);

    @Delete
    void deleteManga(MangaEntity manga);

    @Query("SELECT * FROM mangas")
    LiveData<List<MangaEntity>> getAllManga();

    @Query("SELECT * FROM mangas WHERE id = :mangaId")
    LiveData<MangaEntity> getMangaById(long mangaId);

    @Query("SELECT * FROM mangas WHERE name = :name LIMIT 1")
    MangaEntity getMangaByName(String name);

    @Query("SELECT * FROM mangas ORDER BY lastReadTime DESC LIMIT :limit")
    LiveData<List<MangaEntity>> getRecentlyReadManga(int limit);
    
    @Query("SELECT * FROM mangas ORDER BY lastReadTime DESC LIMIT :limit")
    List<MangaEntity> getRecentlyReadMangasSync(int limit);

    @Query("SELECT * FROM mangas ORDER BY lastReadTime DESC")
    LiveData<List<MangaEntity>> getRecentlyReadManga();

    @Query("UPDATE mangas SET lastReadTime = :timestamp WHERE id = :mangaId")
    void updateLastReadTime(long mangaId, long timestamp);

    @Query("UPDATE mangas SET lastReadChapterId = :chapterId, lastReadChapterNumber = :chapterNumber, " +
            "lastReadChapterName = :chapterName, lastReadPagePosition = :pagePosition, lastReadTime = :timestamp " +
            "WHERE id = :mangaId")
    void updateReadingProgress(long mangaId, long chapterId, int chapterNumber,
            String chapterName, int pagePosition, long timestamp);

    @Query("SELECT * FROM mangas WHERE lastReadChapterId > 0 ORDER BY lastReadTime DESC")
    LiveData<List<MangaEntity>> getMangasWithReadingProgress();

    @Query("SELECT COUNT(*) FROM mangas WHERE name = :name")
    int mangaExists(String name);
    
    // Méthodes ajoutées pour les fonctionnalités de favoris et de bibliothèque
    
    @Query("SELECT * FROM mangas WHERE isFavorite = 1")
    LiveData<List<MangaEntity>> getFavoriteMangas();
    
    @Query("SELECT * FROM mangas WHERE isFavorite = 1")
    List<MangaEntity> getFavoriteMangasSync();
    
    @Query("SELECT * FROM mangas WHERE isDownloaded = 1")
    LiveData<List<MangaEntity>> getDownloadedMangas();
    
    @Query("SELECT * FROM mangas WHERE isDownloaded = 1")
    List<MangaEntity> getDownloadedMangasSync();
    
    @Query("UPDATE mangas SET isFavorite = :isFavorite WHERE id = :mangaId")
    void updateFavoriteStatus(long mangaId, boolean isFavorite);
    
    @Query("UPDATE mangas SET isDownloaded = :isDownloaded WHERE id = :mangaId")
    void updateDownloadStatus(long mangaId, boolean isDownloaded);
}