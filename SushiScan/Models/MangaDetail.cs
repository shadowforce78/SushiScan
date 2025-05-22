using System;
using System.Collections.Generic;
using System.Text.Json.Serialization;
using Avalonia.Media.Imaging;

namespace SushiScan.Models
{
    public class MangaDetail
    {
        [JsonPropertyName("_id")]
        public string Id { get; set; } = string.Empty;
        
        [JsonPropertyName("title")]
        public string Title { get; set; } = string.Empty;
        
        [JsonPropertyName("alt_title")]
        public string AltTitle { get; set; } = string.Empty;
        
        [JsonPropertyName("genres")]
        public List<string> Genres { get; set; } = new List<string>();
        
        [JsonPropertyName("image_url")]
        public string ImageUrl { get; set; } = string.Empty;
        
        [JsonPropertyName("language")]
        public string Language { get; set; } = string.Empty;
        
        [JsonPropertyName("scan_types")]
        public List<ScanType> ScanTypes { get; set; } = new List<ScanType>();
        
        [JsonPropertyName("type")]
        public string Type { get; set; } = string.Empty;
        
        [JsonPropertyName("updated_at")]
        public string UpdatedAt { get; set; } = string.Empty;
        
        [JsonPropertyName("url")]
        public string Url { get; set; } = string.Empty;
        
        [JsonPropertyName("popularity")]
        public int Popularity { get; set; }
        
        // Propriété pour l'affichage de la date formatée
        [JsonIgnore]
        public string FormattedDate
        {
            get
            {
                if (DateTime.TryParse(UpdatedAt, out DateTime date))
                {
                    return date.ToString("dd/MM/yyyy HH:mm");
                }
                return "Date inconnue";
            }
        }
        
        // Image téléchargée pour l'affichage
        [JsonIgnore]
        public Bitmap? Image { get; set; }
    }
    
    public class ScanType
    {
        [JsonPropertyName("name")]
        public string Name { get; set; } = string.Empty;
        
        [JsonPropertyName("url")]
        public string Url { get; set; } = string.Empty;
    }
}
