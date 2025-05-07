package com.saumondeluxe.sushiscan.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entité représentant un chapitre de manga dans la base de données
 */
@Entity(tableName = "chapters", indices = { @Index(value = { "mangaName", "scanType", "number" }, unique = true) })
public class ChapterEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String mangaName;
    private String scanType;
    private int number;
    private String name;
    private boolean isDownloaded;
    private long lastReadTime;

    // Getters et Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMangaName() {
        return mangaName;
    }

    public void setMangaName(String mangaName) {
        this.mangaName = mangaName;
    }

    public String getScanType() {
        return scanType;
    }

    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    public long getLastReadTime() {
        return lastReadTime;
    }

    public void setLastReadTime(long lastReadTime) {
        this.lastReadTime = lastReadTime;
    }
}