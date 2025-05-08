package com.saumondeluxe.sushiscan.database;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Gestionnaire pour les mangas téléchargés
 */
public class DownloadedMangaManager {
    private static final String TAG = "DownloadedMangaManager";
    
    private final Context context;
    private final SushiScanDatabase database;
    private final MangaDao mangaDao;
    private final ChapterDao chapterDao;
    private final Executor executor;

    public DownloadedMangaManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = SushiScanDatabase.getInstance(context);
        this.mangaDao = database.mangaDao();
        this.chapterDao = database.chapterDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Récupère la liste des mangas téléchargés
     */
    public List<MangaEntity> getDownloadedMangas() {
        // Chercher les mangas qui ont au moins un chapitre téléchargé
        List<MangaEntity> downloadedMangas = mangaDao.getDownloadedMangasSync();
        return downloadedMangas != null ? downloadedMangas : new ArrayList<>();
    }

    /**
     * Rescan les mangas téléchargés pour mettre à jour la base de données
     */
    public void rescanDownloadedMangas() {
        executor.execute(() -> {
            // Répertoire principal où sont stockés les chapitres téléchargés
            File chaptersDir = new File(context.getFilesDir(), "chapters");
            
            if (!chaptersDir.exists()) {
                return;
            }
            
            // Parcourir les dossiers de manga
            File[] mangaDirs = chaptersDir.listFiles();
            if (mangaDirs != null) {
                for (File mangaDir : mangaDirs) {
                    if (mangaDir.isDirectory()) {
                        String mangaName = desanitizeFileName(mangaDir.getName());
                        
                        // Vérifier si ce manga existe déjà dans la base de données
                        MangaEntity manga = mangaDao.getMangaByName(mangaName);
                        
                        // Si le manga n'existe pas, le créer
                        if (manga == null) {
                            manga = new MangaEntity();
                            manga.setName(mangaName);
                            manga.setDownloaded(true);
                            long mangaId = mangaDao.insertManga(manga);
                            manga.setId(mangaId);
                        } else {
                            manga.setDownloaded(true);
                            mangaDao.updateManga(manga);
                        }
                        
                        // Parcourir les dossiers de chapitres de ce manga
                        File[] chapterDirs = mangaDir.listFiles();
                        if (chapterDirs != null) {
                            for (File chapterDir : chapterDirs) {
                                if (chapterDir.isDirectory() && chapterDir.getName().startsWith("chapter_")) {
                                    // Extraire le numéro du chapitre
                                    String chapterNumberStr = chapterDir.getName().substring("chapter_".length());
                                    try {
                                        int chapterNumber = Integer.parseInt(chapterNumberStr);
                                        
                                        // Vérifier si ce chapitre existe déjà dans la base de données
                                        ChapterEntity chapter = chapterDao.getChapterByNumber(mangaName, "", chapterNumber);
                                        
                                        // Si le chapitre n'existe pas, le créer
                                        if (chapter == null) {
                                            chapter = new ChapterEntity();
                                            chapter.setName("Chapitre " + chapterNumber);
                                            chapter.setNumber(chapterNumber);
                                            chapter.setMangaName(mangaName);
                                            chapter.setScanType("");
                                            chapter.setDownloaded(true);
                                            long chapterId = chapterDao.insertChapter(chapter);
                                            chapter.setId(chapterId);
                                        } else {
                                            chapter.setDownloaded(true);
                                            chapterDao.updateChapter(chapter);
                                        }
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "Format de numéro de chapitre invalide : " + chapterNumberStr);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Convertit un nom de fichier sanitisé en nom lisible
     */
    private String desanitizeFileName(String sanitizedName) {
        // Cette fonction est une approximation, car on ne peut pas récupérer parfaitement le nom original
        // Remplacer les underscores par des espaces et faire une belle mise en forme
        String readable = sanitizedName.replace('_', ' ');
        return toTitleCase(readable);
    }
    
    /**
     * Convertit une chaîne en Title Case (première lettre de chaque mot en majuscule)
     */
    private String toTitleCase(String input) {
        StringBuilder titleCase = new StringBuilder(input.length());
        boolean nextTitleCase = true;
        
        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            } else {
                c = Character.toLowerCase(c);
            }
            
            titleCase.append(c);
        }
        
        return titleCase.toString();
    }
}