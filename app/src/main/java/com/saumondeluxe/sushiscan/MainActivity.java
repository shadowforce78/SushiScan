package com.saumondeluxe.sushiscan;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {

    ImageView mangaImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mangaImage = findViewById(R.id.mangaImage);

        String imageUrl = "https://upload.wikimedia.org/wikipedia/fr/2/2a/OnePiece72.png";

        Glide.with(this)
                .load(imageUrl)
                .into(mangaImage);
    }
}
