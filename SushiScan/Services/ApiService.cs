using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;
using System.Text.RegularExpressions;
using System.IO;
using Avalonia.Media.Imaging;
using SushiScan.Models;
using System.Threading;
using System.Net;

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
            var handler = new HttpClientHandler
            {
                UseCookies = true,
                CookieContainer = new CookieContainer(),
                AllowAutoRedirect = true // S'assurer que les redirections sont suivies
            };
            _httpClient = new HttpClient(handler)
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
            {                string effectiveImageUrl = imageUrl;                // Vérifier si c'est une URL Google Drive et la transformer pour téléchargement direct
                if (imageUrl.Contains("drive.google.com"))
                {
                    string? fileId = null;
                    
                    // Extraire l'ID selon différents formats d'URL Google Drive
                    if (imageUrl.Contains("uc?export=view&id=") || imageUrl.Contains("uc?export=download&id="))
                    {
                        var match = Regex.Match(imageUrl, @"[?&]id=([^&]+)");
                        if (match.Success)
                        {
                            fileId = match.Groups[1].Value;
                        }
                    }
                    else if (imageUrl.Contains("drive.google.com/file/d/"))
                    {
                        var match = Regex.Match(imageUrl, @"drive\.google\.com/file/d/([^/]+)/");
                        if (match.Success)
                        {
                            fileId = match.Groups[1].Value;
                        }
                    }
                    else if (imageUrl.Contains("drive.google.com/open?id="))
                    {
                        var match = Regex.Match(imageUrl, @"[?&]id=([^&]+)");
                        if (match.Success)
                        {
                            fileId = match.Groups[1].Value;
                        }
                    }
                    
                    if (!string.IsNullOrEmpty(fileId))
                    {
                        // Essayer plusieurs méthodes Google Drive pour télécharger l'image
                        var googleDriveUrls = new[]
                        {
                            $"https://drive.google.com/uc?export=download&id={fileId}&confirm=t",
                            $"https://drive.google.com/thumbnail?id={fileId}&sz=w2000",
                            $"https://lh3.googleusercontent.com/d/{fileId}=w2000",
                            $"https://drive.google.com/uc?id={fileId}&export=download&authuser=0&confirm=t&uuid={Guid.NewGuid()}"
                        };
                        
                        Console.WriteLine($"ID Google Drive extrait: {fileId}");
                        Console.WriteLine($"URL originale: {imageUrl}");
                        
                        // Essayer chaque URL jusqu'à ce qu'une fonctionne
                        foreach (var testUrl in googleDriveUrls)
                        {
                            Console.WriteLine($"Test de l'URL Google Drive: {testUrl}");
                            effectiveImageUrl = testUrl;
                            
                            try
                            {
                                var testRequest = new HttpRequestMessage(HttpMethod.Head, effectiveImageUrl);
                                testRequest.Headers.Add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                                
                                var testResponse = await _httpClient.SendAsync(testRequest);
                                var testContentType = testResponse.Content.Headers.ContentType?.MediaType;
                                
                                Console.WriteLine($"Test URL - Status: {testResponse.StatusCode}, Content-Type: {testContentType}");
                                
                                if (testResponse.IsSuccessStatusCode && testContentType != null && testContentType.StartsWith("image/"))
                                {
                                    Console.WriteLine($"URL Google Drive fonctionnelle trouvée: {effectiveImageUrl}");
                                    break;
                                }
                            }
                            catch (Exception testEx)
                            {
                                Console.WriteLine($"Erreur lors du test de l'URL: {testEx.Message}");
                                continue;
                            }
                        }
                    }
                }

                Console.WriteLine($"Téléchargement de l'image: {effectiveImageUrl}");
                
                // S'assurer que l'URL est absolue
                if (!Uri.IsWellFormedUriString(effectiveImageUrl, UriKind.Absolute))
                {
                    Console.WriteLine($"URL d'image invalide: {effectiveImageUrl}");
                    return null;
                }
                
                // Créer une requête avec des en-têtes personnalisés
                var request = new HttpRequestMessage(HttpMethod.Get, effectiveImageUrl);
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
        
        // Méthode pour obtenir l'image de couverture d'un manga (avec cache)
        private async Task<Bitmap?> GetMangaCoverAsync(string mangaTitle)
        {
            try
            {
                // 1. Vérifier si l'image est en cache
                Bitmap? coverImage = _cacheService.LoadCoverFromCache(mangaTitle);
                if (coverImage != null)
                {
                    Console.WriteLine($"Couverture chargée depuis le cache pour: {mangaTitle}");
                    return coverImage;
                }
                
                // 2. Si non, télécharger l'image
                string slug = GenerateSlug(mangaTitle);
                string imageUrl = $"{ImageBaseUrl}{slug}.jpg";
                
                coverImage = await DownloadImageAsync(imageUrl);
                
                // 3. Si le téléchargement a réussi, mettre en cache
                if (coverImage != null)
                {
                    await _cacheService.SaveCoverToCacheAsync(mangaTitle, coverImage);
                    Console.WriteLine($"Couverture téléchargée et mise en cache pour: {mangaTitle}");
                }
                
                return coverImage;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors de la récupération de la couverture: {ex.Message}");
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
                    
                    // Créer une liste de tâches pour charger toutes les couvertures
                    var coverTasks = new List<Task>();

                    // S'assurer que assignCoverAction est bien Func<Manga, Task>
                    Func<Manga, Task> assignCoverAction = async (manga) =>
                    {
                        string slug = GenerateSlug(manga.Name);
                        string imageUrl = $"{ImageBaseUrl}{slug}.jpg";
                        manga.ImagePath = imageUrl;
                        manga.Image = await GetMangaCoverAsync(manga.Name); // Conserve l'appel existant qui gère le cache
                        Console.WriteLine($"Manga (parallèle): {manga.Name}, URL image: {imageUrl}, Image chargée: {manga.Image != null}");
                    };

                    foreach (var manga in homePageData.Trending)
                    {
                        coverTasks.Add(assignCoverAction(manga));
                    }
                    
                    foreach (var manga in homePageData.Popular)
                    {
                        coverTasks.Add(assignCoverAction(manga));
                    }
                    
                    foreach (var manga in homePageData.Recommended)
                    {
                        coverTasks.Add(assignCoverAction(manga));
                    }

                    // Attendre que toutes les tâches de chargement de couverture soient terminées
                    await Task.WhenAll(coverTasks);
                    Console.WriteLine("Toutes les couvertures de la page d'accueil ont été traitées.");
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
                        // Si cela échoue, ce pourrait être un objet wrapper
                        // Note: Cette partie devrait être adaptée à la structure réelle de l'API
                        Console.WriteLine("Échec de la désérialisation comme objet unique, tentative comme objet wrapper");
                        searchResults = new List<MangaSearchResult>();
                    }
                }
                else
                {
                    Console.WriteLine("Format de réponse non reconnu");
                    searchResults = new List<MangaSearchResult>();
                }
                
                // Télécharger les images pour chaque résultat
                var searchCoverTasks = new List<Task>();
                foreach (var result in searchResults)
                {
                    searchCoverTasks.Add(Task.Run(async () => 
                    {
                        // Utiliser result.Title au lieu de result.Name
                        result.Image = await GetMangaCoverAsync(result.Title);
                        Console.WriteLine($"Couverture chargée pour le résultat de recherche: {result.Title}, Image: {(result.Image != null ? "chargée" : "non chargée")}");
                    }));
                }
                await Task.WhenAll(searchCoverTasks);
                Console.WriteLine("Toutes les couvertures des résultats de recherche ont été traitées.");
                
                return searchResults;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors de la recherche: {ex.Message}");
                return new List<MangaSearchResult>();
            }
        }

        // Méthode pour récupérer les détails d'un manga à partir de son ID
        public async Task<MangaDetail?> GetMangaDetailAsync(string mangaName)
        {
            try
            {
                if (string.IsNullOrWhiteSpace(mangaName))
                {
                    return null;
                }
                
                Console.WriteLine($"Récupération des détails du manga: {mangaName}");
                
                // Générer le slug pour l'URL
                string slug = GenerateSlug(mangaName);
                string url = $"/scans/manga/{slug}";
                
                Console.WriteLine($"URL de l'API: {BaseUrl}{url}");
                
                var response = await _httpClient.GetAsync(url);
                Console.WriteLine($"Statut de la réponse: {response.StatusCode}");
                
                if (!response.IsSuccessStatusCode)
                {
                    Console.WriteLine($"Échec de la récupération des détails: {response.StatusCode}");
                    return null;
                }
                
                var content = await response.Content.ReadAsStringAsync();
                Console.WriteLine($"Contenu de la réponse (50 premiers caractères): {content.Substring(0, Math.Min(50, content.Length))}...");
                
                var options = new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                };
                
                var mangaDetail = JsonSerializer.Deserialize<MangaDetail>(content, options);
                
                if (mangaDetail != null)
                {
                    // Télécharger l'image du manga
                    if (!string.IsNullOrEmpty(mangaDetail.ImageUrl))
                    {
                        string imageUrl = mangaDetail.ImageUrl;
                        
                        // Si l'URL est relative, la convertir en absolue
                        if (!imageUrl.StartsWith("http"))
                        {
                            imageUrl = $"{ImageBaseUrl}{imageUrl}";
                        }
                        
                        mangaDetail.ImageUrl = imageUrl;
                        
                        // Utiliser le cache pour la couverture
                        mangaDetail.Image = await GetMangaCoverAsync(mangaDetail.Title);
                    }
                    
                    Console.WriteLine($"Détails du manga récupérés: {mangaDetail.Title}");
                }
                else
                {
                    Console.WriteLine("La désérialisation a renvoyé null");
                }
                
                return mangaDetail;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors de la récupération des détails: {ex.Message}");
                return null;
            }
        }

        // Méthode pour récupérer les pages d'un chapitre
        public async Task<ChapterDetail?> GetChapterPagesAsync(
            string mangaTitle, 
            string scanName, 
            string chapterNumber,
            IProgress<(int index, Bitmap? page)>? progressCallback = null)
        {
            try
            {
                Console.WriteLine($"Récupération des pages pour {mangaTitle} - {scanName} #{chapterNumber}");
                
                // Vérifier d'abord si le chapitre est en cache
                if (_cacheService.IsChapterCached(mangaTitle, scanName, chapterNumber))
                {
                    Console.WriteLine("Chapitre trouvé en cache, chargement...");
                    var cachedChapter = await _cacheService.LoadChapterFromCacheAsync(
                        mangaTitle, 
                        scanName, 
                        chapterNumber, 
                        progressCallback);
                    
                    if (cachedChapter != null)
                    {
                        Console.WriteLine("Chapitre chargé depuis le cache avec succès");
                        return cachedChapter;
                    }
                    
                    Console.WriteLine("Échec du chargement depuis le cache, récupération depuis l'API...");
                }

                // Générer le slug pour l'URL
                string mangaSlug = GenerateSlug(mangaTitle);
                
                // Vérifier si scanName est déjà "scans" et éviter la duplication
                string scan = scanName.ToLower();
                if (scan == "scans")
                {
                    scan = "";
                }
                
                // Liste des formats d'URL à essayer
                var urlFormats = new List<string>();
                
                // Format original
                if (string.IsNullOrEmpty(scan))
                {
                    urlFormats.Add($"/scans/manga/{mangaSlug}/chapter/{chapterNumber}");
                }
                else
                {
                    urlFormats.Add($"/scans/manga/{mangaSlug}/chapter/{chapterNumber}/{scan}");
                }
                
                // Formats alternatifs
                urlFormats.Add($"/scans/manga/{mangaSlug}/chapter/{chapterNumber}/scan");
                urlFormats.Add($"/scans/manga/{mangaSlug}/chapters/{chapterNumber}");
                urlFormats.Add($"/scans/manga/{mangaSlug}/{chapterNumber}");
                
                // Format avec paramètres de requête (nouvel endpoint documenté)
                string encodedTitle = Uri.EscapeDataString(mangaTitle);
                string encodedScan = Uri.EscapeDataString(scanName);
                string encodedChapter = Uri.EscapeDataString(chapterNumber);
                urlFormats.Add($"/scans/chapter?title={encodedTitle}&scan_name={encodedScan}&chapter_number={encodedChapter}");
                
                // Tester chaque format d'URL jusqu'à ce qu'une réponse réussisse
                HttpResponseMessage? successResponse = null;
                string usedUrl = "";
                
                foreach (var url in urlFormats)
                {
                    Console.WriteLine($"Tentative d'URL: {BaseUrl}{url}");
                    
                    var response = await _httpClient.GetAsync(url);
                    Console.WriteLine($"Statut de la réponse: {response.StatusCode}");
                    
                    if (response.IsSuccessStatusCode)
                    {
                        successResponse = response;
                        usedUrl = url;
                        Console.WriteLine($"URL fonctionnelle trouvée: {BaseUrl}{url}");
                        break;
                    }
                }
                
                if (successResponse == null)
                {
                    Console.WriteLine("Toutes les tentatives d'URL ont échoué");
                    return null;
                }
                
                var content = await successResponse.Content.ReadAsStringAsync();
                Console.WriteLine($"Contenu de la réponse reçu: {content.Length} caractères");
                if (content.Length < 100) 
                {
                    Console.WriteLine($"Contenu reçu (peut aider au débogage): {content}");
                }
                
                var options = new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                };
                
                ChapterDetail? chapterDetail = null;
                
                try 
                {
                    chapterDetail = JsonSerializer.Deserialize<ChapterDetail>(content, options);
                }
                catch (JsonException jsonEx) 
                {
                    Console.WriteLine($"Erreur de désérialisation JSON: {jsonEx.Message}");
                    Console.WriteLine("Tentative de traitement manuel du JSON...");
                    
                    // Tentative de parsing manuel si la structure ne correspond pas exactement
                    try 
                    {
                        var jsonDoc = JsonDocument.Parse(content);
                        var root = jsonDoc.RootElement;
                        
                        chapterDetail = new ChapterDetail 
                        {
                            MangaTitle = mangaTitle,
                            ScanName = scanName,
                            Number = chapterNumber,
                            ImageUrls = new List<string>(),
                            Pages = new List<Bitmap?>()
                        };
                        
                        // Essayer de trouver les URLs d'images dans différentes structures possibles
                        if (root.TryGetProperty("pages", out var pagesElement) && pagesElement.ValueKind == JsonValueKind.Array)
                        {
                            foreach (var page in pagesElement.EnumerateArray())
                            {
                                if (page.ValueKind == JsonValueKind.String)
                                {
                                    chapterDetail.ImageUrls.Add(page.GetString() ?? "");
                                }
                                else if (page.TryGetProperty("url", out var urlProperty) && urlProperty.ValueKind == JsonValueKind.String)
                                {
                                    chapterDetail.ImageUrls.Add(urlProperty.GetString() ?? "");
                                }
                            }
                        }
                        else if (root.TryGetProperty("images", out var imagesElement) && imagesElement.ValueKind == JsonValueKind.Array)
                        {
                            foreach (var image in imagesElement.EnumerateArray())
                            {
                                if (image.ValueKind == JsonValueKind.String)
                                {
                                    chapterDetail.ImageUrls.Add(image.GetString() ?? "");
                                }
                                else if (image.TryGetProperty("url", out var urlProperty) && urlProperty.ValueKind == JsonValueKind.String)
                                {
                                    chapterDetail.ImageUrls.Add(urlProperty.GetString() ?? "");
                                }
                            }
                        }
                        
                        chapterDetail.PageCount = chapterDetail.ImageUrls.Count;
                        Console.WriteLine($"Parsing manuel: {chapterDetail.PageCount} URLs d'images extraites");
                    }
                    catch (Exception parseEx)
                    {
                        Console.WriteLine($"Échec du parsing manuel: {parseEx.Message}");
                    }
                }
                
                if (chapterDetail != null)
                {
                    // Compléter les informations du chapitre
                    chapterDetail.MangaTitle = mangaTitle;
                    chapterDetail.ScanName = scanName;
                    chapterDetail.Number = chapterNumber;
                    chapterDetail.PageCount = chapterDetail.ImageUrls?.Count ?? 0;
                    
                    if (chapterDetail.Pages == null)
                    {
                        chapterDetail.Pages = new List<Bitmap?>();
                    }
                      Console.WriteLine($"Chapitre désérialisé avec {chapterDetail.PageCount} pages");
                    
                    // Télécharger chaque page
                    if (chapterDetail.ImageUrls != null && chapterDetail.ImageUrls.Count > 0)
                    {
                        // Initialiser immédiatement les placeholders pour toutes les pages
                        Console.WriteLine($"Initialisation de {chapterDetail.ImageUrls.Count} placeholders");
                        for (int i = 0; i < chapterDetail.ImageUrls.Count; i++)
                        {
                            progressCallback?.Report((i, null));
                        }
                        
                        // Utiliser un SemaphoreSlim pour limiter le nombre de téléchargements concurrents
                        int maxConcurrentDownloads = 5; // Limite à 5 téléchargements simultanés
                        // S'assurer que System.Threading est inclus pour SemaphoreSlim
                        using var semaphore = new SemaphoreSlim(maxConcurrentDownloads);

                        var downloadTasks = new List<Task>();
                        var pageBitmaps = new Bitmap?[chapterDetail.ImageUrls.Count]; // Pour stocker les bitmaps dans l'ordre

                        for (int i = 0; i < chapterDetail.ImageUrls.Count; i++)
                        {
                            var imageUrl = chapterDetail.ImageUrls[i];
                            int currentIndex = i; // Capturer l'index pour le lambda

                            // Ignorer les URLs vides
                            if (string.IsNullOrWhiteSpace(imageUrl))
                            {
                                Console.WriteLine($"URL de la page {currentIndex + 1} est vide, ignorée");
                                pageBitmaps[currentIndex] = null;
                                progressCallback?.Report((currentIndex, null));
                                continue;
                            }

                            // S'assurer que l'URL est absolue
                            if (!Uri.IsWellFormedUriString(imageUrl, UriKind.Absolute))
                            {
                                if (imageUrl.StartsWith("//"))
                                {
                                    imageUrl = $"https:{imageUrl}";
                                }
                                else if (imageUrl.StartsWith("/"))
                                {
                                    imageUrl = $"{BaseUrl}{imageUrl}"; // Assurez-vous que BaseUrl est correct ici
                                }
                                else 
                                {
                                    // Tenter de former une URL valide, cela peut nécessiter une logique plus robuste
                                    // basée sur la source des URLs relatives.
                                    // Pour l'instant, on suppose qu'elles peuvent être préfixées par https://
                                    imageUrl = $"https://{imageUrl}";
                                }
                                chapterDetail.ImageUrls[currentIndex] = imageUrl; // Mettre à jour l'URL dans la liste si elle a été modifiée
                            }
                            
                            // Attendre qu'un slot soit disponible
                            await semaphore.WaitAsync();

                            downloadTasks.Add(Task.Run(async () =>
                            {
                                try
                                {
                                    Console.WriteLine($"Téléchargement de la page {currentIndex + 1}/{chapterDetail.ImageUrls.Count}: {imageUrl}");
                                    var pageBitmap = await DownloadImageAsync(imageUrl);
                                    pageBitmaps[currentIndex] = pageBitmap;
                                    progressCallback?.Report((currentIndex, pageBitmap));
                                }
                                catch (Exception ex)
                                {
                                    Console.WriteLine($"Erreur lors du téléchargement de la page {currentIndex + 1}: {ex.Message}");
                                    pageBitmaps[currentIndex] = null;
                                    progressCallback?.Report((currentIndex, null));
                                }
                                finally
                                {
                                    semaphore.Release(); // Libérer le slot
                                }
                            }));
                        }
                        
                        await Task.WhenAll(downloadTasks);
                        chapterDetail.Pages = pageBitmaps.ToList(); // Assigner les bitmaps dans le bon ordre
                        
                        Console.WriteLine("Toutes les pages ont été téléchargées");
                        
                        // Mise en cache du chapitre seulement si des pages ont été téléchargées avec succès
                        if (chapterDetail.Pages.Any(p => p != null))
                        {
                            await _cacheService.SaveChapterToCacheAsync(chapterDetail);
                        }
                    }
                    else
                    {
                        Console.WriteLine("Aucune URL d'image trouvée dans la réponse");
                    }
                }
                else
                {
                    Console.WriteLine("La désérialisation a renvoyé null");
                }
                
                return chapterDetail;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors de la récupération des pages: {ex.Message}");
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
