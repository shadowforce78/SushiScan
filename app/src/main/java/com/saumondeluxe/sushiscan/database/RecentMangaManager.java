package com.saumondeluxe.sushiscan.database;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Gestionnaire pour les mangas récemment lus
 */
public class RecentMangaManager {
    private final SushiScanDatabase database;
    private final MangaDao mangaDao;
    private final Executor executor;

    public RecentMangaManager(Context context) {
        this.database = SushiScanDatabase.getInstance(context);
        this.mangaDao = database.mangaDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Récupère la liste des mangas récemment lus
     * 
     * @param limit Nombre maximum de mangas à récupérer
     * @return Liste des mangas récemment lus
     */
    public List<MangaEntity> getRecentlyReadMangas(int limit) {
        List<MangaEntity> recentMangas = mangaDao.getRecentlyReadMangasSync(limit);
        return recentMangas != null ? recentMangas : new ArrayList<>();
    }

    /**
     * Met à jour la date de dernière lecture d'un manga
     */
    public void updateLastReadTime(long mangaId) {
        executor.execute(() -> {
            mangaDao.updateLastReadTime(mangaId, System.currentTimeMillis());
        });
    }

    /**
     * Met à jour la date de dernière lecture d'un manga
     */
    public void updateLastReadTime(String mangaName) {
        executor.execute(() -> {
            MangaEntity manga = mangaDao.getMangaByName(mangaName);
            if (manga != null) {
                manga.setLastReadTime(System.currentTimeMillis());
                mangaDao.updateManga(manga);
            }
        });
    }
}