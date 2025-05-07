package com.saumondeluxe.sushiscan.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entité représentant une page d'image dans la base de données
 */
@Entity(tableName = "page_images", foreignKeys = @ForeignKey(entity = ChapterEntity.class, parentColumns = "id", childColumns = "chapterId", onDelete = ForeignKey.CASCADE), indices = {
        @Index("chapterId"),
        @Index(value = { "chapterId", "pageNumber" }, unique = true)
})
public class PageImageEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long chapterId;
    private int pageNumber;
    private String originalUrl;
    private String localImagePath;
    private boolean isDownloaded;
    private long downloadTime;

    // Getters et Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getChapterId() {
        return chapterId;
    }

    public void setChapterId(long chapterId) {
        this.chapterId = chapterId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getLocalImagePath() {
        return localImagePath;
    }

    public void setLocalImagePath(String localImagePath) {
        this.localImagePath = localImagePath;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    public long getDownloadTime() {
        return downloadTime;
    }

    public void setDownloadTime(long downloadTime) {
        this.downloadTime = downloadTime;
    }
}