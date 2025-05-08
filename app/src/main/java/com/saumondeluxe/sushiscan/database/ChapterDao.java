package com.saumondeluxe.sushiscan.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Interface d'accès aux données pour l'entité ChapterEntity
 */
@Dao
public interface ChapterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertChapter(ChapterEntity chapter);

    @Update
    void updateChapter(ChapterEntity chapter);

    @Query("UPDATE chapters SET lastReadTime = :timestamp WHERE id = :chapterId")
    void updateLastReadTime(long chapterId, long timestamp);

    @Query("UPDATE chapters SET isDownloaded = :isDownloaded WHERE id = :chapterId")
    void updateDownloadStatus(long chapterId, boolean isDownloaded);

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    ChapterEntity getChapterById(long chapterId);

    @Query("SELECT * FROM chapters WHERE mangaName = :mangaName AND scanType = :scanType AND number = :number LIMIT 1")
    ChapterEntity getChapterByNumber(String mangaName, String scanType, int number);

    @Query("SELECT * FROM chapters WHERE mangaName = :mangaName AND scanType = :scanType ORDER BY number DESC")
    LiveData<List<ChapterEntity>> getChaptersByManga(String mangaName, String scanType);
    
    @Query("SELECT * FROM chapters WHERE mangaName = :mangaName AND scanType = :scanType ORDER BY number DESC")
    List<ChapterEntity> getChaptersByMangaSync(String mangaName, String scanType);

    @Query("SELECT * FROM chapters WHERE isDownloaded = 1 ORDER BY lastReadTime DESC LIMIT 20")
    LiveData<List<ChapterEntity>> getRecentDownloadedChapters();
    
    @Query("SELECT * FROM chapters WHERE isDownloaded = 1 ORDER BY lastReadTime DESC LIMIT 20")
    List<ChapterEntity> getRecentDownloadedChaptersSync();

    @Query("DELETE FROM chapters WHERE id = :chapterId")
    void deleteChapter(long chapterId);

    @Query("DELETE FROM chapters WHERE mangaName = :mangaName AND scanType = :scanType")
    void deleteAllChaptersForManga(String mangaName, String scanType);
}