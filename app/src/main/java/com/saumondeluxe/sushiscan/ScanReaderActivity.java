package com.saumondeluxe.sushiscan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ScanReaderActivity extends AppCompatActivity {

    private RecyclerView scanRecyclerView;
    private ProgressBar loadingProgressBar;
    private TextView errorTextView, titleTextView;
    private Button prevButton, nextButton, backButton;
    private Spinner chapterSpinner;
    private OkHttpClient client;

    // Utilisation d'une Map pour stocker les chapitres et leurs pages
    private List<Chapter> chapters;
    private ScanAdapter adapter;
    private String nameForInfo;
    private String scanType;
    private int currentChapterIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_reader);

        // Initialiser les vues
        scanRecyclerView = findViewById(R.id.scanRecyclerView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        errorTextView = findViewById(R.id.errorTextView);
        titleTextView = findViewById(R.id.titleTextView);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        backButton = findViewById(R.id.backButton);
        chapterSpinner = findViewById(R.id.chapterSpinner);

        // Configurer le RecyclerView
        scanRecyclerView.setLayoutManager(new LinearLayoutManager(this));

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
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentChapterIndex > 0) {
                    chapterSpinner.setSelection(currentChapterIndex - 1);
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentChapterIndex < chapters.size() - 1) {
                    chapterSpinner.setSelection(currentChapterIndex + 1);
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Charger les scans
        loadScans();
    }

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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingProgressBar.setVisibility(View.GONE);
                        errorTextView.setVisibility(View.VISIBLE);
                        errorTextView.setText("Erreur de connexion: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseData = response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
                                    }

                                    if (!chapters.isEmpty()) {
                                        setupChapterSpinner();
                                        loadingProgressBar.setVisibility(View.GONE);
                                    } else {
                                        loadingProgressBar.setVisibility(View.GONE);
                                        errorTextView.setVisibility(View.VISIBLE);
                                        errorTextView.setText("Aucun chapitre trouvé");
                                    }
                                } else {
                                    // Ancienne méthode (par sécurité, mais ne devrait plus être utilisée)
                                    loadingProgressBar.setVisibility(View.GONE);
                                    errorTextView.setVisibility(View.VISIBLE);
                                    errorTextView.setText(
                                            "Format d'API non pris en charge. Veuillez mettre à jour l'application.");
                                }
                            } else {
                                // Afficher le message d'erreur retourné par l'API
                                String message = jsonObject.has("message") ? jsonObject.getString("message")
                                        : "Erreur lors du chargement des scans";

                                loadingProgressBar.setVisibility(View.GONE);
                                errorTextView.setVisibility(View.VISIBLE);
                                errorTextView.setText(message);
                            }
                        } catch (JSONException e) {
                            loadingProgressBar.setVisibility(View.GONE);
                            errorTextView.setVisibility(View.VISIBLE);
                            errorTextView.setText("Erreur de parsing JSON: " + e.getMessage());
                        }
                    }
                });
            }
        });
    }

    private void setupChapterSpinner() {
        // Préparer les données pour le Spinner
        List<String> chaptersDisplay = new ArrayList<>();
        for (Chapter chapter : chapters) {
            chaptersDisplay.add(chapter.name);
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

    private void loadChapter(int index) {
        if (index >= 0 && index < chapters.size()) {
            Chapter chapter = chapters.get(index);

            // Mettre à jour l'adaptateur avec les nouvelles pages
            adapter.updateData(chapter.imageUrls);

            // Faire défiler au début du chapitre
            scanRecyclerView.scrollToPosition(0);

            // Mettre à jour les états des boutons
            updateButtonStates();
        }
    }

    private void updateButtonStates() {
        prevButton.setEnabled(currentChapterIndex > 0);
        nextButton.setEnabled(currentChapterIndex < chapters.size() - 1);
    }

    // Classe pour représenter un chapitre
    private static class Chapter {
        int number;
        String name;
        List<String> imageUrls;
    }

    // Adaptateur pour le RecyclerView
    private class ScanAdapter extends RecyclerView.Adapter<ScanAdapter.ScanViewHolder> {

        private List<String> pages;

        public ScanAdapter(List<String> pages) {
            this.pages = pages;
        }

        public void updateData(List<String> newPages) {
            this.pages = newPages;
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

            // Utiliser notre classe utilitaire pour créer une URL compatible avec Google
            // Drive
            GlideUrl glideUrl = DriveImageLoader.getGlideUrl(pageUrl);
            // Préparer aussi les URLs alternatives pour les tentatives suivantes
            String alternativeUrl = DriveImageLoader.getAlternativeUrl(pageUrl);
            String highQualityUrl = DriveImageLoader.getHighQualityUrl(pageUrl);

            // Charger l'image avec Glide en utilisant la GlideUrl personnalisée (1er essai)
            Glide.with(holder.itemView.getContext())
                    .load(glideUrl)
                    .timeout(60000) // 60 secondes de timeout
                    .fallback(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .override(2000, 2000) // Requête d'image plus grande pour meilleure qualité
                    .fitCenter() // Conserver les proportions
                    .dontTransform() // Désactiver les transformations pour préserver la qualité
                    .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model,
                                Target<android.graphics.drawable.Drawable> target,
                                boolean isFirstResource) {
                            // Si la première méthode échoue, essayer l'URL alternative (2ème essai)
                            Glide.with(holder.itemView.getContext())
                                    .load(alternativeUrl)
                                    .timeout(60000)
                                    .error(R.drawable.ic_launcher_foreground)
                                    .override(2000, 2000)
                                    .fitCenter()
                                    .dontTransform()
                                    .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                                        @Override
                                        public boolean onLoadFailed(GlideException e, Object model,
                                                Target<android.graphics.drawable.Drawable> target,
                                                boolean isFirstResource) {
                                            // Si la deuxième méthode échoue, essayer l'URL haute qualité (3ème essai)
                                            Glide.with(holder.itemView.getContext())
                                                    .load(highQualityUrl)
                                                    .timeout(60000)
                                                    .error(R.drawable.ic_launcher_foreground)
                                                    .override(2000, 2000)
                                                    .fitCenter()
                                                    .dontTransform()
                                                    .listener(
                                                            new RequestListener<android.graphics.drawable.Drawable>() {
                                                                @Override
                                                                public boolean onLoadFailed(GlideException e,
                                                                        Object model,
                                                                        Target<android.graphics.drawable.Drawable> target,
                                                                        boolean isFirstResource) {
                                                                    holder.progressBar.setVisibility(View.GONE);
                                                                    Toast.makeText(ScanReaderActivity.this,
                                                                            "Échec du chargement de la page "
                                                                                    + (position + 1),
                                                                            Toast.LENGTH_SHORT).show();
                                                                    return false;
                                                                }

                                                                @Override
                                                                public boolean onResourceReady(
                                                                        android.graphics.drawable.Drawable resource,
                                                                        Object model,
                                                                        Target<android.graphics.drawable.Drawable> target,
                                                                        DataSource dataSource,
                                                                        boolean isFirstResource) {
                                                                    holder.progressBar.setVisibility(View.GONE);
                                                                    return false;
                                                                }
                                                            })
                                                    .into(holder.imageView);
                                            return true;
                                        }

                                        @Override
                                        public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                                Object model,
                                                Target<android.graphics.drawable.Drawable> target,
                                                DataSource dataSource,
                                                boolean isFirstResource) {
                                            holder.progressBar.setVisibility(View.GONE);
                                            return false;
                                        }
                                    })
                                    .into(holder.imageView);
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model,
                                Target<android.graphics.drawable.Drawable> target,
                                DataSource dataSource,
                                boolean isFirstResource) {
                            holder.progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(holder.imageView);
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