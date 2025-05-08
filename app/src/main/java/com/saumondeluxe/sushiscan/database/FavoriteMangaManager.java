package com.saumondeluxe.sushiscan.database;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Gestionnaire pour les mangas favoris
 */
public class FavoriteMangaManager {
    private final SushiScanDatabase database;
    private final MangaDao mangaDao;
    private final Executor executor;

    public FavoriteMangaManager(Context context) {
        this.database = SushiScanDatabase.getInstance(context);
        this.mangaDao = database.mangaDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Récupère la liste des mangas favoris
     */
    public List<MangaEntity> getFavoriteMangas() {
        List<MangaEntity> favorites = mangaDao.getFavoriteMangasSync();
        return favorites != null ? favorites : new ArrayList<>();
    }

    /**
     * Ajoute ou retire un manga des favoris
     */
    public void toggleFavorite(MangaEntity manga, Runnable onComplete) {
        if (manga == null) return;

        executor.execute(() -> {
            // Inverser l'état des favoris
            manga.setFavorite(!manga.isFavorite());
            
            // Mettre à jour dans la base de données
            mangaDao.updateManga(manga);
            
            // Exécuter le callback si fourni
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
    
    /**
     * Vérifie si un manga est dans les favoris
     */
    public boolean isFavorite(String mangaName) {
        MangaEntity manga = mangaDao.getMangaByName(mangaName);
        return manga != null && manga.isFavorite();
    }
}