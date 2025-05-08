package com.saumondeluxe.sushiscan;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchActivity extends AppCompatActivity {

    private ImageView mangaImage;
    private EditText searchEditText;
    private Button searchButton;
    private ImageButton backButton;
    private TextView animeTitleTextView, animeDetailsTextView;
    private RecyclerView animeRecyclerView;
    private ScrollView detailScrollView;
    private LinearLayout scansContainer;
    private ProgressBar loadingProgressBar;
    private OkHttpClient client;
    private List<AnimeItem> animeList;
    private AnimeAdapter animeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initialize views
        mangaImage = findViewById(R.id.mangaImage);
        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        backButton = findViewById(R.id.backButton);
        animeRecyclerView = findViewById(R.id.animeRecyclerView);
        detailScrollView = findViewById(R.id.detailScrollView);
        animeTitleTextView = findViewById(R.id.animeTitleTextView);
        animeDetailsTextView = findViewById(R.id.animeDetailsTextView);
        scansContainer = findViewById(R.id.scansContainer);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);

        // Initialize HTTP client
        client = new OkHttpClient();

        // Initialize anime list and adapter
        animeList = new ArrayList<>();
        animeAdapter = new AnimeAdapter(animeList);
        animeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        animeRecyclerView.setAdapter(animeAdapter);

        // Set up back button
        backButton.setOnClickListener(v -> finish());

        // Set up search button
        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                searchAnime(query);
            } else {
                Toast.makeText(SearchActivity.this, "Veuillez entrer un terme de recherche", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up search on enter key
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                searchButton.performClick();
                return true;
            }
            return false;
        });
    }

    private void searchAnime(String query) {
        // Show loading indicator
        loadingProgressBar.setVisibility(View.VISIBLE);
        
        // Hide detail view and show recycler view
        animeRecyclerView.setVisibility(View.VISIBLE);
        detailScrollView.setVisibility(View.GONE);

        // Build API URL with search term
        String url = "https://api.saumondeluxe.com/scans/search/" + query;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    Toast.makeText(SearchActivity.this, "Erreur de connexion: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseData = response.body().string();

                runOnUiThread(() -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    try {
                        // Clear previous list
                        animeList.clear();

                        // Parse JSON data
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray resultsArray = jsonObject.getJSONArray("results");

                        // Extract results information
                        for (int i = 0; i < resultsArray.length(); i++) {
                            JSONObject animeObject = resultsArray.getJSONObject(i);
                            String title = animeObject.getString("titre");
                            String imageUrl = animeObject.getString("image");
                            String nameForInfo = animeObject.getString("name-for-info");

                            AnimeItem animeItem = new AnimeItem(title, imageUrl, nameForInfo);
                            animeList.add(animeItem);
                        }

                        // Update adapter
                        animeAdapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        Toast.makeText(SearchActivity.this, "Erreur de parsing JSON: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void getAnimeInfo(String nameForInfo) {
        // Show loading
        loadingProgressBar.setVisibility(View.VISIBLE);
        
        // Clear previous scan UI
        scansContainer.setVisibility(View.GONE);
        scansContainer.removeAllViews();
        
        // Add section title
        TextView titleTextView = new TextView(this);
        titleTextView.setText("Scans disponibles");
        titleTextView.setTextSize(16);
        titleTextView.setTextColor(getResources().getColor(android.R.color.white));
        titleTextView.setTextAppearance(android.R.style.TextAppearance_Medium);
        scansContainer.addView(titleTextView);

        // Build API URL for details
        String url = "https://api.saumondeluxe.com/scans/info/" + nameForInfo;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    Toast.makeText(SearchActivity.this, "Erreur de connexion: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseData = response.body().string();

                runOnUiThread(() -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    try {
                        // Parse JSON data
                        JSONObject jsonObject = new JSONObject(responseData);

                        // Display detailed information
                        if (jsonObject.has("titre")) {
                            String title = jsonObject.getString("titre");
                            animeTitleTextView.setText(title);
                        }

                        // Build text with details
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

                        // Load image
                        if (jsonObject.has("image")) {
                            String imageUrl = jsonObject.getString("image");
                            Glide.with(SearchActivity.this)
                                    .load(imageUrl)
                                    .into(mangaImage);
                        } else if (jsonObject.has("image_url")) {
                            String imageUrl = jsonObject.getString("image_url");
                            Glide.with(SearchActivity.this)
                                    .load(imageUrl)
                                    .into(mangaImage);
                        }

                        // Handle available scans
                        if (jsonObject.has("contenu_disponible")) {
                            JSONObject contentObj = jsonObject.getJSONObject("contenu_disponible");

                            if (contentObj.has("manga")) {
                                JSONObject mangaObj = contentObj.getJSONObject("manga");

                                if (mangaObj.has("disponible") && mangaObj.getBoolean("disponible")) {
                                    // If scans are available
                                    if (mangaObj.has("types")) {
                                        JSONArray typesArray = mangaObj.getJSONArray("types");

                                        if (typesArray.length() > 0) {
                                            // Make container visible
                                            scansContainer.setVisibility(View.VISIBLE);

                                            // Add buttons for each scan type
                                            for (int i = 0; i < typesArray.length(); i++) {
                                                JSONObject typeObj = typesArray.getJSONObject(i);
                                                String typeName = typeObj.getString("nom");
                                                String typeUrl = typeObj.getString("url");

                                                // Create button for this scan type
                                                Button scanButton = new Button(SearchActivity.this);
                                                scanButton.setText("Lire les " + typeName);
                                                scanButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));
                                                scanButton.setTextColor(getResources().getColor(android.R.color.white));
                                                scanButton.setLayoutParams(new LinearLayout.LayoutParams(
                                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT));
                                                scanButton.setPadding(8, 8, 8, 8);
                                                scanButton.setLayoutParams(new LinearLayout.LayoutParams(
                                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT));
                                                ((LinearLayout.LayoutParams) scanButton.getLayoutParams()).setMargins(0, 8, 0, 8);

                                                // Store URL as tag
                                                scanButton.setTag(typeUrl);

                                                // Add click listener
                                                final String finalNameForInfo = nameForInfo;
                                                scanButton.setOnClickListener(v -> {
                                                    // Launch scan reader activity
                                                    String scanUrl = (String) v.getTag();
                                                    openScanReader(finalNameForInfo, scanUrl);
                                                });

                                                // Add button to container
                                                scansContainer.addView(scanButton);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Show detail view
                        animeRecyclerView.setVisibility(View.GONE);
                        detailScrollView.setVisibility(View.VISIBLE);

                    } catch (JSONException e) {
                        Toast.makeText(SearchActivity.this, "Erreur de parsing JSON: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void openScanReader(String nameForInfo, String scanUrl) {
        // Create Intent to launch reader activity
        Intent intent = new Intent(this, ScanReaderActivity.class);
        intent.putExtra("nameForInfo", nameForInfo);
        intent.putExtra("scanUrl", scanUrl);
        startActivity(intent);
    }

    // Class to store anime information
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

    // Adapter for RecyclerView
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

            // Load image with Glide
            Glide.with(holder.itemView.getContext())
                    .load(animeItem.imageUrl)
                    .into(holder.imageView);

            // Configure button to show details
            holder.detailButton.setOnClickListener(v -> getAnimeInfo(animeItem.nameForInfo));
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
}