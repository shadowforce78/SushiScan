package com.saumondeluxe.sushiscan.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.saumondeluxe.sushiscan.R;
import com.saumondeluxe.sushiscan.database.MangaEntity;

import java.util.ArrayList;
import java.util.List;

public class MangaGridAdapter extends RecyclerView.Adapter<MangaGridAdapter.MangaViewHolder> {

    private final Context context;
    private final List<MangaEntity> mangaList;
    private final OnMangaClickListener listener;

    public interface OnMangaClickListener {
        void onMangaClick(MangaEntity manga, int position);
    }

    public MangaGridAdapter(Context context, OnMangaClickListener listener) {
        this.context = context;
        this.mangaList = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public MangaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_manga_grid, parent, false);
        return new MangaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MangaViewHolder holder, int position) {
        MangaEntity manga = mangaList.get(position);
        holder.bind(manga, position);
    }

    @Override
    public int getItemCount() {
        return mangaList.size();
    }

    public void setMangaList(List<MangaEntity> mangaList) {
        this.mangaList.clear();
        if (mangaList != null) {
            this.mangaList.addAll(mangaList);
        }
        notifyDataSetChanged();
    }

    public List<MangaEntity> getMangaList() {
        return mangaList;
    }

    public void addManga(MangaEntity manga) {
        mangaList.add(manga);
        notifyItemInserted(mangaList.size() - 1);
    }

    public void updateManga(MangaEntity manga, int position) {
        if (position >= 0 && position < mangaList.size()) {
            mangaList.set(position, manga);
            notifyItemChanged(position);
        }
    }

    public void removeManga(int position) {
        if (position >= 0 && position < mangaList.size()) {
            mangaList.remove(position);
            notifyItemRemoved(position);
        }
    }

    class MangaViewHolder extends RecyclerView.ViewHolder {
        private final ImageView coverImageView;
        private final TextView titleTextView;
        private final TextView infoTextView;
        private final ImageView favoriteIndicator;
        private final ImageView downloadedIndicator;

        public MangaViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.mangaCoverImageView);
            titleTextView = itemView.findViewById(R.id.mangaTitleTextView);
            infoTextView = itemView.findViewById(R.id.mangaInfoTextView);
            favoriteIndicator = itemView.findViewById(R.id.favoriteIndicatorImageView);
            downloadedIndicator = itemView.findViewById(R.id.downloadedIndicatorImageView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMangaClick(mangaList.get(position), position);
                }
            });
        }

        void bind(MangaEntity manga, int position) {
            titleTextView.setText(manga.getName());
            
            // Afficher les genres et le dernier chapitre lu
            String infoText = "";
            if (manga.getGenres() != null && !manga.getGenres().isEmpty()) {
                infoText = String.join(", ", manga.getGenres().subList(0, Math.min(2, manga.getGenres().size())));
            }
            
            if (manga.getLastReadChapterNumber() > 0) {
                if (!infoText.isEmpty()) {
                    infoText += " • ";
                }
                infoText += "Chapitre " + manga.getLastReadChapterNumber();
            }
            
            infoTextView.setText(infoText);
            
            // Charger l'image de couverture
            if (manga.getImageUrl() != null && !manga.getImageUrl().isEmpty()) {
                Glide.with(context)
                    .load(manga.getImageUrl())
                    .apply(new RequestOptions()
                        .placeholder(R.drawable.placeholder_cover)
                        .error(R.drawable.error_cover))
                    .into(coverImageView);
            } else {
                coverImageView.setImageResource(R.drawable.placeholder_cover);
            }
            
            // Afficher les indicateurs de favori et de téléchargement
            favoriteIndicator.setVisibility(manga.isFavorite() ? View.VISIBLE : View.GONE);
            downloadedIndicator.setVisibility(manga.isDownloaded() ? View.VISIBLE : View.GONE);
        }
    }
}