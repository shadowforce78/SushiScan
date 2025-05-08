package com.saumondeluxe.sushiscan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.saumondeluxe.sushiscan.database.DownloadedMangaManager;
import com.saumondeluxe.sushiscan.database.MangaEntity;
import com.saumondeluxe.sushiscan.database.RecentMangaManager;

import java.util.ArrayList;
import java.util.List;

public class LibraryActivity extends AppCompatActivity {

    private RecyclerView downloadedRecyclerView;
    private RecyclerView recentlyReadRecyclerView;
    private TextView emptyDownloadedTextView;
    private TextView emptyRecentlyReadTextView;
    private View loadingOverlay;
    private FloatingActionButton refreshButton;
    
    private MangaAdapter downloadedAdapter;
    private MangaAdapter recentlyReadAdapter;
    private List<MangaEntity> downloadedMangaList = new ArrayList<>();
    private List<MangaEntity> recentlyReadMangaList = new ArrayList<>();
    
    private DownloadedMangaManager downloadedMangaManager;
    private RecentMangaManager recentMangaManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        
        // Initialiser les vues
        downloadedRecyclerView = findViewById(R.id.libraryRecyclerView);
        recentlyReadRecyclerView = findViewById(R.id.mangaRecyclerView);
        emptyDownloadedTextView = findViewById(R.id.emptyLibraryTextView);
        emptyRecentlyReadTextView = findViewById(R.id.emptyTextView);
        loadingOverlay = findViewById(R.id.loadingProgressBar);
        refreshButton = findViewById(R.id.refreshFab);
        ImageButton backButton = findViewById(R.id.libraryToolbar).findViewById(android.R.id.home);
        
        // Configurer la toolbar avec un bouton de retour
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.libraryToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        
        // Initialiser les gestionnaires de données
        downloadedMangaManager = new DownloadedMangaManager(this);
        recentMangaManager = new RecentMangaManager(this);
        
        // Configurer les RecyclerViews
        setupRecyclerViews();
        
        // Configurer le bouton d'actualisation
        refreshButton.setOnClickListener(v -> refreshLibrary());
        
        // Configurer le SwipeRefreshLayout si présent
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshLibrary();
                swipeRefreshLayout.setRefreshing(false);
            });
        }
        
        // Charger les données
        loadLibraryData();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les données à chaque retour sur l'activité
        loadLibraryData();
    }
    
    private void setupRecyclerViews() {
        // Configurer la RecyclerView des mangas téléchargés
        downloadedAdapter = new MangaAdapter(downloadedMangaList, MangaAdapter.TYPE_DOWNLOADED);
        downloadedRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        downloadedRecyclerView.setAdapter(downloadedAdapter);
        
        // Configurer la RecyclerView des mangas récemment lus
        recentlyReadAdapter = new MangaAdapter(recentlyReadMangaList, MangaAdapter.TYPE_RECENT);
        recentlyReadRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recentlyReadRecyclerView.setAdapter(recentlyReadAdapter);
    }
    
    private void loadLibraryData() {
        loadingOverlay.setVisibility(View.VISIBLE);
        
        // Charger les mangas téléchargés
        downloadedMangaList.clear();
        downloadedMangaList.addAll(downloadedMangaManager.getDownloadedMangas());
        downloadedAdapter.notifyDataSetChanged();
        
        // Mettre à jour les vues vides
        emptyDownloadedTextView.setVisibility(downloadedMangaList.isEmpty() ? View.VISIBLE : View.GONE);
        
        // Charger les mangas récemment lus
        recentlyReadMangaList.clear();
        recentlyReadMangaList.addAll(recentMangaManager.getRecentlyReadMangas(10));
        recentlyReadAdapter.notifyDataSetChanged();
        
        // Mettre à jour les vues vides
        emptyRecentlyReadTextView.setVisibility(recentlyReadMangaList.isEmpty() ? View.VISIBLE : View.GONE);
        
        loadingOverlay.setVisibility(View.GONE);
    }
    
    private void refreshLibrary() {
        loadingOverlay.setVisibility(View.VISIBLE);
        // Vérifier les nouveaux téléchargements et les mises à jour
        downloadedMangaManager.rescanDownloadedMangas();
        // Recharger les données
        loadLibraryData();
    }
    
    private void openMangaDetails(MangaEntity manga) {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra("nameForInfo", manga.getName());
        intent.putExtra("showDetails", true);
        startActivity(intent);
    }
    
    private void openScanReader(MangaEntity manga) {
        Intent intent = new Intent(this, ScanReaderActivity.class);
        intent.putExtra("nameForInfo", manga.getName());
        intent.putExtra("scanUrl", manga.getScanTypeUrl());
        intent.putExtra("isDownloaded", true);
        startActivity(intent);
    }
    
    // Adapteur pour les mangas
    private class MangaAdapter extends RecyclerView.Adapter<MangaAdapter.MangaViewHolder> {
        
        public static final int TYPE_DOWNLOADED = 0;
        public static final int TYPE_RECENT = 1;
        public static final int TYPE_FAVORITE = 2;
        
        private final List<MangaEntity> mangaList;
        private final int type;
        
        public MangaAdapter(List<MangaEntity> mangaList, int type) {
            this.mangaList = mangaList;
            this.type = type;
        }
        
        @NonNull
        @Override
        public MangaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.item_manga_grid, parent, false);
            return new MangaViewHolder(itemView);
        }
        
        @Override
        public void onBindViewHolder(@NonNull MangaViewHolder holder, int position) {
            MangaEntity manga = mangaList.get(position);
            
            // Définir le titre
            holder.titleTextView.setText(manga.getName());
            
            // Définir l'info selon le type
            if (type == TYPE_RECENT) {
                holder.infoTextView.setText("Chapitre " + manga.getLastReadChapterNumber());
            } else {
                holder.infoTextView.setText(manga.getScanTypeUrl());
            }
            
            // Charger l'image de couverture
            if (manga.getImageUrl() != null && !manga.getImageUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(manga.getImageUrl())
                        .into(holder.coverImageView);
            }
            
            // Afficher les indicateurs appropriés
            holder.favoriteIndicator.setVisibility(manga.isFavorite() ? View.VISIBLE : View.GONE);
            holder.downloadedIndicator.setVisibility(manga.isDownloaded() ? View.VISIBLE : View.GONE);
            
            // Définir les écouteurs de clics
            holder.itemView.setOnClickListener(v -> {
                if (type == TYPE_DOWNLOADED) {
                    openScanReader(manga);
                } else {
                    openMangaDetails(manga);
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return mangaList.size();
        }
        
        class MangaViewHolder extends RecyclerView.ViewHolder {
            private final android.widget.ImageView coverImageView;
            private final android.widget.ImageView favoriteIndicator;
            private final android.widget.ImageView downloadedIndicator;
            private final TextView titleTextView;
            private final TextView infoTextView;
            
            public MangaViewHolder(@NonNull View itemView) {
                super(itemView);
                coverImageView = itemView.findViewById(R.id.mangaCoverImageView);
                favoriteIndicator = itemView.findViewById(R.id.favoriteIndicatorImageView);
                downloadedIndicator = itemView.findViewById(R.id.downloadedIndicatorImageView);
                titleTextView = itemView.findViewById(R.id.mangaTitleTextView);
                infoTextView = itemView.findViewById(R.id.mangaInfoTextView);
            }
        }
    }
}