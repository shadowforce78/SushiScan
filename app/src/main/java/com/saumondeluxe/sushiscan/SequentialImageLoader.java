package com.saumondeluxe.sushiscan;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Classe qui gère le chargement séquentiel et optimisé des images
 * Cette classe priorise le chargement des images visibles et précharge
 * les prochaines images pour une expérience de lecture fluide
 */
public class SequentialImageLoader {
    private static final String TAG = "SequentialImageLoader";
    private static final int PRELOAD_AHEAD_COUNT = 3; // Nombre d'images à précharger en avance
    private static final int MAX_CONCURRENT_LOADS = 2; // Limite de chargements simultanés

    private final Context context;
    private final List<String> imageUrls;
    private final Map<Integer, Boolean> loadingStatus; // true = chargé, false = en erreur
    private final Map<Integer, Boolean> loadingQueue; // images en cours de chargement
    private int visiblePosition = 0;
    private final ExecutorService executor;
    private final Handler mainHandler;

    // Écouteur pour notifier quand une image est chargée
    private final List<LoadingStateListener> listeners = new ArrayList<>();

    public interface LoadingStateListener {
        void onImageLoaded(int position, boolean success);
    }

    public SequentialImageLoader(Context context, List<String> imageUrls) {
        this.context = context.getApplicationContext();
        this.imageUrls = imageUrls;
        this.loadingStatus = new HashMap<>();
        this.loadingQueue = new HashMap<>();
        this.executor = Executors.newFixedThreadPool(MAX_CONCURRENT_LOADS);
        this.mainHandler = new Handler(Looper.getMainLooper());

        // Précharger les premières images
        updateVisiblePosition(0);
    }

    public void addLoadingStateListener(LoadingStateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeLoadingStateListener(LoadingStateListener listener) {
        listeners.remove(listener);
    }

    /**
     * Met à jour la position visible et gère le chargement prioritaire
     */
    public void updateVisiblePosition(int newPosition) {
        this.visiblePosition = newPosition;
        scheduleLoading();
    }

    /**
     * Planifie le chargement des images en fonction de la position actuelle
     */
    private void scheduleLoading() {
        // Limiter le nombre de chargements simultanés
        if (loadingQueue.size() >= MAX_CONCURRENT_LOADS) {
            return;
        }

        // Trouver les positions à charger prioritairement
        List<Integer> positionsToLoad = new ArrayList<>();

        // D'abord l'image visible
        if (!isLoaded(visiblePosition) && !isLoading(visiblePosition)) {
            positionsToLoad.add(visiblePosition);
        }

        // Ensuite les images suivantes à précharger
        for (int i = 1; i <= PRELOAD_AHEAD_COUNT; i++) {
            int position = visiblePosition + i;
            if (position < imageUrls.size() && !isLoaded(position) && !isLoading(position)) {
                positionsToLoad.add(position);
            }
        }

        // Précharger les images précédentes aussi (1 avant)
        int prevPosition = visiblePosition - 1;
        if (prevPosition >= 0 && !isLoaded(prevPosition) && !isLoading(prevPosition)) {
            positionsToLoad.add(prevPosition);
        }

        // Lancer les chargements dans l'ordre priorisé
        for (int position : positionsToLoad) {
            if (loadingQueue.size() < MAX_CONCURRENT_LOADS) {
                loadImage(position);
            }
        }
    }

    /**
     * Charge une image spécifique en utilisant Glide
     */
    private void loadImage(int position) {
        if (position < 0 || position >= imageUrls.size()) {
            return;
        }

        // Marquer comme en cours de chargement
        loadingQueue.put(position, true);

        // Obtenir l'URL de l'image
        String url = imageUrls.get(position);

        executor.execute(() -> {
            try {
                // Vérifier s'il s'agit d'une image locale
                if (url.startsWith("file://")) {
                    // Charger l'image locale
                    loadLocalImage(url, position);
                } else {
                    // Charger l'image à partir d'Internet
                    loadRemoteImage(url, position);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du chargement de l'image à la position " + position + ": " + e.getMessage());
                notifyLoadingComplete(position, false);
            }
        });
    }

    /**
     * Charge une image locale
     */
    private void loadLocalImage(String url, int position) {
        String localPath = url.substring(7); // Enlever "file://"

        mainHandler.post(() -> {
            Glide.with(context)
                    .load(localPath)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Pas besoin de cache pour les fichiers locaux
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                Target<Drawable> target, boolean isFirstResource) {
                            notifyLoadingComplete(position, false);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                Target<Drawable> target, DataSource dataSource,
                                boolean isFirstResource) {
                            notifyLoadingComplete(position, true);
                            return false;
                        }
                    })
                    .preload();
        });
    }

    /**
     * Charge une image distante avec les différentes méthodes de fallback
     */
    private void loadRemoteImage(String url, int position) {
        // Créer les différentes URLs pour tenter plusieurs méthodes de chargement
        GlideUrl glideUrl = DriveImageLoader.getGlideUrl(url);
        String alternativeUrl = DriveImageLoader.getAlternativeUrl(url);
        String highQualityUrl = DriveImageLoader.getHighQualityUrl(url);

        mainHandler.post(() -> {
            // Tenter le chargement avec la première URL
            Glide.with(context)
                    .load(glideUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .timeout(30000)
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource,
                                @Nullable Transition<? super Drawable> transition) {
                            notifyLoadingComplete(position, true);
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            // Essayer avec l'URL alternative
                            tryAlternativeUrl(alternativeUrl, highQualityUrl, position);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Ne rien faire
                        }
                    });
        });
    }

    /**
     * Essaie de charger avec l'URL alternative
     */
    private void tryAlternativeUrl(String alternativeUrl, String highQualityUrl, int position) {
        Glide.with(context)
                .load(alternativeUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(30000)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource,
                            @Nullable Transition<? super Drawable> transition) {
                        notifyLoadingComplete(position, true);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        // Essayer avec l'URL haute qualité
                        tryHighQualityUrl(highQualityUrl, position);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Ne rien faire
                    }
                });
    }

    /**
     * Essaie de charger avec l'URL haute qualité
     */
    private void tryHighQualityUrl(String highQualityUrl, int position) {
        Glide.with(context)
                .load(highQualityUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(30000)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource,
                            @Nullable Transition<? super Drawable> transition) {
                        notifyLoadingComplete(position, true);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        notifyLoadingComplete(position, false);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Ne rien faire
                    }
                });
    }

    /**
     * Notifie que le chargement d'une image est terminé
     */
    private void notifyLoadingComplete(int position, boolean success) {
        mainHandler.post(() -> {
            // Mettre à jour les états
            loadingStatus.put(position, success);
            loadingQueue.remove(position);

            // Notifier les écouteurs
            for (LoadingStateListener listener : listeners) {
                listener.onImageLoaded(position, success);
            }

            // Essayer de charger plus d'images
            scheduleLoading();
        });
    }

    /**
     * Vérifie si une image est déjà chargée
     */
    public boolean isLoaded(int position) {
        return loadingStatus.containsKey(position) && loadingStatus.get(position);
    }

    /**
     * Vérifie si une image est en cours de chargement
     */
    public boolean isLoading(int position) {
        return loadingQueue.containsKey(position);
    }
}