package com.saumondeluxe.sushiscan;

import android.util.Log;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.Headers;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class DriveImageLoader {
    private static final String TAG = "DriveImageLoader";

    /**
     * Crée une GlideUrl spécialement formatée pour les liens Google Drive
     * avec des en-têtes qui simulent un navigateur pour éviter les blocages
     */
    public static GlideUrl getGlideUrl(String driveUrl) {
        try {
            // Ajouter des en-têtes de navigateur pour tromper la protection Google Drive
            return new GlideUrl(driveUrl, new Headers() {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36");
                    headers.put("Accept",
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
                    headers.put("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7");
                    headers.put("Sec-Fetch-Dest", "document");
                    headers.put("Sec-Fetch-Mode", "navigate");
                    headers.put("Sec-Fetch-Site", "none");
                    headers.put("Sec-Fetch-User", "?1");
                    return headers;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la création de GlideUrl: " + e.getMessage());
            return new GlideUrl(driveUrl);
        }
    }

    /**
     * Convertit les URLs Google Drive en URLs haute qualité qui fonctionnent mieux
     * avec les applications mobiles.
     */
    public static String getAlternativeUrl(String driveUrl) {
        try {
            if (driveUrl.contains("drive.google.com") && driveUrl.contains("id=")) {
                // Extraire l'ID du fichier
                String fileId = driveUrl.substring(driveUrl.indexOf("id=") + 3);
                if (fileId.contains("&")) {
                    fileId = fileId.substring(0, fileId.indexOf("&"));
                }

                // URL alternative utilisant une résolution plus élevée
                // sz=w0 signifie pas de redimensionnement, obtenir la taille originale
                return "https://drive.google.com/uc?export=download&id=" + fileId;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la conversion d'URL: " + e.getMessage());
        }
        return driveUrl;
    }

    /**
     * Troisième méthode pour obtenir une image en pleine qualité si les deux
     * premières échouent
     */
    public static String getHighQualityUrl(String driveUrl) {
        try {
            if (driveUrl.contains("drive.google.com") && driveUrl.contains("id=")) {
                // Extraire l'ID du fichier
                String fileId = driveUrl.substring(driveUrl.indexOf("id=") + 3);
                if (fileId.contains("&")) {
                    fileId = fileId.substring(0, fileId.indexOf("&"));
                }

                // URL haute qualité - parfois plus lente mais meilleure qualité
                return "https://drive.google.com/thumbnail?id=" + fileId + "&sz=w2000-h2000";
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la création de l'URL haute qualité: " + e.getMessage());
        }
        return driveUrl;
    }
}