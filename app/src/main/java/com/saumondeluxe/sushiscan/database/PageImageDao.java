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
 * Interface d'accès aux données pour l'entité PageImageEntity
 */
@Dao
public interface PageImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPage(PageImageEntity page);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPageImages(List<PageImageEntity> pageImages);

    @Update
    void updatePage(PageImageEntity page);

    @Delete
    void deletePageImage(PageImageEntity pageImage);

    @Query("SELECT * FROM page_images WHERE chapterId = :chapterId ORDER BY pageNumber")
    LiveData<List<PageImageEntity>> getPagesByChapter(long chapterId);

    @Query("SELECT * FROM page_images WHERE chapterId = :chapterId ORDER BY pageNumber")
    List<PageImageEntity> getPagesByChapterSync(long chapterId);

    @Query("SELECT COUNT(*) FROM page_images WHERE chapterId = :chapterId AND isDownloaded = 1")
    int getDownloadedPageCount(long chapterId);

    @Query("SELECT COUNT(*) FROM page_images WHERE chapterId = :chapterId")
    int getTotalPageCount(long chapterId);

    @Query("UPDATE page_images SET isDownloaded = :isDownloaded, localImagePath = :localPath WHERE id = :pageId")
    void updateDownloadStatus(long pageId, boolean isDownloaded, String localPath);

    @Query("SELECT * FROM page_images WHERE id = :pageId")
    PageImageEntity getPageById(long pageId);

    @Query("SELECT * FROM page_images WHERE chapterId = :chapterId AND pageNumber = :pageNumber LIMIT 1")
    PageImageEntity getPageByNumber(long chapterId, int pageNumber);

    @Query("DELETE FROM page_images WHERE id = :pageId")
    void deletePage(long pageId);

    @Query("DELETE FROM page_images WHERE chapterId = :chapterId")
    void deleteAllPagesForChapter(long chapterId);
}