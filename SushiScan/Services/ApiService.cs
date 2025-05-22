using System;
using System.Collections.Generic;
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
            
            // Remplacer les caractères accentués par leurs équivalents sans accent
            slug = Regex.Replace(slug, "[éèêë]", "e");
            slug = Regex.Replace(slug, "[àâä]", "a");
            slug = Regex.Replace(slug, "[îï]", "i");
            slug = Regex.Replace(slug, "[ôö]", "o");
            slug = Regex.Replace(slug, "[ùûü]", "u");
            slug = Regex.Replace(slug, "ç", "c");
            
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

        // Méthode pour rechercher un manga par titre
        public async Task<List<MangaSearchResult>> SearchMangaAsync(string title)
        {
            try
            {
                if (string.IsNullOrWhiteSpace(title))
                {
                    return new List<MangaSearchResult>();
                }

                Console.WriteLine($"Recherche de manga avec le titre: {title}");
                
                // Convertir le titre pour l'URL et construire l'URL avec paramètre de recherche
                string encodedTitle = Uri.EscapeDataString(title);
                string url = $"/scans/manga/search?title={encodedTitle}";
                
                Console.WriteLine($"URL de recherche: {BaseUrl}{url}");
                
                var response = await _httpClient.GetAsync(url);
                Console.WriteLine($"Statut de la réponse: {response.StatusCode}");
                
                if (!response.IsSuccessStatusCode)
                {
                    Console.WriteLine($"Échec de la recherche: {response.StatusCode}");
                    return new List<MangaSearchResult>();
                }
                
                var content = await response.Content.ReadAsStringAsync();
                
                // Affichage du contenu pour le débogage
                Console.WriteLine($"Contenu de la réponse: {content.Substring(0, Math.Min(500, content.Length))}...");

                var options = new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                };
                
                List<MangaSearchResult> searchResults;
                
                // Essayer de déterminer si la réponse est un objet unique ou un tableau
                if (content.StartsWith("[") && content.EndsWith("]"))
                {
                    // La réponse est un tableau
                    searchResults = JsonSerializer.Deserialize<List<MangaSearchResult>>(content, options) ?? new List<MangaSearchResult>();
                    Console.WriteLine("Format détecté: Tableau");
                }
                else if (content.StartsWith("{") && content.EndsWith("}"))
                {
                    // La réponse est un objet unique ou un wrapper
                    try
                    {
                        // Tenter de désérialiser comme un objet unique
                        var singleResult = JsonSerializer.Deserialize<MangaSearchResult>(content, options);
                        searchResults = singleResult != null ? new List<MangaSearchResult> { singleResult } : new List<MangaSearchResult>();
                        Console.WriteLine("Format détecté: Objet unique");
                    }
                    catch
                    {
                        // Peut-être un wrapper avec une propriété contenant les résultats
                        try 
                        {
                            var responseObject = JsonDocument.Parse(content);
                            searchResults = new List<MangaSearchResult>();
                            
                            foreach (var property in responseObject.RootElement.EnumerateObject())
                            {
                                Console.WriteLine($"Propriété trouvée dans la réponse: {property.Name}");
                                if (property.Value.ValueKind == JsonValueKind.Array)
                                {
                                    // Extraire le tableau de cette propriété
                                    var arrayJson = property.Value.GetRawText();
                                    var results = JsonSerializer.Deserialize<List<MangaSearchResult>>(arrayJson, options);
                                    if (results != null)
                                    {
                                        searchResults.AddRange(results);
                                    }
                                    Console.WriteLine($"Résultats extraits de la propriété '{property.Name}'");
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            Console.WriteLine($"Erreur lors de l'analyse du JSON: {ex.Message}");
                            return new List<MangaSearchResult>();
                        }
                    }
                }
                else
                {
                    Console.WriteLine("Format JSON non reconnu");
                    return new List<MangaSearchResult>();
                }
                
                Console.WriteLine($"Résultats trouvés: {searchResults.Count}");
                
                // Télécharger les images pour chaque résultat
                foreach (var result in searchResults)
                {
                    if (!string.IsNullOrEmpty(result.ImageUrl))
                    {
                        result.Image = await DownloadImageAsync(result.ImageUrl);
                    }
                    else
                    {
                        // Si pas d'URL d'image fournie, générer une URL basée sur le titre
                        string slug = GenerateSlug(result.Title);
                        string imageUrl = $"{ImageBaseUrl}{slug}.jpg";
                        result.ImageUrl = imageUrl;
                        result.Image = await DownloadImageAsync(imageUrl);
                    }
                }
                
                return searchResults;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"ERREUR recherche: {ex.GetType().Name} - {ex.Message}");
                Console.WriteLine($"Stack trace: {ex.StackTrace}");
                return new List<MangaSearchResult>();
            }
        }
    }
}

