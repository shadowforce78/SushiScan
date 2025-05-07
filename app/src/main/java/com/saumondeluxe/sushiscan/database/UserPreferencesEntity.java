package com.saumondeluxe.sushiscan.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entité pour stocker les préférences de l'utilisateur
 */
@Entity(tableName = "user_preferences")
public class UserPreferencesEntity {

    @PrimaryKey
    private long id;

    private boolean darkMode;
    private boolean autoDownloadChapters;
    private boolean showNSFWContent;
    private int maxConcurrentDownloads;
    private boolean notificationsEnabled;

    public UserPreferencesEntity() {
        // Constructeur vide requis par Room
    }

    // Getters et Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public boolean isAutoDownloadChapters() {
        return autoDownloadChapters;
    }

    public void setAutoDownloadChapters(boolean autoDownloadChapters) {
        this.autoDownloadChapters = autoDownloadChapters;
    }

    public boolean isShowNSFWContent() {
        return showNSFWContent;
    }

    public void setShowNSFWContent(boolean showNSFWContent) {
        this.showNSFWContent = showNSFWContent;
    }

    public int getMaxConcurrentDownloads() {
        return maxConcurrentDownloads;
    }

    public void setMaxConcurrentDownloads(int maxConcurrentDownloads) {
        this.maxConcurrentDownloads = maxConcurrentDownloads;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }
}