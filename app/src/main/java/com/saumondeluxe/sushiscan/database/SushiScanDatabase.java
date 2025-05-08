package com.saumondeluxe.sushiscan.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Base de données principale de l'application
 */
@Database(entities = {
        MangaEntity.class,
        ChapterEntity.class,
        PageImageEntity.class,
        UserPreferencesEntity.class,
        MangaTagEntity.class,
        MangaTagJoinEntity.class }, version = 1, exportSchema = false)
@TypeConverters({ DateConverter.class, Converters.class })
public abstract class SushiScanDatabase extends RoomDatabase {

    // DAOs
    public abstract MangaDao mangaDao();

    public abstract ChapterDao chapterDao();

    public abstract PageImageDao pageImageDao();

    public abstract UserPreferencesDao userPreferencesDao();

    public abstract MangaTagDao mangaTagDao();

    public abstract MangaTagJoinDao mangaTagJoinDao();

    // Instance unique (Singleton)
    private static volatile SushiScanDatabase INSTANCE;
    private static final String DATABASE_NAME = "sushiscan_db";
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static SushiScanDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (SushiScanDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            SushiScanDatabase.class,
                            DATABASE_NAME)
                            .addCallback(sRoomDatabaseCallback)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Callback pour les opérations d'initialisation de la base de données
     */
    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            databaseWriteExecutor.execute(() -> {
                // Initialiser la base de données avec des valeurs par défaut si nécessaire
                SushiScanDatabase database = INSTANCE;
                if (database != null) {
                    // Initialiser les tags par défaut
                    MangaTagDao tagDao = database.mangaTagDao();

                    // Tags de genre
                    tagDao.insertTag(new MangaTagEntity("Action", MangaTagEntity.TAG_TYPE_GENRE));
                    tagDao.insertTag(new MangaTagEntity("Aventure", MangaTagEntity.TAG_TYPE_GENRE));
                    tagDao.insertTag(new MangaTagEntity("Comédie", MangaTagEntity.TAG_TYPE_GENRE));
                    tagDao.insertTag(new MangaTagEntity("Drame", MangaTagEntity.TAG_TYPE_GENRE));
                    tagDao.insertTag(new MangaTagEntity("Fantasy", MangaTagEntity.TAG_TYPE_GENRE));
                    tagDao.insertTag(new MangaTagEntity("Horreur", MangaTagEntity.TAG_TYPE_GENRE));
                    tagDao.insertTag(new MangaTagEntity("Mystère", MangaTagEntity.TAG_TYPE_GENRE));
                    tagDao.insertTag(new MangaTagEntity("Romance", MangaTagEntity.TAG_TYPE_GENRE));
                    tagDao.insertTag(new MangaTagEntity("Science-fiction", MangaTagEntity.TAG_TYPE_GENRE));
                    tagDao.insertTag(new MangaTagEntity("Slice of Life", MangaTagEntity.TAG_TYPE_GENRE));
                    tagDao.insertTag(new MangaTagEntity("Sport", MangaTagEntity.TAG_TYPE_GENRE));
                    tagDao.insertTag(new MangaTagEntity("Surnaturel", MangaTagEntity.TAG_TYPE_GENRE));

                    // Tags d'état
                    tagDao.insertTag(new MangaTagEntity("En cours", MangaTagEntity.TAG_TYPE_STATUS));
                    tagDao.insertTag(new MangaTagEntity("Terminé", MangaTagEntity.TAG_TYPE_STATUS));
                    tagDao.insertTag(new MangaTagEntity("Abandonné", MangaTagEntity.TAG_TYPE_STATUS));

                    // Initialiser les préférences utilisateur par défaut
                    UserPreferencesDao preferencesDao = database.userPreferencesDao();
                    UserPreferencesEntity defaultPreferences = new UserPreferencesEntity();
                    defaultPreferences.setId(1); // ID unique pour les préférences
                    defaultPreferences.setDarkMode(false);
                    defaultPreferences.setAutoDownloadChapters(false);
                    defaultPreferences.setShowNSFWContent(false);
                    defaultPreferences.setMaxConcurrentDownloads(2);
                    defaultPreferences.setNotificationsEnabled(true);
                    preferencesDao.insertPreferences(defaultPreferences);
                }
            });
        }
    };
}