package com.saumondeluxe.sushiscan.download;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.request.FutureTarget;
import com.saumondeluxe.sushiscan.DriveImageLoader;
import com.saumondeluxe.sushiscan.database.ChapterDao;
import com.saumondeluxe.sushiscan.database.ChapterEntity;
import com.saumondeluxe.sushiscan.database.PageImageDao;
import com.saumondeluxe.sushiscan.database.PageImageEntity;
import com.saumondeluxe.sushiscan.database.SushiScanDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Classe responsable du téléchargement et du stockage local des chapitres
 */
public class ChapterDownloadManager {
    private static final String TAG = "ChapterDownloadManager";
    private static final int MAX_CONCURRENT_DOWNLOADS = 2;
    private static ChapterDownloadManager instance;

    private static volatile ChapterDownloadManager INSTANCE;

    private final Context context;
    private final SushiScanDatabase database;
    private final ChapterDao chapterDao;
    private final PageImageDao pageImageDao;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final OkHttpClient client;

    // Interface d'écouteur pour les progrès de téléchargement
    public interface DownloadProgressListener {
        void onProgressUpdate(int current, int total);

        void onDownloadComplete(boolean success);
    }

    private ChapterDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = SushiScanDatabase.getInstance(context);
        this.chapterDao = database.chapterDao();
        this.pageImageDao = database.pageImageDao();
        this.executor = Executors.newFixedThreadPool(MAX_CONCURRENT_DOWNLOADS);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }

    public static ChapterDownloadManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ChapterDownloadManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ChapterDownloadManager(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Télécharge toutes les images d'un chapitre et les enregistre localement
     * 
     * @param chapter   Entité de chapitre à télécharger
     * @param imageUrls Liste des URLs des images du chapitre
     * @param listener  Écouteur pour suivre la progression
     */
    public void downloadChapter(ChapterEntity chapter, List<String> imageUrls, DownloadProgressListener listener) {
        if (chapter == null || imageUrls == null || imageUrls.isEmpty()) {
            if (listener != null) {
                mainHandler.post(() -> listener.onDownloadComplete(false));
            }
            return;
        }

        executor.execute(() -> {
            try {
                // Créer le répertoire de destination pour ce chapitre
                File chaptersDir = new File(context.getFilesDir(), "chapters");
                if (!chaptersDir.exists() && !chaptersDir.mkdirs()) {
                    Log.e(TAG, "Impossible de créer le répertoire de chapitres");
                    notifyDownloadComplete(listener, false);
                    return;
                }

                // Créer un sous-répertoire spécifique pour ce chapitre
                String mangaDir = sanitizeFileName(chapter.getMangaName());
                String chapterDir = "chapter_" + chapter.getNumber();

                File mangaFolder = new File(chaptersDir, mangaDir);
                if (!mangaFolder.exists() && !mangaFolder.mkdirs()) {
                    Log.e(TAG, "Impossible de créer le répertoire du manga");
                    notifyDownloadComplete(listener, false);
                    return;
                }

                File chapterFolder = new File(mangaFolder, chapterDir);
                if (!chapterFolder.exists() && !chapterFolder.mkdirs()) {
                    Log.e(TAG, "Impossible de créer le répertoire du chapitre");
                    notifyDownloadComplete(listener, false);
                    return;
                }

                // Enregistrer les informations des pages dans la base de données
                List<PageImageEntity> pages = new ArrayList<>();
                for (int i = 0; i < imageUrls.size(); i++) {
                    PageImageEntity page = new PageImageEntity();
                    page.setChapterId(chapter.getId());
                    page.setPageNumber(i);
                    page.setOriginalUrl(imageUrls.get(i));
                    page.setDownloaded(false);

                    long pageId = pageImageDao.insertPage(page);
                    page.setId(pageId);
                    pages.add(page);
                }

                // Télécharger chaque image
                int totalPages = imageUrls.size();
                int successCount = 0;

                for (int i = 0; i < totalPages; i++) {
                    final int pageIndex = i;
                    final PageImageEntity page = pages.get(i);

                    // Notifier la progression
                    notifyProgressUpdate(listener, i, totalPages);

                    try {
                        // Obtenir l'URL de l'image
                        String imageUrl = imageUrls.get(i);

                        // Télécharger et enregistrer l'image
                        boolean success = downloadAndSaveImage(imageUrl, chapterFolder, page);

                        if (success) {
                            successCount++;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors du téléchargement de l'image " + i + ": " + e.getMessage());
                    }
                }

                // Mettre à jour l'état du chapitre
                boolean allSuccessful = successCount == totalPages;
                chapter.setDownloaded(allSuccessful);
                chapterDao.updateChapter(chapter);

                // Notifier que le téléchargement est terminé
                notifyDownloadComplete(listener, allSuccessful);

            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du téléchargement du chapitre: " + e.getMessage());
                notifyDownloadComplete(listener, false);
            }
        });
    }

    /**
     * Télécharge et enregistre une seule image
     * 
     * @return true si le téléchargement a réussi
     */
    private boolean downloadAndSaveImage(String imageUrl, File chapterFolder, PageImageEntity page) {
        try {
            // Vérifier d'abord si l'image est déjà dans le cache Glide
            GlideUrl glideUrl = DriveImageLoader.getGlideUrl(imageUrl);
            String alternativeUrl = DriveImageLoader.getAlternativeUrl(imageUrl);
            String highQualityUrl = DriveImageLoader.getHighQualityUrl(imageUrl);

            // Essayer de télécharger avec toutes les méthodes possibles
            Bitmap bitmap = null;

            try {
                FutureTarget<Bitmap> target = Glide.with(context)
                        .asBitmap()
                        .load(glideUrl)
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .submit();
                bitmap = target.get();
            } catch (ExecutionException e) {
                Log.e(TAG, "Impossible de télécharger l'image avec la première URL: " + e.getMessage());
            }

            // Essayer l'URL alternative si la première a échoué
            if (bitmap == null) {
                try {
                    FutureTarget<Bitmap> target = Glide.with(context)
                            .asBitmap()
                            .load(alternativeUrl)
                            .diskCacheStrategy(DiskCacheStrategy.DATA)
                            .submit();
                    bitmap = target.get();
                } catch (ExecutionException e) {
                    Log.e(TAG, "Impossible de télécharger l'image avec l'URL alternative: " + e.getMessage());
                }
            }

            // Essayer l'URL haute qualité si les autres ont échoué
            if (bitmap == null) {
                try {
                    FutureTarget<Bitmap> target = Glide.with(context)
                            .asBitmap()
                            .load(highQualityUrl)
                            .diskCacheStrategy(DiskCacheStrategy.DATA)
                            .submit();
                    bitmap = target.get();
                } catch (ExecutionException e) {
                    Log.e(TAG, "Impossible de télécharger l'image avec l'URL haute qualité: " + e.getMessage());
                }
            }

            // Si on a réussi à obtenir l'image
            if (bitmap != null) {
                // Générer un nom de fichier pour cette image
                String fileName = "page_" + page.getPageNumber() + ".jpg";
                File outputFile = new File(chapterFolder, fileName);

                // Enregistrer le bitmap dans un fichier
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(outputFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                    fos.flush();

                    // Mettre à jour la base de données
                    page.setLocalImagePath(outputFile.getAbsolutePath());
                    page.setDownloaded(true);
                    page.setDownloadTime(System.currentTimeMillis());
                    pageImageDao.updatePage(page);

                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "Erreur lors de l'enregistrement de l'image: " + e.getMessage());
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            // Ignorer
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur générale lors du téléchargement: " + e.getMessage(), e);
        }

        return false;
    }

    /**
     * Supprime un chapitre téléchargé
     */
    public void deleteDownloadedChapter(ChapterEntity chapter, Runnable onComplete) {
        if (chapter == null || !chapter.isDownloaded()) {
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
            return;
        }

        executor.execute(() -> {
            try {
                // Supprimer les fichiers du chapitre
                File chaptersDir = new File(context.getFilesDir(), "chapters");
                String mangaDir = sanitizeFileName(chapter.getMangaName());
                String chapterDir = "chapter_" + chapter.getNumber();

                File chapterFolder = new File(new File(chaptersDir, mangaDir), chapterDir);

                if (chapterFolder.exists()) {
                    deleteRecursive(chapterFolder);
                }

                // Mettre à jour l'état de téléchargement dans la base de données
                chapter.setDownloaded(false);
                chapterDao.updateChapter(chapter);

                // Mettre à jour les entités de page
                List<PageImageEntity> pages = pageImageDao.getPagesByChapterSync(chapter.getId());
                for (PageImageEntity page : pages) {
                    page.setDownloaded(false);
                    page.setLocalImagePath(null);
                    pageImageDao.updatePage(page);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la suppression du chapitre: " + e.getMessage());
            } finally {
                if (onComplete != null) {
                    mainHandler.post(onComplete);
                }
            }
        });
    }

    /**
     * Supprime récursivement un dossier et son contenu
     */
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    /**
     * Notifie la progression du téléchargement
     */
    private void notifyProgressUpdate(DownloadProgressListener listener, int current, int total) {
        if (listener != null) {
            mainHandler.post(() -> listener.onProgressUpdate(current, total));
        }
    }

    /**
     * Notifie la fin du téléchargement
     */
    private void notifyDownloadComplete(DownloadProgressListener listener, boolean success) {
        if (listener != null) {
            mainHandler.post(() -> listener.onDownloadComplete(success));
        }
    }

    /**
     * Sanitize le nom de fichier pour qu'il soit valide dans le système de fichiers
     */
    @NonNull
    private String sanitizeFileName(String input) {
        if (input == null) {
            return "unknown";
        }
        return input.replaceAll("[^a-zA-Z0-9.-]", "_").toLowerCase();
    }

    /**
     * Télécharge une seule image
     */
    private void downloadImage(String imageUrl, String localPath) throws IOException {
        Request request = new Request.Builder()
                .url(imageUrl)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Réponse du serveur incorrecte: " + response.code());
            }

            // Enregistrer l'image sur le disque
            try (InputStream inputStream = response.body().byteStream();
                 FileOutputStream outputStream = new FileOutputStream(localPath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        }
    }

    /**
     * Nettoie un nom de fichier pour qu'il soit valide sur le système de fichiers
     */
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}