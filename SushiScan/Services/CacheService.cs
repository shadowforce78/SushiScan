using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;
using System.Threading.Tasks;
using Avalonia.Media.Imaging;
using SushiScan.Models;

namespace SushiScan.Services
{
    public class CacheService
    {
        private readonly string _chapterCacheDirectory;
        private readonly string _coverCacheDirectory;
        
        public CacheService()
        {
            // Créer les dossiers dans AppData pour le cache
            string baseDirectory = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "SushiScan"
            );
            
            _chapterCacheDirectory = Path.Combine(baseDirectory, "ChapterCache");
            _coverCacheDirectory = Path.Combine(baseDirectory, "CoverCache");
            
            // S'assurer que les répertoires existent
            Directory.CreateDirectory(_chapterCacheDirectory);
            Directory.CreateDirectory(_coverCacheDirectory);
            
            Console.WriteLine($"Répertoire de cache chapitres: {_chapterCacheDirectory}");
            Console.WriteLine($"Répertoire de cache couvertures: {_coverCacheDirectory}");
        }

        #region Chapter Cache

        /// <summary>
        /// Génère une clé unique pour un chapitre
        /// </summary>
        private string GenerateChapterKey(string mangaTitle, string scanName, string chapterNumber)
        {
            return $"{mangaTitle.Replace(" ", "_")}_{scanName}_{chapterNumber}";
        }
        
        /// <summary>
        /// Vérifie si un chapitre est déjà en cache
        /// </summary>
        public bool IsChapterCached(string mangaTitle, string scanName, string chapterNumber)
        {
            string chapterKey = GenerateChapterKey(mangaTitle, scanName, chapterNumber);
            string metadataPath = Path.Combine(_chapterCacheDirectory, $"{chapterKey}_metadata.json");
            
            return File.Exists(metadataPath);
        }

        /// <summary>
        /// Sauvegarde un chapitre dans le cache
        /// </summary>
        public async Task SaveChapterToCacheAsync(ChapterDetail chapter)
        {
            try
            {
                string chapterKey = GenerateChapterKey(chapter.MangaTitle, chapter.ScanName, chapter.Number);
                string metadataPath = Path.Combine(_chapterCacheDirectory, $"{chapterKey}_metadata.json");
                string imagesDirPath = Path.Combine(_chapterCacheDirectory, chapterKey);
                
                // Créer le dossier pour les images si nécessaire
                Directory.CreateDirectory(imagesDirPath);

                // Sauvegarder les métadonnées (sans les images)
                var chapterMetadata = new ChapterDetail
                {
                    Id = chapter.Id,
                    MangaTitle = chapter.MangaTitle,
                    Number = chapter.Number,
                    ScanName = chapter.ScanName,
                    AddedAt = chapter.AddedAt,
                    ImageUrls = chapter.ImageUrls,
                    PageCount = chapter.PageCount,
                    ChapterTitle = chapter.ChapterTitle
                };
                
                string metadataJson = JsonSerializer.Serialize(chapterMetadata, new JsonSerializerOptions 
                { 
                    WriteIndented = true 
                });
                
                await File.WriteAllTextAsync(metadataPath, metadataJson);
                Console.WriteLine($"Métadonnées du chapitre sauvegardées: {metadataPath}");

                // Sauvegarder les images
                for (int i = 0; i < chapter.Pages.Count; i++)
                {
                    if (chapter.Pages[i] != null)
                    {
                        string imagePath = Path.Combine(imagesDirPath, $"page_{i}.png");
                        using var fileStream = File.Create(imagePath);
                        chapter.Pages[i]!.Save(fileStream);
                        Console.WriteLine($"Image sauvegardée: {imagePath}");
                    }
                }
                
                Console.WriteLine($"Chapitre mis en cache avec succès: {chapterKey}");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors de la sauvegarde du chapitre en cache: {ex.Message}");
            }
        }

