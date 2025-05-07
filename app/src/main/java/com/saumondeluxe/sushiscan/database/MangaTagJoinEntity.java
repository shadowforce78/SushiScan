package com.saumondeluxe.sushiscan.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;

/**
 * Table de jonction pour la relation many-to-many entre Manga et Tags
 */
@Entity(tableName = "manga_tag_join", primaryKeys = { "mangaId", "tagId" }, foreignKeys = {
        @ForeignKey(entity = MangaEntity.class, parentColumns = "id", childColumns = "mangaId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = MangaTagEntity.class, parentColumns = "id", childColumns = "tagId", onDelete = ForeignKey.CASCADE)
}, indices = {
        @Index("mangaId"),
        @Index("tagId")
})
public class MangaTagJoinEntity {

    private long mangaId;
    private long tagId;

    public MangaTagJoinEntity() {
        // Constructeur vide requis par Room
    }

    @Ignore
    public MangaTagJoinEntity(long mangaId, long tagId) {
        this.mangaId = mangaId;
        this.tagId = tagId;
    }

    public long getMangaId() {
        return mangaId;
    }

    public void setMangaId(long mangaId) {
        this.mangaId = mangaId;
    }

    public long getTagId() {
        return tagId;
    }

    public void setTagId(long tagId) {
        this.tagId = tagId;
    }
}