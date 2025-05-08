package com.saumondeluxe.sushiscan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.saumondeluxe.sushiscan.database.FavoriteMangaManager;
import com.saumondeluxe.sushiscan.database.MangaEntity;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView favoritesRecyclerView;
    private View emptyView;
    private View loadingOverlay;
    
    private FavoritesAdapter favoritesAdapter;
    private List<MangaEntity> favoritesList = new ArrayList<>();
    
    private FavoriteMangaManager favoriteMangaManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        
        // Initialiser les vues
        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        ImageButton backButton = findViewById(R.id.backButton);
        
        // Initialiser le gestionnaire de favoris
        favoriteMangaManager = new FavoriteMangaManager(this);
        
        // Configurer le bouton de retour
        backButton.setOnClickListener(v -> finish());
        
        // Configurer la RecyclerView
        setupRecyclerView();
        
        // Charger les données
        loadFavorites();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les données à chaque retour sur l'activité
        loadFavorites();
    }
    
    private void setupRecyclerView() {
        favoritesAdapter = new FavoritesAdapter(favoritesList);
        favoritesRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        favoritesRecyclerView.setAdapter(favoritesAdapter);
    }
    
    private void loadFavorites() {
        loadingOverlay.setVisibility(View.VISIBLE);
        
        // Charger les mangas favoris
        favoritesList.clear();
        favoritesList.addAll(favoriteMangaManager.getFavoriteMangas());
        favoritesAdapter.notifyDataSetChanged();
        
        // Afficher ou masquer la vue vide
        updateEmptyView();
        
        loadingOverlay.setVisibility(View.GONE);
    }
    
    private void updateEmptyView() {
        if (favoritesList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            favoritesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            favoritesRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void openMangaDetails(MangaEntity manga) {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra("nameForInfo", manga.getName());
        intent.putExtra("showDetails", true);
        startActivity(intent);
    }
    
    // Adapteur pour les favoris
    private class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {
        
        private final List<MangaEntity> mangaList;
        
        public FavoritesAdapter(List<MangaEntity> mangaList) {
            this.mangaList = mangaList;
        }
        
        @NonNull
        @Override
        public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.item_manga_grid, parent, false);
            return new FavoriteViewHolder(itemView);
        }
        
        @Override
        public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
            MangaEntity manga = mangaList.get(position);
            
            // Définir le titre
            holder.titleTextView.setText(manga.getName());
            
            // Définir l'info additionnelle
            holder.infoTextView.setText(manga.getGenres());
            
            // Charger l'image de couverture
            if (manga.getImageUrl() != null && !manga.getImageUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(manga.getImageUrl())
                        .into(holder.coverImageView);
            }
            
            // Afficher l'indicateur de favori
            holder.favoriteIndicator.setVisibility(View.VISIBLE);
            
            // Afficher l'indicateur de téléchargement si nécessaire
            holder.downloadedIndicator.setVisibility(manga.isDownloaded() ? View.VISIBLE : View.GONE);
            
            // Configurer le clic sur l'élément
            holder.itemView.setOnClickListener(v -> openMangaDetails(manga));
        }
        
        @Override
        public int getItemCount() {
            return mangaList.size();
        }
        
        class FavoriteViewHolder extends RecyclerView.ViewHolder {
            private final ImageView coverImageView;
            private final ImageView favoriteIndicator;
            private final ImageView downloadedIndicator;
            private final TextView titleTextView;
            private final TextView infoTextView;
            
            public FavoriteViewHolder(@NonNull View itemView) {
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