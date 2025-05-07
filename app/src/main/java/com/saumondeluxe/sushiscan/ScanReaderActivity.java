package com.saumondeluxe.sushiscan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.saumondeluxe.sushiscan.database.ChapterDao;
import com.saumondeluxe.sushiscan.database.ChapterEntity;
import com.saumondeluxe.sushiscan.database.MangaDao;
import com.saumondeluxe.sushiscan.database.MangaEntity;
import com.saumondeluxe.sushiscan.database.PageImageDao;
import com.saumondeluxe.sushiscan.database.PageImageEntity;
import com.saumondeluxe.sushiscan.database.SushiScanDatabase;
import com.saumondeluxe.sushiscan.download.ChapterDownloadManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ScanReaderActivity extends AppCompatActivity implements SequentialImageLoader.LoadingStateListener,
        ChapterDownloadManager.DownloadProgressListener {

    private RecyclerView scanRecyclerView;
    private ProgressBar loadingProgressBar;
    private ProgressBar downloadProgressBar;
    private TextView errorTextView, titleTextView;
    private Button prevButton, nextButton, backButton;
    private ImageButton downloadButton;
    private Spinner chapterSpinner;
    private OkHttpClient client;

    // Utilisation d'une liste pour stocker les chapitres
    private List<Chapter> chapters;
    private ScanAdapter adapter;
    private String nameForInfo;
    private String scanType;
    private int currentChapterIndex = 0;

    // Gestionnaire de chargement séquentiel d'images
    private SequentialImageLoader imageLoader;

    // Pour suivre l'élément visible
    private int lastVisibleItemPosition = 0;

    // Base de données et téléchargement
    private SushiScanDatabase database;
    private ChapterDao chapterDao;
    private PageImageDao pageImageDao;
    private MangaDao mangaDao;
    private ChapterDownloadManager downloadManager;
    private Executor dbExecutor;

    // Chapitre actuellement affiché
    private ChapterEntity currentChapterEntity;

    // Manga actuellement affiché
    private MangaEntity currentMangaEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_reader);

        // Initialiser les vues
        scanRecyclerView = findViewById(R.id.scanRecyclerView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);
        errorTextView = findViewById(R.id.errorTextView);
        titleTextView = findViewById(R.id.titleTextView);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        backButton = findViewById(R.id.backButton);
        chapterSpinner = findViewById(R.id.chapterSpinner);
        downloadButton = findViewById(R.id.downloadButton);

        // Initialiser la base de données et les DAOs
        database = SushiScanDatabase.getInstance(this);
        chapterDao = database.chapterDao();
        pageImageDao = database.pageImageDao();
        mangaDao = database.mangaDao();
        dbExecutor = Executors.newSingleThreadExecutor();

        // Initialiser le gestionnaire de téléchargement
        downloadManager = ChapterDownloadManager.getInstance(this);

        // Configurer le bouton de téléchargement
        downloadButton.setOnClickListener(v -> {
            if (currentChapterEntity != null) {
                if (currentChapterEntity.isDownloaded()) {
                    // Si le chapitre est déjà téléchargé, proposer de le supprimer
                    Toast.makeText(this, "Ce chapitre est déjà téléchargé", Toast.LENGTH_SHORT).show();
                    // Ici on pourrait ajouter une boîte de dialogue pour confirmer la suppression
                } else {
                    // Démarrer le téléchargement du chapitre
                    startChapterDownload();
                }
            }
        });

        // Configurer le RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        scanRecyclerView.setLayoutManager(layoutManager);

        // Ajouter un écouteur de défilement pour détecter les éléments visibles
        scanRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // Obtenir l'élément actuellement visible
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                if (firstVisibleItemPosition != RecyclerView.NO_POSITION &&
                        firstVisibleItemPosition != lastVisibleItemPosition) {
                    lastVisibleItemPosition = firstVisibleItemPosition;

                    // Mettre à jour le chargeur pour prioriser les images visibles
                    if (imageLoader != null) {
                        imageLoader.updateVisiblePosition(firstVisibleItemPosition);
                    }

                    // Sauvegarder la progression de lecture
                    if (currentChapterEntity != null && chapters.size() > currentChapterIndex) {
                        saveReadingProgress(firstVisibleItemPosition);
                    }
                }
            }
        });

        // Récupérer les données de l'intent
        if (getIntent() != null) {
            nameForInfo = getIntent().getStringExtra("nameForInfo");
            scanType = getIntent().getStringExtra("scanUrl");

            // Définir le titre
            titleTextView.setText(nameForInfo);
        }

        // Initialiser le client HTTP
        client = new OkHttpClient();

        // Initialiser les collections
        chapters = new ArrayList<>();

        // Initialiser l'adaptateur avec une liste vide
        adapter = new ScanAdapter(new ArrayList<>());
        scanRecyclerView.setAdapter(adapter);

        // Configurer le Spinner pour la sélection des chapitres
        chapterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentChapterIndex = position;
                loadChapter(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ne rien faire
            }
        });

        // Configurer les boutons de navigation entre chapitres
        prevButton.setOnClickListener(v -> {
            if (currentChapterIndex > 0) {
                chapterSpinner.setSelection(currentChapterIndex - 1);
            }
        });

        nextButton.setOnClickListener(v -> {
            if (currentChapterIndex < chapters.size() - 1) {
                chapterSpinner.setSelection(currentChapterIndex + 1);
            }
        });

        backButton.setOnClickListener(v -> finish());

        // Charger les scans
        loadScans();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Nettoyer le chargeur d'images
        if (imageLoader != null) {
            imageLoader.removeLoadingStateListener(this);
        }
    }

    @Override
    public void onImageLoaded(int position, boolean success) {
        // Mettre à jour l'adaptateur quand une image est chargée
        if (position >= 0 && position < adapter.getItemCount()) {
            adapter.notifyItemChanged(position);
        }
    }

    @Override
    public void onProgressUpdate(int current, int total) {
        runOnUiThread(() -> {
            downloadProgressBar.setVisibility(View.VISIBLE);
            downloadProgressBar.setMax(total);
            downloadProgressBar.setProgress(current);
        });
    }

    @Override
    public void onDownloadComplete(boolean success) {
        runOnUiThread(() -> {
            downloadProgressBar.setVisibility(View.GONE);
            if (success) {
                Toast.makeText(this, "Téléchargement terminé avec succès", Toast.LENGTH_SHORT).show();
                updateDownloadButtonState(true);

                // Mettre à jour l'état du chapitre dans la base de données
                if (currentChapterEntity != null) {
                    dbExecutor.execute(() -> {
                        currentChapterEntity.setDownloaded(true);
                        chapterDao.updateChapter(currentChapterEntity);
                    });
                }
            } else {
                Toast.makeText(this, "Échec du téléchargement", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Démarre le téléchargement du chapitre actuel
     */
    private void startChapterDownload() {
        if (currentChapterEntity != null && chapters.size() > currentChapterIndex) {
            Chapter chapter = chapters.get(currentChapterIndex);
            downloadProgressBar.setVisibility(View.VISIBLE);
            downloadProgressBar.setProgress(0);
            downloadManager.downloadChapter(currentChapterEntity, chapter.imageUrls, this);
        }
    }

    /**
     * Met à jour l'affichage du bouton de téléchargement
     */
    private void updateDownloadButtonState(boolean isDownloaded) {
        if (isDownloaded) {
            downloadButton.setImageResource(android.R.drawable.ic_menu_delete);
            downloadButton.setContentDescription("Supprimer le chapitre téléchargé");
        } else {
            downloadButton.setImageResource(android.R.drawable.ic_menu_save);
            downloadButton.setContentDescription("Télécharger le chapitre");
        }
    }

    /**
     * Charge les informations des scans depuis l'API
     */
    private void loadScans() {
        // Afficher le chargement
        loadingProgressBar.setVisibility(View.VISIBLE);
        errorTextView.setVisibility(View.GONE);

        // Construire l'URL de l'API pour les scans
        // Format: https://api.saumondeluxe.com/scans/get_scan/{name}/{url}
        String url = "https://api.saumondeluxe.com/scans/get_scan/" + nameForInfo + "/" + scanType;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    errorTextView.setVisibility(View.VISIBLE);
                    errorTextView.setText("Erreur de connexion: " + e.getMessage());

                    // Essayer de charger depuis la base de données locale si disponible
                    loadChaptersFromDatabase();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseData = response.body().string();

                runOnUiThread(() -> {
                    try {
                        // Parser les données JSON
                        JSONObject jsonObject = new JSONObject(responseData);

                        // Vérifier si success est true
                        if (jsonObject.has("success") && jsonObject.getBoolean("success")) {
                            // Nettoyer les listes précédentes
                            chapters.clear();

                            // Vérifier si le JSON est déjà parsé (nouvelle API)
                            if (jsonObject.has("parsed") && jsonObject.getBoolean("parsed")) {
                                // Extraire les chapitres directement du JSON
                                JSONArray chaptersArray = jsonObject.getJSONArray("chapters");

                                for (int i = 0; i < chaptersArray.length(); i++) {
                                    JSONObject chapterObj = chaptersArray.getJSONObject(i);

                                    // Créer un nouveau Chapter
                                    Chapter chapter = new Chapter();
                                    chapter.number = chapterObj.getInt("number");
                                    chapter.name = chapterObj.getString("name");

                                    // Récupérer les images
                                    JSONArray imagesArray = chapterObj.getJSONArray("images");
                                    List<String> imageUrls = new ArrayList<>();

                                    for (int j = 0; j < imagesArray.length(); j++) {
                                        JSONObject imageObj = imagesArray.getJSONObject(j);
                                        imageUrls.add(imageObj.getString("url"));
                                    }

                                    chapter.imageUrls = imageUrls;
                                    chapters.add(chapter);

                                    // Sauvegarder les informations du chapitre dans la base de données
                                    saveChapterToDatabase(chapter);
                                }

                                if (!chapters.isEmpty()) {
                                    setupChapterSpinner();
                                    loadingProgressBar.setVisibility(View.GONE);

                                    // Charger la progression de lecture après avoir chargé les chapitres
                                    loadReadingProgress();
                                } else {
                                    loadingProgressBar.setVisibility(View.GONE);
                                    errorTextView.setVisibility(View.VISIBLE);
                                    errorTextView.setText("Aucun chapitre trouvé");

                                    // Essayer de charger depuis la base de données locale si disponible
                                    loadChaptersFromDatabase();
                                }
                            } else {
                                // Ancienne méthode (par sécurité, mais ne devrait plus être utilisée)
                                loadingProgressBar.setVisibility(View.GONE);
                                errorTextView.setVisibility(View.VISIBLE);
                                errorTextView.setText(
                                        "Format d'API non pris en charge. Veuillez mettre à jour l'application.");

                                // Essayer de charger depuis la base de données locale si disponible
                                loadChaptersFromDatabase();
                            }
                        } else {
                            // Afficher le message d'erreur retourné par l'API
                            String message = jsonObject.has("message") ? jsonObject.getString("message")
                                    : "Erreur lors du chargement des scans";

                            loadingProgressBar.setVisibility(View.GONE);
                            errorTextView.setVisibility(View.VISIBLE);
                            errorTextView.setText(message);

                            // Essayer de charger depuis la base de données locale si disponible
                            loadChaptersFromDatabase();
                        }
                    } catch (JSONException e) {
                        loadingProgressBar.setVisibility(View.GONE);
                        errorTextView.setVisibility(View.VISIBLE);
                        errorTextView.setText("Erreur de parsing JSON: " + e.getMessage());

                        // Essayer de charger depuis la base de données locale si disponible
                        loadChaptersFromDatabase();
                    }
                });
            }
        });
    }

    /**
     * Sauvegarde un chapitre dans la base de données locale
     */
    private void saveChapterToDatabase(Chapter chapter) {
        dbExecutor.execute(() -> {
            // Vérifier si le chapitre existe déjà dans la base de données
            ChapterEntity existingChapter = chapterDao.getChapterByNumber(nameForInfo, scanType, chapter.number);

            if (existingChapter == null) {
                // Créer une nouvelle entité de chapitre
                ChapterEntity newChapter = new ChapterEntity();
                newChapter.setNumber(chapter.number);
                newChapter.setName(chapter.name);
                newChapter.setMangaName(nameForInfo);
                newChapter.setScanType(scanType);
                newChapter.setDownloaded(false);
                newChapter.setLastReadTime(System.currentTimeMillis());

                // Insérer dans la base de données
                long chapterId = chapterDao.insertChapter(newChapter);

                // Ajouter des informations de l'ID à l'objet Chapter
                chapter.databaseId = chapterId;
            } else {
                // Mettre à jour l'horodatage du dernier accès
                existingChapter.setLastReadTime(System.currentTimeMillis());
                chapterDao.updateChapter(existingChapter);

                // Ajouter des informations de l'ID à l'objet Chapter
                chapter.databaseId = existingChapter.getId();
                chapter.isDownloaded = existingChapter.isDownloaded();
            }
        });
    }

    /**
     * Charge les chapitres depuis la base de données locale
     */
    private void loadChaptersFromDatabase() {
        dbExecutor.execute(() -> {
            // Vérifier s'il y a des chapitres téléchargés pour ce manga
            List<ChapterEntity> dbChapters = chapterDao.getChaptersByManga(nameForInfo, scanType).getValue();

            if (dbChapters != null && !dbChapters.isEmpty()) {
                // Convertir les entités de base de données en objets Chapter
                List<Chapter> localChapters = new ArrayList<>();

                for (ChapterEntity entity : dbChapters) {
                    if (entity.isDownloaded()) {
                        // Charger les pages de ce chapitre
                        List<PageImageEntity> pages = pageImageDao.getPagesByChapterSync(entity.getId());
                        if (!pages.isEmpty()) {
                            Chapter chapter = new Chapter();
                            chapter.number = entity.getNumber();
                            chapter.name = entity.getName();
                            chapter.databaseId = entity.getId();
                            chapter.isDownloaded = true;

                            // Charger les chemins locaux des images
                            List<String> localPaths = new ArrayList<>();
                            for (PageImageEntity page : pages) {
                                if (page.isDownloaded() && page.getLocalImagePath() != null) {
                                    localPaths.add("file://" + page.getLocalImagePath());
                                }
                            }

                            if (!localPaths.isEmpty()) {
                                chapter.imageUrls = localPaths;
                                localChapters.add(chapter);
                            }
                        }
                    }
                }

                if (!localChapters.isEmpty()) {
                    // Mettre à jour l'UI avec les chapitres locaux
                    runOnUiThread(() -> {
                        chapters.clear();
                        chapters.addAll(localChapters);
                        setupChapterSpinner();
                        errorTextView.setVisibility(View.GONE);
                        Toast.makeText(this, "Chapitres chargés depuis le stockage local", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        errorTextView.setText("Aucun chapitre téléchargé disponible");
                    });
                }
            }
        });
    }

    /**
     * Configure le Spinner pour sélectionner les chapitres
     */
    private void setupChapterSpinner() {
        // Préparer les données pour le Spinner
        List<String> chaptersDisplay = new ArrayList<>();
        for (Chapter chapter : chapters) {
            String displayText = chapter.name;
            if (chapter.isDownloaded) {
                displayText += " [✓]"; // Indiquer les chapitres téléchargés
            }
            chaptersDisplay.add(displayText);
        }

        // Configurer l'adaptateur du Spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, chaptersDisplay);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chapterSpinner.setAdapter(spinnerAdapter);

        // Charger le premier chapitre par défaut s'il existe
        if (!chapters.isEmpty()) {
            currentChapterIndex = 0;
            chapterSpinner.setSelection(0);
            loadChapter(0);
        }

        // Mettre à jour les états des boutons
        updateButtonStates();
    }

    /**
     * Charge un chapitre spécifique par son index
     */
    private void loadChapter(int index) {
        if (index >= 0 && index < chapters.size()) {
            Chapter chapter = chapters.get(index);

            // Réinitialiser la position du défilement
            lastVisibleItemPosition = 0;

            // Mettre à jour l'adaptateur avec les nouvelles pages
            adapter.updateData(chapter.imageUrls);

            // Initialiser le chargeur d'images séquentiel
            imageLoader = new SequentialImageLoader(this, chapter.imageUrls);
            imageLoader.addLoadingStateListener(this);

            // Démarrer le chargement à partir de la première image
            imageLoader.updateVisiblePosition(0);

            // Faire défiler au début du chapitre
            scanRecyclerView.scrollToPosition(0);

            // Mettre à jour l'état du bouton de téléchargement
            updateDownloadButtonState(chapter.isDownloaded);

            // Mettre à jour l'entité de chapitre actuelle
            dbExecutor.execute(() -> {
                currentChapterEntity = chapterDao.getChapterByNumber(nameForInfo, scanType, chapter.number);

                // Si le chapitre n'existe pas dans la base de données, le créer
                if (currentChapterEntity == null) {
                    currentChapterEntity = new ChapterEntity();
                    currentChapterEntity.setName(chapter.name);
                    currentChapterEntity.setNumber(chapter.number);
                    currentChapterEntity.setMangaName(nameForInfo);
                    currentChapterEntity.setScanType(scanType);
                    currentChapterEntity.setDownloaded(chapter.isDownloaded);
                    currentChapterEntity.setLastReadTime(System.currentTimeMillis());

                    long id = chapterDao.insertChapter(currentChapterEntity);
                    currentChapterEntity.setId(id);
                    chapter.databaseId = id;
                } else {
                    // Mettre à jour l'horodatage de dernière lecture
                    currentChapterEntity.setLastReadTime(System.currentTimeMillis());
                    chapterDao.updateLastReadTime(currentChapterEntity.getId(), System.currentTimeMillis());
                    chapter.databaseId = currentChapterEntity.getId();
                    chapter.isDownloaded = currentChapterEntity.isDownloaded();

                    // Mettre à jour l'état du bouton de téléchargement sur l'UI thread
                    runOnUiThread(() -> updateDownloadButtonState(currentChapterEntity.isDownloaded()));
                }
            });

            // Mettre à jour les états des boutons
            updateButtonStates();
        }
    }

    /**
     * Met à jour l'état des boutons de navigation
     */
    private void updateButtonStates() {
        prevButton.setEnabled(currentChapterIndex > 0);
        nextButton.setEnabled(currentChapterIndex < chapters.size() - 1);
    }

    /**
     * Sauvegarde la progression de lecture actuelle
     * 
     * @param pagePosition Position actuelle dans le chapitre
     */
    private void saveReadingProgress(int pagePosition) {
        if (currentChapterEntity == null || currentChapterIndex < 0 || currentChapterIndex >= chapters.size()) {
            return;
        }

        Chapter chapter = chapters.get(currentChapterIndex);
        dbExecutor.execute(() -> {
            // Vérifier si le manga existe déjà dans la base de données
            MangaEntity manga = mangaDao.getMangaByName(nameForInfo);

            if (manga == null) {
                // Créer une nouvelle entité manga
                manga = new MangaEntity(nameForInfo, "");
                manga.updateReadingProgress(
                        currentChapterEntity.getId(),
                        currentChapterEntity.getNumber(),
                        currentChapterEntity.getName(),
                        pagePosition,
                        scanType // Sauvegarder l'URL du scan
                );
                long mangaId = mangaDao.insertManga(manga);
                manga.setId(mangaId);
                currentMangaEntity = manga;
            } else {
                // Mettre à jour la progression
                manga.updateReadingProgress(
                        currentChapterEntity.getId(),
                        currentChapterEntity.getNumber(),
                        currentChapterEntity.getName(),
                        pagePosition,
                        scanType // Sauvegarder l'URL du scan
                );
                mangaDao.updateManga(manga);
                currentMangaEntity = manga;
            }
        });
    }

    /**
     * Vérifie si l'utilisateur a une progression de lecture sauvegardée et la
     * restaure
     */
    private void loadReadingProgress() {
        dbExecutor.execute(() -> {
            // Récupérer le manga s'il existe
            MangaEntity manga = mangaDao.getMangaByName(nameForInfo);

            if (manga != null && manga.getLastReadChapterId() > 0) {
                currentMangaEntity = manga;

                // Trouver l'index du chapitre à charger
                int chapterToLoadIndex = -1;
                for (int i = 0; i < chapters.size(); i++) {
                    if (chapters.get(i).number == manga.getLastReadChapterNumber()) {
                        chapterToLoadIndex = i;
                        break;
                    }
                }

                if (chapterToLoadIndex >= 0) {
                    final int finalChapterIndex = chapterToLoadIndex;
                    final int pagePosition = manga.getLastReadPagePosition();

                    // Charger le chapitre et restaurer la position sur l'UI thread
                    runOnUiThread(() -> {
                        // Sélectionner le chapitre dans le spinner
                        chapterSpinner.setSelection(finalChapterIndex);

                        // Un petit délai pour s'assurer que le chapitre est chargé
                        scanRecyclerView.post(() -> {
                            // Défiler jusqu'à la position sauvegardée
                            scanRecyclerView.scrollToPosition(pagePosition);

                            // Mettre à jour les variables de suivi
                            lastVisibleItemPosition = pagePosition;
                            currentChapterIndex = finalChapterIndex;

                            Toast.makeText(ScanReaderActivity.this,
                                    "Reprise de la lecture au chapitre " +
                                            manga.getLastReadChapterName() +
                                            ", page " + (pagePosition + 1),
                                    Toast.LENGTH_SHORT).show();
                        });
                    });
                }
            }
        });
    }

    // Classe pour représenter un chapitre
    private static class Chapter {
        int number;
        String name;
        List<String> imageUrls;
        long databaseId = -1; // ID dans la base de données locale
        boolean isDownloaded = false; // Indique si le chapitre est téléchargé
    }

    // Adaptateur pour le RecyclerView
    private class ScanAdapter extends RecyclerView.Adapter<ScanAdapter.ScanViewHolder> {

        private List<String> pages;
        // Map pour suivre l'état de chargement des images (true = chargée, false = en
        // chargement/erreur)
        private Map<Integer, Boolean> loadingStatus;

        public ScanAdapter(List<String> pages) {
            this.pages = pages;
            this.loadingStatus = new HashMap<>();
        }

        public void updateData(List<String> newPages) {
            this.pages = newPages;
            this.loadingStatus.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ScanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_scan_page, parent, false);
            return new ScanViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ScanViewHolder holder, int position) {
            String pageUrl = pages.get(position);

            // Afficher l'indicateur de chargement
            holder.progressBar.setVisibility(View.VISIBLE);

            // Vérifier si c'est une page locale ou distante
            if (pageUrl.startsWith("file://")) {
                // C'est une image locale téléchargée
                File localFile = new File(pageUrl.substring(7)); // Enlever "file://"

                Glide.with(holder.itemView.getContext())
                        .load(localFile)
                        .override(2000, 2000)
                        .fitCenter()
                        .dontTransform()
                        .diskCacheStrategy(DiskCacheStrategy.NONE) // Pas besoin de cache disque pour les fichiers
                                                                   // locaux
                        .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(GlideException e, Object model,
                                    Target<android.graphics.drawable.Drawable> target,
                                    boolean isFirstResource) {
                                holder.progressBar.setVisibility(View.GONE);
                                loadingStatus.put(position, false);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model,
                                    Target<android.graphics.drawable.Drawable> target,
                                    DataSource dataSource,
                                    boolean isFirstResource) {
                                holder.progressBar.setVisibility(View.GONE);
                                loadingStatus.put(position, true);
                                return false;
                            }
                        })
                        .into(holder.imageView);
            } else {
                // C'est une image en ligne, utiliser notre méthode habituelle
                // Utiliser notre classe utilitaire pour créer une URL compatible avec Google
                // Drive
                GlideUrl glideUrl = DriveImageLoader.getGlideUrl(pageUrl);
                // Préparer aussi les URLs alternatives pour les tentatives suivantes
                String alternativeUrl = DriveImageLoader.getAlternativeUrl(pageUrl);
                String highQualityUrl = DriveImageLoader.getHighQualityUrl(pageUrl);

                // Si l'image est déjà chargée dans le cache, Glide la récupérera rapidement
                Glide.with(holder.itemView.getContext())
                        .load(glideUrl)
                        .thumbnail(Glide.with(holder.itemView.getContext())
                                .load(alternativeUrl)
                                .thumbnail(Glide.with(holder.itemView.getContext())
                                        .load(highQualityUrl)))
                        .timeout(60000)
                        .error(R.drawable.ic_launcher_foreground)
                        .override(2000, 2000)
                        .fitCenter()
                        .dontTransform()
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Mettre en cache sur le disque
                        .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(GlideException e, Object model,
                                    Target<android.graphics.drawable.Drawable> target,
                                    boolean isFirstResource) {
                                holder.progressBar.setVisibility(View.GONE);
                                loadingStatus.put(position, false);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model,
                                    Target<android.graphics.drawable.Drawable> target,
                                    DataSource dataSource,
                                    boolean isFirstResource) {
                                holder.progressBar.setVisibility(View.GONE);
                                loadingStatus.put(position, true);
                                return false;
                            }
                        })
                        .into(holder.imageView);
            }
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        public class ScanViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imageView;
            private final ProgressBar progressBar;

            public ScanViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.scanImageView);
                progressBar = itemView.findViewById(R.id.pageLoadingProgressBar);
            }
        }
    }
}