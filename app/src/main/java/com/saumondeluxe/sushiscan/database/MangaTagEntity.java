package com.saumondeluxe.sushiscan.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

/**
 * Entité pour représenter un tag/genre de manga
 */
@Entity(tableName = "manga_tags")
public class MangaTagEntity {

    public static final int TAG_TYPE_GENRE = 1;
    public static final int TAG_TYPE_STATUS = 2;

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String name;

    private int tagType; // Type de tag (genre, statut, etc.)

    // Constructeurs
    public MangaTagEntity() {
        // Constructeur vide requis par Room
    }

    @Ignore
    public MangaTagEntity(@NonNull String name, int tagType) {
        this.name = name;
        this.tagType = tagType;
    }

    // Getters et Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public int getTagType() {
        return tagType;
    }

    public void setTagType(int tagType) {
        this.tagType = tagType;
    }
}