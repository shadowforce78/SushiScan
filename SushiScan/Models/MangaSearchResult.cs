using System;
using System.Collections.Generic;
using Avalonia.Media.Imaging;

namespace SushiScan.Models
{
    public class MangaSearchResult
    {
        public required string _id { get; set; }
        public required string Title { get; set; }
        public List<string> Genres { get; set; } = new List<string>();
        public required string Type { get; set; }
          // Propriétés supplémentaires pour la gestion des images dans l'application
        public required string ImageUrl { get; set; }
        public Bitmap? Image { get; set; }
    }
}
