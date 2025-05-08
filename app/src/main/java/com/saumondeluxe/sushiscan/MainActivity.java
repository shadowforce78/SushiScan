package com.saumondeluxe.sushiscan;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private MaterialButton searchButton, libraryButton, myListButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        searchButton = findViewById(R.id.searchButton);
        libraryButton = findViewById(R.id.libraryButton);
        myListButton = findViewById(R.id.myListButton);

        // Set click listeners for the buttons
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch Search Activity
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });

        libraryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch Library Activity
                Toast.makeText(MainActivity.this, "Bibliothèque - Fonctionnalité à venir", Toast.LENGTH_SHORT).show();
                // Intent intent = new Intent(MainActivity.this, LibraryActivity.class);
                // startActivity(intent);
            }
        });

        myListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch MyList Activity
                Toast.makeText(MainActivity.this, "Ma liste - Fonctionnalité à venir", Toast.LENGTH_SHORT).show();
                // Intent intent = new Intent(MainActivity.this, MyListActivity.class);
                // startActivity(intent);
            }
        });
    }
}
