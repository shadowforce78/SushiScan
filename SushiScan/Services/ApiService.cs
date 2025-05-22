using System;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;
using System.Text.RegularExpressions;
using System.IO;
using Avalonia.Media.Imaging;
using SushiScan.Models;

namespace SushiScan.Services
{
    public class ApiService
    {
        private readonly HttpClient _httpClient;
        private const string BaseUrl = "https://api.saumondeluxe.com";
        private const string ImageBaseUrl = "https://cdn.statically.io/gh/Anime-Sama/IMG/img/contenu/";

        public ApiService()
        {
            _httpClient = new HttpClient
            {
                BaseAddress = new Uri(BaseUrl)
            };
        }

        // Méthode pour générer un slug à partir du titre
        private string GenerateSlug(string title)
        {
            // Convertir en minuscules et remplacer les espaces par des tirets
            string slug = title.ToLower().Trim();
            
            // Remplacer les caractères spéciaux et les espaces par des tirets
            slug = Regex.Replace(slug, @"[^a-z0-9\s-]", "");
            slug = Regex.Replace(slug, @"[\s-]+", "-");
            
            return slug;
        }

        // Méthode pour télécharger et convertir une image en Bitmap
        private async Task<Bitmap?> DownloadImageAsync(string imageUrl)
        {
            try
            {
                Console.WriteLine($"Téléchargement de l'image: {imageUrl}");
                var response = await _httpClient.GetAsync(imageUrl);
                
                if (!response.IsSuccessStatusCode)
                {
                    Console.WriteLine($"Échec du téléchargement de l'image: {response.StatusCode}");
                    return null;
                }
                
                var imageData = await response.Content.ReadAsByteArrayAsync();
                
                // Création d'un MemoryStream à partir des données téléchargées
                using var memoryStream = new MemoryStream(imageData);
                
                // Création d'un Bitmap à partir du MemoryStream
                var bitmap = new Bitmap(memoryStream);
                Console.WriteLine("Image téléchargée et convertie en Bitmap avec succès");
                return bitmap;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors du téléchargement de l'image: {ex.Message}");
                return null;
            }
        }

        public async Task<HomePageData?> GetHomePageDataAsync()
        {
            try
            {
                Console.WriteLine("Début de la récupération des données API...");
                Console.WriteLine($"URL de l'API: {BaseUrl}/scans/homepage");
                
                var response = await _httpClient.GetAsync("/scans/homepage");
                Console.WriteLine($"Statut de la réponse: {response.StatusCode}");
                
                response.EnsureSuccessStatusCode();
                
                var content = await response.Content.ReadAsStringAsync();
                Console.WriteLine($"Contenu de la réponse (50 premiers caractères): {content.Substring(0, Math.Min(50, content.Length))}...");
                
                var options = new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                };
                
                Console.WriteLine("Désérialisation du JSON...");
                var homePageData = JsonSerializer.Deserialize<HomePageData>(content, options);
                
                if (homePageData != null)
                {
                    Console.WriteLine($"Données récupérées: Trending: {homePageData.Trending.Count}, Popular: {homePageData.Popular.Count}, Recommended: {homePageData.Recommended.Count}");
                    
                    // Générer les URL d'images et télécharger les images pour chaque manga
                    foreach (var manga in homePageData.Trending)
                    {
                        string slug = GenerateSlug(manga.Name);
                        string imageUrl = $"{ImageBaseUrl}{slug}.jpg";
                        manga.ImagePath = imageUrl;
                        manga.Image = await DownloadImageAsync(imageUrl);
                        Console.WriteLine($"Manga trending: {manga.Name}, URL image: {imageUrl}");
                    }
                    
                    foreach (var manga in homePageData.Popular)
                    {
                        string slug = GenerateSlug(manga.Name);
                        string imageUrl = $"{ImageBaseUrl}{slug}.jpg";
                        manga.ImagePath = imageUrl;
                        manga.Image = await DownloadImageAsync(imageUrl);
                        Console.WriteLine($"Manga popular: {manga.Name}, URL image: {imageUrl}");
                    }
                    
                    foreach (var manga in homePageData.Recommended)
                    {
                        string slug = GenerateSlug(manga.Name);
                        string imageUrl = $"{ImageBaseUrl}{slug}.jpg";
                        manga.ImagePath = imageUrl;
                        manga.Image = await DownloadImageAsync(imageUrl);
                        Console.WriteLine($"Manga recommended: {manga.Name}, URL image: {imageUrl}");
                    }
                }
                else
                {
                    Console.WriteLine("La désérialisation a renvoyé null");
                }
                
                return homePageData;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"ERREUR API: {ex.GetType().Name} - {ex.Message}");
                if (ex.InnerException != null)
                {
                    Console.WriteLine($"Exception interne: {ex.InnerException.Message}");
                }
                Console.WriteLine($"Stack trace: {ex.StackTrace}");
                return null;
            }
        }
    }
}
