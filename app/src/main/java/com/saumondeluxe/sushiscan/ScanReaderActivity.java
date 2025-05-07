package com.saumondeluxe.sushiscan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ScanReaderActivity extends AppCompatActivity {

    private ViewPager2 scanViewPager;
    private ProgressBar loadingProgressBar;
    private TextView errorTextView, titleTextView, pageCounterTextView;
    private Button prevButton, nextButton, backButton;
    private OkHttpClient client;
    private List<String> scanPages;
    private ScanPagerAdapter adapter;
    private String nameForInfo;
    private String scanType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_reader);

        // Initialiser les vues
        scanViewPager = findViewById(R.id.scanViewPager);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        errorTextView = findViewById(R.id.errorTextView);
        titleTextView = findViewById(R.id.titleTextView);
        pageCounterTextView = findViewById(R.id.pageCounterTextView);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        backButton = findViewById(R.id.backButton);

        // Récupérer les données de l'intent
        if (getIntent() != null) {
            nameForInfo = getIntent().getStringExtra("nameForInfo");
            scanType = getIntent().getStringExtra("scanUrl");
            
            // Définir le titre
            titleTextView.setText(nameForInfo);
        }

        // Initialiser le client HTTP
        client = new OkHttpClient();
        
        // Initialiser la liste et l'adaptateur
        scanPages = new ArrayList<>();
        adapter = new ScanPagerAdapter(scanPages);
        scanViewPager.setAdapter(adapter);
        
        // Écouter les changements de page
        scanViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updatePageCounter(position);
                updateButtonStates(position);
            }
        });
        
        // Configurer les boutons de navigation
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (scanViewPager.getCurrentItem() > 0) {
                    scanViewPager.setCurrentItem(scanViewPager.getCurrentItem() - 1);
                }
            }
        });
        
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (scanViewPager.getCurrentItem() < scanPages.size() - 1) {
                    scanViewPager.setCurrentItem(scanViewPager.getCurrentItem() + 1);
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
                                // Effacer la liste précédente
                                scanPages.clear();
                                
                                // Extraire le contenu brut contenant les URLs des pages
                                if (jsonObject.has("rawContent")) {
                                    String rawContent = jsonObject.getString("rawContent");
                                    List<String> urls = extractUrlsFromRawContent(rawContent);
                                    
                                    if (!urls.isEmpty()) {
                                        scanPages.addAll(urls);
                                        adapter.notifyDataSetChanged();
                                        
                                        // Initialiser le compteur de pages
                                        updatePageCounter(0);
                                        updateButtonStates(0);
                                        
                                        loadingProgressBar.setVisibility(View.GONE);
                                    } else {
                                        // Tenter d'obtenir les URLs à partir de scriptUrl si disponible
                                        if (jsonObject.has("scriptUrl")) {
                                            loadScriptContent(jsonObject.getString("scriptUrl"));
                                        } else {
                                            // Aucune page trouvée
                                            loadingProgressBar.setVisibility(View.GONE);
                                            errorTextView.setVisibility(View.VISIBLE);
                                            errorTextView.setText("Format de donnée inattendu ou aucune page trouvée");
                                        }
                                    }
                                } else {
                                    // Tenter d'obtenir les URLs à partir de scriptUrl si disponible
                                    if (jsonObject.has("scriptUrl")) {
                                        loadScriptContent(jsonObject.getString("scriptUrl"));
                                    } else {
                                        // Aucune page trouvée
                                        loadingProgressBar.setVisibility(View.GONE);
                                        errorTextView.setVisibility(View.VISIBLE);
                                        errorTextView.setText("Format de donnée inattendu ou aucune page trouvée");
                                    }
                                }
                            } else {
                                // Afficher le message d'erreur retourné par l'API
                                String message = jsonObject.has("message") ? 
                                    jsonObject.getString("message") : "Erreur lors du chargement des scans";
                                
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
    
    private void loadScriptContent(String scriptUrl) {
        Request request = new Request.Builder()
                .url(scriptUrl)
                .build();
                
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingProgressBar.setVisibility(View.GONE);
                        errorTextView.setVisibility(View.VISIBLE);
                        errorTextView.setText("Erreur de chargement du script: " + e.getMessage());
                    }
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String scriptContent = response.body().string();
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        List<String> urls = extractUrlsFromRawContent(scriptContent);
                        
                        if (!urls.isEmpty()) {
                            scanPages.clear();
                            scanPages.addAll(urls);
                            adapter.notifyDataSetChanged();
                            
                            // Initialiser le compteur de pages
                            updatePageCounter(0);
                            updateButtonStates(0);
                            
                            loadingProgressBar.setVisibility(View.GONE);
                        } else {
                            loadingProgressBar.setVisibility(View.GONE);
                            errorTextView.setVisibility(View.VISIBLE);
                            errorTextView.setText("Aucune URL d'image trouvée dans le script");
                        }
                    }
                });
            }
        });
    }
    
    private List<String> extractUrlsFromRawContent(String rawContent) {
        List<String> urls = new ArrayList<>();
        
        // Expression régulière pour trouver les URLs entre guillemets simples ou doubles
        Pattern pattern = Pattern.compile("['\"](https?://[^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(rawContent);
        
        while (matcher.find()) {
            String url = matcher.group(1);
            urls.add(url);
        }
        
        return urls;
    }
    
    private void updatePageCounter(int position) {
        pageCounterTextView.setText((position + 1) + "/" + scanPages.size());
    }
    
    private void updateButtonStates(int position) {
        prevButton.setEnabled(position > 0);
        nextButton.setEnabled(position < scanPages.size() - 1);
    }
    
    // Adaptateur pour le ViewPager2
    private class ScanPagerAdapter extends RecyclerView.Adapter<ScanPagerAdapter.ScanPageViewHolder> {
        
        private final List<String> pages;
        
        public ScanPagerAdapter(List<String> pages) {
            this.pages = pages;
        }
        
        @NonNull
        @Override
        public ScanPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_scan_page, parent, false);
            return new ScanPageViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ScanPageViewHolder holder, int position) {
            String pageUrl = pages.get(position);
            
            // Afficher l'indicateur de chargement
            holder.progressBar.setVisibility(View.VISIBLE);
            
            // Charger l'image avec Glide
            Glide.with(holder.itemView.getContext())
                    .load(pageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_launcher_foreground) // Utilisez une image d'erreur appropriée
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, 
                                                   com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                   boolean isFirstResource) {
                            holder.progressBar.setVisibility(View.GONE);
                            Toast.makeText(ScanReaderActivity.this, "Échec du chargement de la page " + (position + 1), 
                                          Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, 
                                                      com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                      com.bumptech.glide.load.DataSource dataSource, 
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
        
        public class ScanPageViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imageView;
            private final ProgressBar progressBar;
            
            public ScanPageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.scanImageView);
                progressBar = itemView.findViewById(R.id.pageLoadingProgressBar);
            }
        }
    }
}