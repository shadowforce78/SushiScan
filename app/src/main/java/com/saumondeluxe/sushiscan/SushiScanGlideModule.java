package com.saumondeluxe.sushiscan;

import android.content.Context;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

@GlideModule
public class SushiScanGlideModule extends AppGlideModule {

    private static final int MEMORY_CACHE_SIZE = 1024 * 1024 * 20; // 20 MB
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 500; // 500 MB
    private static final String DISK_CACHE_FOLDER = "image_cache";

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // Configuration du cache mémoire
        builder.setMemoryCache(new LruResourceCache(MEMORY_CACHE_SIZE));

        // Configuration du cache disque
        String diskCachePath = context.getCacheDir().getPath() + File.separator + DISK_CACHE_FOLDER;
        builder.setDiskCache(new DiskLruCacheFactory(diskCachePath, DISK_CACHE_SIZE));

        // Configuration par défaut des requêtes
        builder.setDefaultRequestOptions(
                new RequestOptions()
                        .format(DecodeFormat.PREFER_ARGB_8888)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .skipMemoryCache(false));
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        // Créer un client OkHttp avec un timeout plus long (30 secondes)
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true);

        // Remplacer le chargeur par défaut par notre client OkHttp personnalisé
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(builder.build()));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}