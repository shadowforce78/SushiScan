package com.saumondeluxe.sushiscan;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private ImageView mangaImage;
    private EditText searchEditText;
    private Button searchButton;
    private TextView resultsTextView;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialiser les vues
        mangaImage = findViewById(R.id.mangaImage);
        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        resultsTextView = findViewById(R.id.resultsTextView);

        // Initialiser le client HTTP
        client = new OkHttpClient();

        // Image par défaut
        String imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/50/Yes_Check_Circle.svg/2048px-Yes_Check_Circle.svg.png";
        Glide.with(this).load(imageUrl).into(mangaImage);

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

    private void searchAnime(String query) {
        // Construire l'URL de l'API avec le terme de recherche
        String url = "https://api.saumondeluxe.com/scans/get_info_from_search/" + query;

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
                            // Afficher les données brutes dans le TextView
                            resultsTextView.setText(responseData);
                            resultsTextView.setVisibility(View.VISIBLE);

                            // Parser les données JSON
                            JSONObject jsonObject = new JSONObject(responseData);

                            // Extraire l'URL de l'image
                            if (jsonObject.has("image_url")) {
                                String imageUrl = jsonObject.getString("image_url");

                                // Charger l'image avec Glide
                                Glide.with(MainActivity.this)
                                        .load(imageUrl)
                                        .into(mangaImage);
                            }
                        } catch (JSONException e) {
                            Toast.makeText(MainActivity.this, "Erreur de parsing JSON: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }
}
