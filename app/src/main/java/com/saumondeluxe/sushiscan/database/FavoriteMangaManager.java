package com.saumondeluxe.sushiscan.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FavoriteMangaManager {
    private static final String TAG = "FavoriteMangaManager";
    private static final String PREFS_NAME = "favorite_mangas_prefs";
    private static final String KEY_FAVORITE_MANGAS = "favorite_mangas";
    
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    
    public FavoriteMangaManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }
    
    /**
     * Ajoute un manga aux favoris
     */
    public void addFavoriteManga(MangaEntity manga) {
        List<MangaEntity> favoriteMangas = getFavoriteMangas();
        
        // Vérifie si le manga existe déjà dans la liste
        boolean exists = false;
        for (int i = 0; i < favoriteMangas.size(); i++) {
            if (favoriteMangas.get(i).getName().equals(manga.getName())) {
                // Mise à jour du manga existant
                MangaEntity existingManga = favoriteMangas.get(i);
                existingManga.setImageUrl(manga.getImageUrl());
                existingManga.setGenres(manga.getGenres());
                existingManga.setDescription(manga.getDescription());
                existingManga.setFavorite(true);
                favoriteMangas.set(i, existingManga);
                exists = true;
                break;
            }
        }
        
        // Ajoute le manga s'il n'existe pas encore
        if (!exists) {
            manga.setFavorite(true);
            favoriteMangas.add(manga);
        }
        
        saveFavoriteMangas(favoriteMangas);
        Log.d(TAG, "Manga favori ajouté: " + manga.getName());
    }
    
    /**
     * Retire un manga des favoris
     */
    public void removeFavoriteManga(String mangaName) {
        List<MangaEntity> favoriteMangas = getFavoriteMangas();
        
        for (int i = 0; i < favoriteMangas.size(); i++) {
            if (favoriteMangas.get(i).getName().equals(mangaName)) {
                favoriteMangas.get(i).setFavorite(false);
                favoriteMangas.remove(i);
                break;
            }
        }
        
        saveFavoriteMangas(favoriteMangas);
        Log.d(TAG, "Manga favori supprimé: " + mangaName);
    }
    
    /**
     * Récupère la liste des mangas favoris
     */
    public List<MangaEntity> getFavoriteMangas() {
        String json = sharedPreferences.getString(KEY_FAVORITE_MANGAS, "");
        
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        
        Type type = new TypeToken<List<MangaEntity>>(){}.getType();
        List<MangaEntity> mangas = gson.fromJson(json, type);
        
        return mangas != null ? mangas : new ArrayList<>();
    }
    
    /**
     * Vérifie si un manga est en favori
     */
    public boolean isMangaFavorite(String mangaName) {
        List<MangaEntity> favoriteMangas = getFavoriteMangas();
        
        for (MangaEntity manga : favoriteMangas) {
            if (manga.getName().equals(mangaName)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Bascule l'état favori d'un manga (ajoute ou supprime)
     * 
     * @return le nouvel état (true = favori, false = non favori)
     */
    public boolean toggleFavorite(MangaEntity manga) {
        if (isMangaFavorite(manga.getName())) {
            removeFavoriteManga(manga.getName());
            return false;
        } else {
            addFavoriteManga(manga);
            return true;
        }
    }
    
    /**
     * Enregistre la liste des mangas favoris
     */
    private void saveFavoriteMangas(List<MangaEntity> favoriteMangas) {
        String json = gson.toJson(favoriteMangas);
        sharedPreferences.edit().putString(KEY_FAVORITE_MANGAS, json).apply();
    }
}