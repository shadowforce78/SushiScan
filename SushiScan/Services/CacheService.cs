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
        private readonly string _cacheDirectory;
        
        public CacheService()
        {
            // Créer un dossier dans AppData pour le cache
            _cacheDirectory = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "SushiScan", 
                "ChapterCache"
            );
            
            // S'assurer que le répertoire existe
            Directory.CreateDirectory(_cacheDirectory);
            Console.WriteLine($"Répertoire de cache initialisé: {_cacheDirectory}");
        }

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
            string metadataPath = Path.Combine(_cacheDirectory, $"{chapterKey}_metadata.json");
            
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
                string metadataPath = Path.Combine(_cacheDirectory, $"{chapterKey}_metadata.json");
                string imagesDirPath = Path.Combine(_cacheDirectory, chapterKey);
                
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
        public async Task<ChapterDetail?> LoadChapterFromCacheAsync(string mangaTitle, string scanName, string chapterNumber)
        {
            try
            {
                string chapterKey = GenerateChapterKey(mangaTitle, scanName, chapterNumber);
                string metadataPath = Path.Combine(_cacheDirectory, $"{chapterKey}_metadata.json");
                string imagesDirPath = Path.Combine(_cacheDirectory, chapterKey);

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
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine($"Erreur lors du chargement de l'image {imagePath}: {ex.Message}");
                        chapter.Pages.Add(null); // Ajouter un espace réservé pour conserver l'index correct
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
                string metadataPath = Path.Combine(_cacheDirectory, $"{chapterKey}_metadata.json");
                string imagesDirPath = Path.Combine(_cacheDirectory, chapterKey);

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
                Console.WriteLine($"Erreur lors de la suppression du cache: {ex.Message}");
            }
        }

        /// <summary>
        /// Supprime tous les chapitres du cache
        /// </summary>
        public void ClearAllCache()
        {
            try
            {
                if (Directory.Exists(_cacheDirectory))
                {
                    var directories = Directory.GetDirectories(_cacheDirectory);
                    var files = Directory.GetFiles(_cacheDirectory, "*_metadata.json");

                    foreach (var dir in directories)
                    {
                        Directory.Delete(dir, true);
                    }

                    foreach (var file in files)
                    {
                        File.Delete(file);
                    }
                }

                Console.WriteLine("Cache entièrement vidé");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors du vidage du cache: {ex.Message}");
            }
        }
    }
}