        /// <summary>
        /// Charge un chapitre depuis le cache
        /// </summary>
        public async Task<ChapterDetail?> LoadChapterFromCacheAsync(
            string mangaTitle, 
            string scanName, 
            string chapterNumber, 
            IProgress<(int index, Bitmap? page)>? progressCallback = null)
        {
            try
            {
                string chapterKey = GenerateChapterKey(mangaTitle, scanName, chapterNumber);
                string metadataPath = Path.Combine(_chapterCacheDirectory, $"{chapterKey}_metadata.json");
                string imagesDirPath = Path.Combine(_chapterCacheDirectory, chapterKey);

                if (!File.Exists(metadataPath) || !Directory.Exists(imagesDirPath))
                {
                    Console.WriteLine($"Chapitre non trouvé en cache: {chapterKey}");
                    return null;
                }

                // Charger les métadonnées
                string metadataJson = await File.ReadAllTextAsync(metadataPath);
                var chapter = JsonSerializer.Deserialize<ChapterDetail>(metadataJson);

                if (chapter == null)
                {
                    Console.WriteLine("Échec de la désérialisation des métadonnées du chapitre");
                    return null;
                }

                // Charger les images
                chapter.Pages = new List<Bitmap?>();
                int pageIndex = 0;
                
                while (true)
                {
                    string imagePath = Path.Combine(imagesDirPath, $"page_{pageIndex}.png");
                    if (!File.Exists(imagePath))
                    {
                        break;
                    }
                    
                    try
                    {
                        using var fileStream = File.OpenRead(imagePath);
                        var bitmap = new Bitmap(fileStream);
                        chapter.Pages.Add(bitmap);
                        Console.WriteLine($"Image chargée depuis le cache: {imagePath}");
                        
                        // Notifier de la progression
                        progressCallback?.Report((pageIndex, bitmap));
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine($"Erreur lors du chargement de l'image {imagePath}: {ex.Message}");
                        chapter.Pages.Add(null);
                        progressCallback?.Report((pageIndex, null));
                    }
                    
                    pageIndex++;
                }

                if (chapter.Pages.Count == 0)
                {
                    Console.WriteLine($"Aucune image trouvée en cache pour le chapitre: {chapterKey}");
                    return null;
                }

                Console.WriteLine($"Chapitre chargé depuis le cache avec succès: {chapterKey}, {chapter.Pages.Count} pages");
                return chapter;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors du chargement du chapitre depuis le cache: {ex.Message}");
                return null;
            }
        }

        /// <summary>
        /// Supprime un chapitre du cache
        /// </summary>
        public void ClearChapterCache(string mangaTitle, string scanName, string chapterNumber)
        {
            try
            {
                string chapterKey = GenerateChapterKey(mangaTitle, scanName, chapterNumber);
                string metadataPath = Path.Combine(_chapterCacheDirectory, $"{chapterKey}_metadata.json");
                string imagesDirPath = Path.Combine(_chapterCacheDirectory, chapterKey);

                if (File.Exists(metadataPath))
                {
                    File.Delete(metadataPath);
                }

                if (Directory.Exists(imagesDirPath))
                {
                    Directory.Delete(imagesDirPath, true);
                }
                
                Console.WriteLine($"Cache du chapitre supprimé: {chapterKey}");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors de la suppression du cache du chapitre: {ex.Message}");
            }
        }
        
        #endregion
        
        #region Cover Cache
        
        /// <summary>
        /// Génère une clé unique pour une couverture de manga
        /// </summary>
        private string GenerateCoverKey(string mangaTitle)
        {
            // Utiliser le slug pour la cohérence avec l'ApiService
            return mangaTitle.ToLower()
                .Replace(" ", "-")
                .Replace("é", "e")
                .Replace("è", "e")
                .Replace("ê", "e")
                .Replace("ë", "e")
                .Replace("à", "a")
                .Replace("â", "a")
                .Replace("ä", "a")
                .Replace("î", "i")
                .Replace("ï", "i")
                .Replace("ô", "o")
                .Replace("ö", "o")
                .Replace("ù", "u")
                .Replace("û", "u")
                .Replace("ü", "u")
                .Replace("ç", "c");
        }
        
        /// <summary>
        /// Vérifie si une couverture de manga est déjà en cache
        /// </summary>
        public bool IsCoverCached(string mangaTitle)
        {
            string coverKey = GenerateCoverKey(mangaTitle);
            string coverPath = Path.Combine(_coverCacheDirectory, $"{coverKey}.jpg");
            
            return File.Exists(coverPath);
        }
        
        /// <summary>
        /// Sauvegarde une couverture de manga dans le cache
        /// </summary>
        public async Task SaveCoverToCacheAsync(string mangaTitle, Bitmap cover)
        {
            try
            {
                string coverKey = GenerateCoverKey(mangaTitle);
                string coverPath = Path.Combine(_coverCacheDirectory, $"{coverKey}.jpg");
                
                using var fileStream = File.Create(coverPath);
                cover.Save(fileStream);
                
                Console.WriteLine($"Couverture mise en cache avec succès: {coverPath}");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors de la sauvegarde de la couverture en cache: {ex.Message}");
            }
        }
        
        /// <summary>
        /// Charge une couverture de manga depuis le cache
        /// </summary>
        public Bitmap? LoadCoverFromCache(string mangaTitle)
        {
            try
            {
                string coverKey = GenerateCoverKey(mangaTitle);
                string coverPath = Path.Combine(_coverCacheDirectory, $"{coverKey}.jpg");
                
                if (!File.Exists(coverPath))
                {
                    Console.WriteLine($"Couverture non trouvée en cache: {coverKey}");
                    return null;
                }
                
                using var fileStream = File.OpenRead(coverPath);
                var bitmap = new Bitmap(fileStream);
                
                Console.WriteLine($"Couverture chargée depuis le cache: {coverPath}");
                return bitmap;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors du chargement de la couverture depuis le cache: {ex.Message}");
                return null;
            }
        }
        
        #endregion
    }
}
