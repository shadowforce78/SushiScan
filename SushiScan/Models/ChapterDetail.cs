using System;
using System.Collections.Generic;
using System.Text.Json.Serialization;
using Avalonia.Media.Imaging;

namespace SushiScan.Models
{
    public class ChapterDetail
    {
        [JsonPropertyName("_id")]
        public string Id { get; set; } = string.Empty;

        [JsonPropertyName("manga_title")]
        public string MangaTitle { get; set; } = string.Empty;

        [JsonPropertyName("number")]
        public string Number { get; set; } = string.Empty; // Garder en string pour les zéros/points

        [JsonPropertyName("scan_name")]
        public string ScanName { get; set; } = string.Empty;

        [JsonPropertyName("added_at")]
        public string AddedAt { get; set; } = string.Empty;

        [JsonPropertyName("image_urls")]
        public List<string> ImageUrls { get; set; } = new List<string>();

        [JsonPropertyName("page_count")]
        public int PageCount { get; set; }

        [JsonPropertyName("title")]
        public string? ChapterTitle { get; set; } // Titre optionnel du chapitre

        // Propriétés pour l'affichage
        [JsonIgnore]
        public List<Bitmap?> Pages { get; set; } = new List<Bitmap?>();

        [JsonIgnore]
        public string FormattedAddedDate
        {
            get
            {
                if (DateTime.TryParse(AddedAt, out DateTime date))
                {
                    return date.ToString("dd/MM/yyyy HH:mm");
                }
                return "Date inconnue";
            }
        }
    }
}
