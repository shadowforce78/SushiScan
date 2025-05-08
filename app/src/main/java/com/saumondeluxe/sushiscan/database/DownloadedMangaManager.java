package com.saumondeluxe.sushiscan.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DownloadedMangaManager {
    private static final String TAG = "DownloadedMangaManager";
    private static final String PREFS_NAME = "downloaded_mangas_prefs";
    private static final String KEY_DOWNLOADED_MANGAS = "downloaded_mangas";
    
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    
    public DownloadedMangaManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }
    
    /**
     * Ajoute un manga à la liste des téléchargements
     */
    public void addDownloadedManga(MangaEntity manga) {
        List<MangaEntity> downloadedMangas = getDownloadedMangas();
        
        // Vérifie si le manga existe déjà dans la liste
        boolean exists = false;
        for (int i = 0; i < downloadedMangas.size(); i++) {
            if (downloadedMangas.get(i).getName().equals(manga.getName())) {
                // Mise à jour du manga existant
                MangaEntity existingManga = downloadedMangas.get(i);
                existingManga.setImageUrl(manga.getImageUrl());
                existingManga.setScanTypeUrl(manga.getScanTypeUrl());
                existingManga.setDownloaded(true);
                existingManga.setLastReadTimestamp(System.currentTimeMillis());
                downloadedMangas.set(i, existingManga);
                exists = true;
                break;
            }
        }
        
        // Ajoute le manga s'il n'existe pas encore
        if (!exists) {
            manga.setDownloaded(true);
            manga.setLastReadTimestamp(System.currentTimeMillis());
            downloadedMangas.add(manga);
        }
        
        saveDownloadedMangas(downloadedMangas);
        Log.d(TAG, "Manga téléchargé ajouté: " + manga.getName());
    }
    
    /**
     * Retire un manga de la liste des téléchargements
     */
    public void removeDownloadedManga(String mangaName) {
        List<MangaEntity> downloadedMangas = getDownloadedMangas();
        
        for (int i = 0; i < downloadedMangas.size(); i++) {
            if (downloadedMangas.get(i).getName().equals(mangaName)) {
                downloadedMangas.get(i).setDownloaded(false);
                downloadedMangas.remove(i);
                break;
            }
        }
        
        saveDownloadedMangas(downloadedMangas);
        Log.d(TAG, "Manga téléchargé supprimé: " + mangaName);
        
        // Supprimer également les fichiers téléchargés
        deleteDownloadedFiles(mangaName);
    }
    
    /**
     * Récupère la liste des mangas téléchargés
     */
    public List<MangaEntity> getDownloadedMangas() {
        String json = sharedPreferences.getString(KEY_DOWNLOADED_MANGAS, "");
        
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        
        Type type = new TypeToken<List<MangaEntity>>(){}.getType();
        List<MangaEntity> mangas = gson.fromJson(json, type);
        
        return mangas != null ? mangas : new ArrayList<>();
    }
    
    /**
     * Vérifie si un manga est téléchargé
     */
    public boolean isMangaDownloaded(String mangaName) {
        List<MangaEntity> downloadedMangas = getDownloadedMangas();
        
        for (MangaEntity manga : downloadedMangas) {
            if (manga.getName().equals(mangaName)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Enregistre la liste des mangas téléchargés
     */
    private void saveDownloadedMangas(List<MangaEntity> downloadedMangas) {
        String json = gson.toJson(downloadedMangas);
        sharedPreferences.edit().putString(KEY_DOWNLOADED_MANGAS, json).apply();
    }
    
    /**
     * Supprime les fichiers téléchargés d'un manga
     */
    private void deleteDownloadedFiles(String mangaName) {
        // Créer un répertoire sécurisé pour le manga
        String safeMangaName = mangaName.replaceAll("[^a-zA-Z0-9]", "_");
        File mangaDir = new File(context.getExternalFilesDir(null), "downloads/" + safeMangaName);
        
        if (mangaDir.exists() && mangaDir.isDirectory()) {
            deleteRecursive(mangaDir);
            Log.d(TAG, "Fichiers téléchargés supprimés pour: " + mangaName);
        }
    }
    
    /**
     * Supprime récursivement un répertoire et son contenu
     */
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }
    
    /**
     * Vérifie et met à jour la liste des mangas téléchargés en fonction des fichiers présents
     */
    public void rescanDownloadedMangas() {
        File downloadsDir = new File(context.getExternalFilesDir(null), "downloads");
        
        if (!downloadsDir.exists() || !downloadsDir.isDirectory()) {
            return;
        }
        
        List<MangaEntity> downloadedMangas = getDownloadedMangas();
        List<MangaEntity> updatedList = new ArrayList<>();
        
        // Vérifier que chaque manga dans la liste a toujours ses fichiers
        for (MangaEntity manga : downloadedMangas) {
            String safeMangaName = manga.getName().replaceAll("[^a-zA-Z0-9]", "_");
            File mangaDir = new File(downloadsDir, safeMangaName);
            
            if (mangaDir.exists() && mangaDir.isDirectory() && mangaDir.listFiles().length > 0) {
                updatedList.add(manga);
            } else {
                Log.d(TAG, "Manga sans fichiers supprimé: " + manga.getName());
            }
        }
        
        // Mettre à jour la liste si des changements ont été détectés
        if (updatedList.size() != downloadedMangas.size()) {
            saveDownloadedMangas(updatedList);
            Log.d(TAG, "Liste des mangas téléchargés mise à jour après analyse.");
        }
    }
}