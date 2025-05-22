using System;
using System.Collections.Generic;
using Avalonia.Media.Imaging;
using System.Text.Json.Serialization;

namespace SushiScan.Models
{
    public class Manga
    {
        [JsonPropertyName("_id")]
        public string Id { get; set; } = string.Empty;
        
        [JsonPropertyName("title")]
        public string Name { get; set; } = string.Empty;
        
        [JsonPropertyName("genres")]
        public List<string> Genres { get; set; } = new List<string>();
        
        [JsonPropertyName("type")]
        public string Type { get; set; } = string.Empty;
        
        [JsonPropertyName("popularity")]
        public int Popularity { get; set; }
        
        public string MainGenre => Genres.Count > 0 ? Genres[0] : "Inconnu";
        
        // Propriétés non liées à l'API mais utilisées par l'interface
        [JsonIgnore]
        public string ImagePath { get; set; } = string.Empty;
        
        [JsonIgnore]
        public Bitmap? Image { get; set; }
    }

    public class HomePageData
    {
        [JsonPropertyName("trending")]
        public List<Manga> Trending { get; set; } = new List<Manga>();
        
        [JsonPropertyName("popular")]
        public List<Manga> Popular { get; set; } = new List<Manga>();
        
        [JsonPropertyName("recommended")]
        public List<Manga> Recommended { get; set; } = new List<Manga>();
    }
}
