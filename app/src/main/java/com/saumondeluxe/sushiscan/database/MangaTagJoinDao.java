package com.saumondeluxe.sushiscan.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RewriteQueriesToDropUnusedColumns;
import androidx.room.Transaction;

import java.util.List;

/**
 * DAO pour gérer la relation many-to-many entre Mangas et Tags
 */
@Dao
public interface MangaTagJoinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MangaTagJoinEntity join);

    @Delete
    void delete(MangaTagJoinEntity join);

    @Query("DELETE FROM manga_tag_join WHERE mangaId = :mangaId")
    void deleteAllTagsForManga(long mangaId);

    @Query("DELETE FROM manga_tag_join WHERE mangaId = :mangaId AND tagId = :tagId")
    void deleteTagFromManga(long mangaId, long tagId);

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM manga_tags " +
            "INNER JOIN manga_tag_join ON manga_tags.id = manga_tag_join.tagId " +
            "WHERE manga_tag_join.mangaId = :mangaId")
    LiveData<List<MangaTagEntity>> getTagsForManga(long mangaId);

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM manga_tags " +
            "INNER JOIN manga_tag_join ON manga_tags.id = manga_tag_join.tagId " +
            "WHERE manga_tag_join.mangaId = :mangaId AND manga_tags.tagType = :tagType")
    LiveData<List<MangaTagEntity>> getTagsForMangaByType(long mangaId, int tagType);

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM mangas " +
            "INNER JOIN manga_tag_join ON mangas.id = manga_tag_join.mangaId " +
            "WHERE manga_tag_join.tagId = :tagId")
    LiveData<List<MangaEntity>> getMangasWithTag(long tagId);

    @Transaction
    @Query("SELECT EXISTS(SELECT 1 FROM manga_tag_join WHERE mangaId = :mangaId AND tagId = :tagId)")
    boolean hasMangaTag(long mangaId, long tagId);
}