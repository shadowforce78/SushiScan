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
        private readonly CacheService _cacheService;
        private const string BaseUrl = "https://api.saumondeluxe.com";
        private const string ImageBaseUrl = "https://cdn.statically.io/gh/Anime-Sama/IMG/img/contenu/";

        public ApiService()
        {
            _httpClient = new HttpClient
            {
                BaseAddress = new Uri(BaseUrl)
            };
            _cacheService = new CacheService();
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
                
                // S'assurer que l'URL est absolue
                if (!Uri.IsWellFormedUriString(imageUrl, UriKind.Absolute))
                {
                    Console.WriteLine($"URL d'image invalide: {imageUrl}");
                    return null;
                }
                
                // Créer une requête avec des en-têtes personnalisés
                var request = new HttpRequestMessage(HttpMethod.Get, imageUrl);
                request.Headers.Add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                request.Headers.Add("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
                request.Headers.Add("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7");
                
                // Envoi de la requête
                var response = await _httpClient.SendAsync(request);
                
                Console.WriteLine($"Statut du téléchargement de l'image: {response.StatusCode}");
                
                if (!response.IsSuccessStatusCode)
                {
                    Console.WriteLine($"Échec du téléchargement de l'image: {response.StatusCode}");
                    return null;
                }
                
                var contentType = response.Content.Headers.ContentType?.MediaType;
                Console.WriteLine($"Type de contenu de l'image: {contentType}");
                
                var imageData = await response.Content.ReadAsByteArrayAsync();
                Console.WriteLine($"Taille des données image téléchargées: {imageData.Length} octets");
                
                if (imageData.Length == 0)
                {
                    Console.WriteLine("Données d'image vides reçues");
                    return null;
                }
                
                // Création d'un MemoryStream à partir des données téléchargées
                using var memoryStream = new MemoryStream(imageData);
                
                try
                {
                    // Création d'un Bitmap à partir du MemoryStream
                    var bitmap = new Bitmap(memoryStream);
                    Console.WriteLine($"Image téléchargée et convertie en Bitmap avec succès: {bitmap.Size.Width}x{bitmap.Size.Height}");
                    return bitmap;
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Erreur lors de la création du Bitmap: {ex.Message}");
                    return null;
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors du téléchargement de l'image: {ex.Message}");
                if (ex.InnerException != null)
                {
                    Console.WriteLine($"Exception interne: {ex.InnerException.Message}");
                }
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

        // Méthode pour récupérer les détails d'un manga
        public async Task<MangaDetail?> GetMangaDetailAsync(string title)
        {
            try
            {
                if (string.IsNullOrWhiteSpace(title))
                {
                    Console.WriteLine("Titre du manga non fourni pour les détails");
                    return null;
                }
                
                // Convertir le titre pour l'URL
                string encodedTitle = Uri.EscapeDataString(title);
                string url = $"/scans/manga/{encodedTitle}";
                
                Console.WriteLine($"Récupération des détails du manga: {title}");
                Console.WriteLine($"URL de l'API: {BaseUrl}{url}");
                
                var response = await _httpClient.GetAsync(url);
                Console.WriteLine($"Statut de la réponse: {response.StatusCode}");
                
                if (!response.IsSuccessStatusCode)
                {
                    Console.WriteLine($"Échec de la récupération des détails: {response.StatusCode}");
                    return null;
                }
                
                var content = await response.Content.ReadAsStringAsync();
                
                // Affichage du contenu pour le débogage (limité à 500 caractères)
                Console.WriteLine($"Contenu de la réponse: {content.Substring(0, Math.Min(500, content.Length))}...");
                
                var options = new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                };
                
                // Désérialiser la réponse
                var mangaDetail = JsonSerializer.Deserialize<MangaDetail>(content, options);
                
                if (mangaDetail == null)
                {
                    Console.WriteLine("La désérialisation des détails du manga a renvoyé null");
                    return null;
                }
                
                // Télécharger l'image du manga si une URL est fournie
                if (!string.IsNullOrEmpty(mangaDetail.ImageUrl))
                {
                    mangaDetail.Image = await DownloadImageAsync(mangaDetail.ImageUrl);
                }
                else
                {
                    // Si pas d'URL d'image fournie, générer une URL basée sur le titre
                    string slug = GenerateSlug(mangaDetail.Title);
                    string imageUrl = $"{ImageBaseUrl}{slug}.jpg";
                    mangaDetail.ImageUrl = imageUrl;
                    mangaDetail.Image = await DownloadImageAsync(imageUrl);
                }
                
                Console.WriteLine($"Détails du manga récupérés avec succès: {mangaDetail.Title}");
                return mangaDetail;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"ERREUR lors de la récupération des détails du manga: {ex.GetType().Name} - {ex.Message}");
                Console.WriteLine($"Stack trace: {ex.StackTrace}");
                return null;
            }
        }

        // Méthode pour récupérer les détails d'un chapitre
        public async Task<ChapterDetail?> GetChapterDetailAsync(string mangaTitle, string scanName, string chapterNumber, IProgress<(int index, Bitmap? page)>? progressCallback = null)
        {
            try
            {
                if (string.IsNullOrWhiteSpace(mangaTitle) || string.IsNullOrWhiteSpace(scanName) || string.IsNullOrWhiteSpace(chapterNumber))
                {
                    Console.WriteLine("Informations manquantes pour récupérer les détails du chapitre.");
                    return null;
                }

                Console.WriteLine($"Vérification du cache pour le chapitre: {mangaTitle} - {scanName} - Chapitre {chapterNumber}");
                
                // Vérifier d'abord si le chapitre est déjà en cache
                if (_cacheService.IsChapterCached(mangaTitle, scanName, chapterNumber))
                {
                    Console.WriteLine("Chapitre trouvé en cache, chargement depuis le cache...");
                    var cachedChapter = await _cacheService.LoadChapterFromCacheAsync(mangaTitle, scanName, chapterNumber, progressCallback);
                    
                    if (cachedChapter != null && cachedChapter.Pages.Count > 0)
                    {
                        Console.WriteLine($"Chapitre chargé depuis le cache avec succès: {cachedChapter.Pages.Count} pages");
                        return cachedChapter;
                    }
                    else
                    {
                        Console.WriteLine("Le cache semble corrompu, téléchargement du chapitre depuis l'API...");
                    }
                }
                else
                {
                    Console.WriteLine("Chapitre non trouvé en cache, téléchargement depuis l'API...");
                }

                // Si le chapitre n'est pas en cache ou si le cache est corrompu, télécharger depuis l'API
                string encodedTitle = Uri.EscapeDataString(mangaTitle);
                string encodedScanName = Uri.EscapeDataString(scanName);
                string encodedChapterNumber = Uri.EscapeDataString(chapterNumber);
                
                string url = $"/scans/chapter?title={encodedTitle}&scan_name={encodedScanName}&chapter_number={encodedChapterNumber}";
                
                Console.WriteLine($"URL de l'API: {BaseUrl}{url}");

                var response = await _httpClient.GetAsync(url);
                Console.WriteLine($"Statut de la réponse: {response.StatusCode}");

                if (!response.IsSuccessStatusCode)
                {
                    Console.WriteLine($"Échec de la récupération des détails du chapitre: {response.StatusCode}");
                    return null;
                }

                var content = await response.Content.ReadAsStringAsync();
                Console.WriteLine($"Contenu de la réponse (chapitre): {content.Substring(0, Math.Min(500, content.Length))}...");

                var options = new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                };

                var chapterDetail = JsonSerializer.Deserialize<ChapterDetail>(content, options);

                if (chapterDetail == null)
                {
                    Console.WriteLine("La désérialisation des détails du chapitre a renvoyé null.");
                    return null;
                }

                // Télécharger les images des pages
                if (chapterDetail.ImageUrls != null && chapterDetail.ImageUrls.Count > 0)
                {
                    Console.WriteLine($"Téléchargement de {chapterDetail.ImageUrls.Count} pages pour le chapitre...");
                    
                    // Pré-remplir avec des pages nulles pour conserver l'ordre
                    for (int i = 0; i < chapterDetail.ImageUrls.Count; i++)
                    {
                        chapterDetail.Pages.Add(null);
                    }
                    
                    // Téléchargement asynchrone des images avec notification de progression
                    var downloadTasks = new List<Task>();
                    
                    for (int i = 0; i < chapterDetail.ImageUrls.Count; i++)
                    {
                        int pageIndex = i; // Capture de la variable pour éviter les problèmes de closure
                        string imageUrl = chapterDetail.ImageUrls[i];
                        
                        var task = Task.Run(async () =>
                        {
                            var imageBitmap = await DownloadImageAsync(imageUrl);
                            chapterDetail.Pages[pageIndex] = imageBitmap;
                            
                            // Notifier la progression
                            progressCallback?.Report((pageIndex, imageBitmap));
                        });
                        
                        downloadTasks.Add(task);
                    }
                    
                    // Attendre que toutes les images soient téléchargées
                    await Task.WhenAll(downloadTasks);
                    
                    Console.WriteLine("Toutes les pages du chapitre ont été téléchargées.");
                    
                    // Sauvegarder le chapitre dans le cache pour les prochaines consultations
                    await _cacheService.SaveChapterToCacheAsync(chapterDetail);
                }
                else
                {
                    Console.WriteLine("Aucune URL d'image trouvée pour les pages du chapitre.");
                }
                
                Console.WriteLine($"Détails du chapitre récupérés : {chapterDetail.MangaTitle} - Chapitre {chapterDetail.Number}");
                return chapterDetail;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"ERREUR lors de la récupération des détails du chapitre: {ex.GetType().Name} - {ex.Message}");
                Console.WriteLine($"Stack trace: {ex.StackTrace}");
                return null;
            }
        }
    }
}
