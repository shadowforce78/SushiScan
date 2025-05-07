package com.saumondeluxe.sushiscan.database;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entité pour représenter un manga dans la base de données
 */
@Entity(tableName = "mangas", indices = { @Index(value = { "name" }, unique = true) })
public class MangaEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private String imageUrl;
    private String description;
    private String author;
    private int yearOfRelease;
    private boolean isFavorite;
    private long lastReadTime;
    private long lastUpdateTime;

    // Nouveaux champs pour la progression de lecture
    private long lastReadChapterId;
    private int lastReadChapterNumber;
    private int lastReadPagePosition;
    private String lastReadChapterName;
    private String scanTypeUrl; // URL pour accéder au type de scan (ex: "scan", "vf", etc.)

    // Constructeurs
    public MangaEntity() {
        // Constructeur vide requis par Room
    }

    @Ignore
    public MangaEntity(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.lastReadTime = System.currentTimeMillis();
        this.lastUpdateTime = System.currentTimeMillis();
        this.lastReadPagePosition = 0;
    }

    // Getters et Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getYearOfRelease() {
        return yearOfRelease;
    }

    public void setYearOfRelease(int yearOfRelease) {
        this.yearOfRelease = yearOfRelease;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public long getLastReadTime() {
        return lastReadTime;
    }

    public void setLastReadTime(long lastReadTime) {
        this.lastReadTime = lastReadTime;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    // Nouveaux getters et setters pour la progression de lecture
    public long getLastReadChapterId() {
        return lastReadChapterId;
    }

    public void setLastReadChapterId(long lastReadChapterId) {
        this.lastReadChapterId = lastReadChapterId;
    }

    public int getLastReadChapterNumber() {
        return lastReadChapterNumber;
    }

    public void setLastReadChapterNumber(int lastReadChapterNumber) {
        this.lastReadChapterNumber = lastReadChapterNumber;
    }

    public int getLastReadPagePosition() {
        return lastReadPagePosition;
    }

    public void setLastReadPagePosition(int lastReadPagePosition) {
        this.lastReadPagePosition = lastReadPagePosition;
    }

    public String getLastReadChapterName() {
        return lastReadChapterName;
    }

    public void setLastReadChapterName(String lastReadChapterName) {
        this.lastReadChapterName = lastReadChapterName;
    }

    public String getScanTypeUrl() {
        return scanTypeUrl;
    }

    public void setScanTypeUrl(String scanTypeUrl) {
        this.scanTypeUrl = scanTypeUrl;
    }

    /**
     * Met à jour les informations de lecture pour ce manga
     * 
     * @param chapterId     ID du chapitre lu
     * @param chapterNumber Numéro du chapitre lu
     * @param chapterName   Nom du chapitre lu
     * @param pagePosition  Position de la page dans le chapitre
     * @param scanTypeUrl   URL du type de scan (pour rouvrir le manga)
     */
    public void updateReadingProgress(long chapterId, int chapterNumber, String chapterName, int pagePosition,
            String scanTypeUrl) {
        this.lastReadChapterId = chapterId;
        this.lastReadChapterNumber = chapterNumber;
        this.lastReadChapterName = chapterName;
        this.lastReadPagePosition = pagePosition;
        this.scanTypeUrl = scanTypeUrl;
        this.lastReadTime = System.currentTimeMillis();
    }
}