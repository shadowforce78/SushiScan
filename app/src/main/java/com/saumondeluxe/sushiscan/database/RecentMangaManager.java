package com.saumondeluxe.sushiscan.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RecentMangaManager {
    private static final String TAG = "RecentMangaManager";
    private static final String PREFS_NAME = "recent_mangas_prefs";
    private static final String KEY_RECENT_MANGAS = "recent_mangas";
    
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    
    public RecentMangaManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }
    
    /**
     * Ajoute ou met à jour un manga dans la liste des récemment lus
     */
    public void addRecentlyReadManga(MangaEntity manga, int chapterNumber) {
        List<MangaEntity> recentMangas = getRecentMangas();
        
        // Vérifier si le manga existe déjà dans la liste
        boolean exists = false;
        for (int i = 0; i < recentMangas.size(); i++) {
            if (recentMangas.get(i).getName().equals(manga.getName())) {
                // Mise à jour du manga existant
                MangaEntity existingManga = recentMangas.get(i);
                existingManga.setImageUrl(manga.getImageUrl());
                existingManga.setLastReadChapterNumber(chapterNumber);
                existingManga.setLastReadTimestamp(System.currentTimeMillis());
                recentMangas.set(i, existingManga);
                exists = true;
                break;
            }
        }
        
        // Ajouter le manga s'il n'existe pas encore
        if (!exists) {
            manga.setLastReadChapterNumber(chapterNumber);
            manga.setLastReadTimestamp(System.currentTimeMillis());
            recentMangas.add(manga);
        }
        
        // Trier par date de lecture (plus récent en premier)
        sortByMostRecent(recentMangas);
        
        // Limiter la liste à 50 mangas maximum
        if (recentMangas.size() > 50) {
            recentMangas = recentMangas.subList(0, 50);
        }
        
        saveRecentMangas(recentMangas);
        Log.d(TAG, "Manga récemment lu ajouté/mis à jour: " + manga.getName() + " (Chapitre " + chapterNumber + ")");
    }
    
    /**
     * Récupère tous les mangas récemment lus
     */
    public List<MangaEntity> getRecentMangas() {
        String json = sharedPreferences.getString(KEY_RECENT_MANGAS, "");
        
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        
        Type type = new TypeToken<List<MangaEntity>>(){}.getType();
        List<MangaEntity> mangas = gson.fromJson(json, type);
        
        return mangas != null ? mangas : new ArrayList<>();
    }
    
    /**
     * Récupère un nombre limité de mangas récemment lus
     */
    public List<MangaEntity> getRecentlyReadMangas(int limit) {
        List<MangaEntity> recentMangas = getRecentMangas();
        
        // Trier par date de lecture (plus récent en premier)
        sortByMostRecent(recentMangas);
        
        // Limiter la liste au nombre demandé
        if (recentMangas.size() > limit) {
            return recentMangas.subList(0, limit);
        } else {
            return recentMangas;
        }
    }
    
    /**
     * Trie la liste des mangas par date de lecture (plus récent en premier)
     */
    private void sortByMostRecent(List<MangaEntity> mangas) {
        Collections.sort(mangas, new Comparator<MangaEntity>() {
            @Override
            public int compare(MangaEntity manga1, MangaEntity manga2) {
                // Tri par ordre décroissant de timestamp
                return Long.compare(manga2.getLastReadTimestamp(), manga1.getLastReadTimestamp());
            }
        });
    }
    
    /**
     * Enregistre la liste des mangas récemment lus
     */
    private void saveRecentMangas(List<MangaEntity> recentMangas) {
        String json = gson.toJson(recentMangas);
        sharedPreferences.edit().putString(KEY_RECENT_MANGAS, json).apply();
    }
}