package com.saumondeluxe.sushiscan;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.saumondeluxe.sushiscan.database.MangaDao;
import com.saumondeluxe.sushiscan.database.MangaEntity;
import com.saumondeluxe.sushiscan.database.SushiScanDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private ImageView mangaImage;
    private EditText searchEditText;
    private Button searchButton;
    private TextView animeTitleTextView, animeDetailsTextView;
    private RecyclerView animeRecyclerView, recentMangaRecyclerView;
    private LinearLayout recentMangaSection;
    private ScrollView detailScrollView;
    private LinearLayout scansContainer;
    private OkHttpClient client;
    private AnimeAdapter animeAdapter;
    private RecentMangaAdapter recentMangaAdapter;
    private List<AnimeItem> animeList;
    private List<MangaEntity> recentMangaList;

    // Base de données
    private SushiScanDatabase database;
    private MangaDao mangaDao;
    private Executor dbExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialiser les vues
        mangaImage = findViewById(R.id.mangaImage);
        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        animeRecyclerView = findViewById(R.id.animeRecyclerView);
        recentMangaRecyclerView = findViewById(R.id.recentMangaRecyclerView);
        recentMangaSection = findViewById(R.id.recentMangaSection);
        detailScrollView = findViewById(R.id.detailScrollView);
        animeTitleTextView = findViewById(R.id.animeTitleTextView);
        animeDetailsTextView = findViewById(R.id.animeDetailsTextView);
        scansContainer = findViewById(R.id.scansContainer);

        // Initialiser la base de données
        database = SushiScanDatabase.getInstance(this);
        mangaDao = database.mangaDao();
        dbExecutor = Executors.newSingleThreadExecutor();

        // Initialiser le client HTTP
        client = new OkHttpClient();

        // Initialiser la liste des recherches et l'adaptateur
        animeList = new ArrayList<>();
        animeAdapter = new AnimeAdapter(animeList);
        animeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        animeRecyclerView.setAdapter(animeAdapter);

        // Initialiser la liste des mangas récents et l'adaptateur
        recentMangaList = new ArrayList<>();
        recentMangaAdapter = new RecentMangaAdapter(recentMangaList);
        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,
                false);
        recentMangaRecyclerView.setLayoutManager(horizontalLayoutManager);
        recentMangaRecyclerView.setAdapter(recentMangaAdapter);

        // Charger les mangas récemment consultés
        loadRecentMangas();

        // Configurer le bouton de recherche
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchAnime(query);
                } else {
                    Toast.makeText(MainActivity.this, "Veuillez entrer un terme de recherche", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les mangas récents à chaque fois que l'activité reprend
        loadRecentMangas();
    }

    /**
     * Charge les mangas récemment consultés depuis la base de données
     */
    private void loadRecentMangas() {
        // Observer les changements de la liste des mangas récents (LiveData)
        mangaDao.getRecentlyReadManga(5).observe(this, new Observer<List<MangaEntity>>() {
            @Override
            public void onChanged(List<MangaEntity> mangaEntities) {
                if (mangaEntities != null && !mangaEntities.isEmpty()) {
                    recentMangaList.clear();
                    recentMangaList.addAll(mangaEntities);
                    recentMangaAdapter.notifyDataSetChanged();
                    recentMangaSection.setVisibility(View.VISIBLE);
                } else {
                    // Masquer la section si aucun manga récent
                    recentMangaSection.setVisibility(View.GONE);
                }
            }
        });
    }

    private void searchAnime(String query) {
        // Afficher la RecyclerView et masquer les détails
        animeRecyclerView.setVisibility(View.VISIBLE);
        detailScrollView.setVisibility(View.GONE);

        // Construire l'URL de l'API avec le terme de recherche
        String url = "https://api.saumondeluxe.com/scans/search/" + query;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Erreur de connexion: " + e.getMessage(), Toast.LENGTH_LONG)
                                .show();
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
                            // Effacer la liste précédente
                            animeList.clear();

                            // Parser les données JSON
                            JSONObject jsonObject = new JSONObject(responseData);
                            JSONArray resultsArray = jsonObject.getJSONArray("results");

                            // Extraire les informations des résultats
                            for (int i = 0; i < resultsArray.length(); i++) {
                                JSONObject animeObject = resultsArray.getJSONObject(i);
                                String title = animeObject.getString("titre");
                                String imageUrl = animeObject.getString("image");
                                String nameForInfo = animeObject.getString("name-for-info");

                                AnimeItem animeItem = new AnimeItem(title, imageUrl, nameForInfo);
                                animeList.add(animeItem);
                            }

                            // Mettre à jour l'adaptateur
                            animeAdapter.notifyDataSetChanged();

                        } catch (JSONException e) {
                            Toast.makeText(MainActivity.this, "Erreur de parsing JSON: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    private void getAnimeInfo(String nameForInfo) {
        // Nettoyer l'ancienne UI des scans
        scansContainer.setVisibility(View.GONE);
        scansContainer.removeAllViews();

        // Ajouter le titre de la section
        TextView titleTextView = new TextView(this);
        titleTextView.setText("Scans disponibles");
        titleTextView.setTextSize(16);
        // Remplacer la méthode dépréciée par la version à jour
        titleTextView.setTextAppearance(android.R.style.TextAppearance_Medium);
        scansContainer.addView(titleTextView);

        // Construire l'URL de l'API pour les détails
        String url = "https://api.saumondeluxe.com/scans/info/" + nameForInfo;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Erreur de connexion: " + e.getMessage(), Toast.LENGTH_LONG)
                                .show();
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

                            // Afficher les informations détaillées
                            if (jsonObject.has("titre")) {
                                String title = jsonObject.getString("titre");
                                animeTitleTextView.setText(title);
                            }

                            // Construire un texte avec les détails
                            StringBuilder detailsBuilder = new StringBuilder();

                            if (jsonObject.has("synopsis")) {
                                detailsBuilder.append("Synopsis: ")
                                        .append(jsonObject.getString("synopsis"))
                                        .append("\n\n");
                            } else if (jsonObject.has("description")) {
                                detailsBuilder.append("Description: ")
                                        .append(jsonObject.getString("description"))
                                        .append("\n\n");
                            }

                            if (jsonObject.has("genres")) {
                                detailsBuilder.append("Genres: ")
                                        .append(jsonObject.getString("genres"))
                                        .append("\n\n");
                            }

                            if (jsonObject.has("avancement")) {
                                detailsBuilder.append("Avancement: ")
                                        .append(jsonObject.getString("avancement"))
                                        .append("\n\n");
                            } else if (jsonObject.has("statut")) {
                                detailsBuilder.append("Statut: ")
                                        .append(jsonObject.getString("statut"))
                                        .append("\n\n");
                            }

                            animeDetailsTextView.setText(detailsBuilder.toString());

                            // Charger l'image
                            if (jsonObject.has("image")) {
                                String imageUrl = jsonObject.getString("image");
                                Glide.with(MainActivity.this)
                                        .load(imageUrl)
                                        .into(mangaImage);
                            } else if (jsonObject.has("image_url")) {
                                String imageUrl = jsonObject.getString("image_url");
                                Glide.with(MainActivity.this)
                                        .load(imageUrl)
                                        .into(mangaImage);
                            }

                            // Gérer les scans disponibles
                            if (jsonObject.has("contenu_disponible")) {
                                JSONObject contentObj = jsonObject.getJSONObject("contenu_disponible");

                                if (contentObj.has("manga")) {
                                    JSONObject mangaObj = contentObj.getJSONObject("manga");

                                    if (mangaObj.has("disponible") && mangaObj.getBoolean("disponible")) {
                                        // Si des scans sont disponibles
                                        if (mangaObj.has("types")) {
                                            JSONArray typesArray = mangaObj.getJSONArray("types");

                                            if (typesArray.length() > 0) {
                                                // Rendre le conteneur visible
                                                scansContainer.setVisibility(View.VISIBLE);

                                                // Ajouter des boutons pour chaque type de scan
                                                for (int i = 0; i < typesArray.length(); i++) {
                                                    JSONObject typeObj = typesArray.getJSONObject(i);
                                                    String typeName = typeObj.getString("nom");
                                                    String typeUrl = typeObj.getString("url");

                                                    // Créer un bouton pour ce type de scan
                                                    Button scanButton = new Button(MainActivity.this);
                                                    scanButton.setText("Lire les " + typeName);
                                                    scanButton.setLayoutParams(new LinearLayout.LayoutParams(
                                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                                            LinearLayout.LayoutParams.WRAP_CONTENT));

                                                    // Stocker l'URL comme tag
                                                    scanButton.setTag(typeUrl);

                                                    // Ajouter un écouteur de clic
                                                    final String finalNameForInfo = nameForInfo;
                                                    scanButton.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            // Lancer l'activité de lecture de scans
                                                            String scanUrl = (String) v.getTag();
                                                            openScanReader(finalNameForInfo, scanUrl);
                                                        }
                                                    });

                                                    // Ajouter le bouton au conteneur
                                                    scansContainer.addView(scanButton);
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Afficher la vue détaillée
                            animeRecyclerView.setVisibility(View.GONE);
                            detailScrollView.setVisibility(View.VISIBLE);

                        } catch (JSONException e) {
                            Toast.makeText(MainActivity.this, "Erreur de parsing JSON: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    private void openScanReader(String nameForInfo, String scanUrl) {
        // Créer un Intent pour lancer l'activité de lecture
        Intent intent = new Intent(this, ScanReaderActivity.class);
        intent.putExtra("nameForInfo", nameForInfo);
        intent.putExtra("scanUrl", scanUrl);
        startActivity(intent);
    }

    // Classe pour stocker les informations d'un anime
    private static class AnimeItem {
        private final String title;
        private final String imageUrl;
        private final String nameForInfo;

        public AnimeItem(String title, String imageUrl, String nameForInfo) {
            this.title = title;
            this.imageUrl = imageUrl;
            this.nameForInfo = nameForInfo;
        }
    }

    // Adaptateur pour la RecyclerView
    private class AnimeAdapter extends RecyclerView.Adapter<AnimeAdapter.AnimeViewHolder> {

        private final List<AnimeItem> animeItems;

        public AnimeAdapter(List<AnimeItem> animeItems) {
            this.animeItems = animeItems;
        }

        @NonNull
        @Override
        public AnimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_anime, parent, false);
            return new AnimeViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull AnimeViewHolder holder, int position) {
            AnimeItem animeItem = animeItems.get(position);
            holder.titleTextView.setText(animeItem.title);

            // Charger l'image avec Glide
            Glide.with(holder.itemView.getContext())
                    .load(animeItem.imageUrl)
                    .into(holder.imageView);

            // Configurer le bouton pour afficher les détails
            holder.detailButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getAnimeInfo(animeItem.nameForInfo);
                }
            });
        }

        @Override
        public int getItemCount() {
            return animeItems.size();
        }

        public class AnimeViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imageView;
            private final TextView titleTextView;
            private final Button detailButton;

            public AnimeViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.animeImageView);
                titleTextView = itemView.findViewById(R.id.animeTitleTextView);
                detailButton = itemView.findViewById(R.id.viewDetailsButton);
            }
        }
    }

    // Adaptateur pour la RecyclerView des mangas récents
    private class RecentMangaAdapter extends RecyclerView.Adapter<RecentMangaAdapter.RecentMangaViewHolder> {

        private final List<MangaEntity> mangaList;

        public RecentMangaAdapter(List<MangaEntity> mangaList) {
            this.mangaList = mangaList;
        }

        @NonNull
        @Override
        public RecentMangaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recent_manga, parent, false);
            return new RecentMangaViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull RecentMangaViewHolder holder, int position) {
            MangaEntity manga = mangaList.get(position);
            holder.titleTextView.setText(manga.getName());

            // Afficher le chapitre en cours de lecture
            String chapterInfo = "Ch. " + manga.getLastReadChapterNumber();
            if (manga.getLastReadChapterName() != null && !manga.getLastReadChapterName().isEmpty()) {
                chapterInfo += " - " + manga.getLastReadChapterName();
            }
            holder.chapterTextView.setText(chapterInfo);

            // Charger l'image avec Glide si disponible
            if (manga.getImageUrl() != null && !manga.getImageUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(manga.getImageUrl())
                        .into(holder.imageView);
            } else {
                // Utiliser une image par défaut
                holder.imageView.setImageResource(R.drawable.ic_launcher_foreground);
            }

            // Configurer le clic pour reprendre la lecture
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Utiliser l'URL du scan stockée dans l'entité manga
                    String scanType = manga.getScanTypeUrl();

                    // Si l'URL est vide, utiliser une valeur par défaut
                    if (scanType == null || scanType.isEmpty()) {
                        scanType = "scan"; // URL par défaut
                        Toast.makeText(holder.itemView.getContext(),
                                "URL de scan non disponible, utilisation de l'URL par défaut",
                                Toast.LENGTH_SHORT).show();
                    }

                    // Lancer l'activité de lecture avec l'URL correcte
                    final String finalScanType = scanType;
                    openScanReader(manga.getName(), finalScanType);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mangaList.size();
        }

        public class RecentMangaViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imageView;
            private final TextView titleTextView;
            private final TextView chapterTextView;

            public RecentMangaViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.recentMangaImage);
                titleTextView = itemView.findViewById(R.id.recentMangaTitle);
                chapterTextView = itemView.findViewById(R.id.recentMangaChapter);
            }
        }
    }
}
