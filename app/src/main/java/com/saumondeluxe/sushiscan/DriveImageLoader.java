package com.saumondeluxe.sushiscan;

import android.util.Log;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.Headers;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitaire pour charger des images depuis Google Drive ou d'autres sources
 */
public class DriveImageLoader {
    private static final String TAG = "DriveImageLoader";

    /**
     * Crée une URL compatible avec Glide pour Google Drive et d'autres sources
     * @param url URL de l'image
     * @return GlideUrl configurée avec les en-têtes appropriés
     */
    public static GlideUrl getGlideUrl(String url) {
        // Ajouter des en-têtes personnalisés pour éviter les problèmes de cache et de redirection
        return new GlideUrl(url, new Headers() {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36");
                return headers;
            }
        });
    }

    /**
     * Génère une URL alternative pour les cas où l'URL principale ne fonctionne pas
     * @param url URL originale
     * @return URL alternative
     */
    public static String getAlternativeUrl(String url) {
        // Pour les URLs de Google Drive, essayer un format alternatif
        if (url.contains("drive.google.com")) {
            // Remplacer le format de visualisation par un format de téléchargement
            return url.replace("view?usp=sharing", "preview");
        }
        return url;
    }

    /**
     * Génère une URL de haute qualité si possible
     * @param url URL originale
     * @return URL en haute qualité
     */
    public static String getHighQualityUrl(String url) {
        // Tenter d'obtenir une version de l'image en haute qualité
        if (url.contains("=w")) {
            // Augmenter la largeur pour les images redimensionnées
            return url.replaceAll("=w\\d+", "=w2000");
        }
        return url;
    }
}