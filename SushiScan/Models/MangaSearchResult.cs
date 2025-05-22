using System;
using System.Collections.Generic;
using Avalonia.Media.Imaging;

namespace SushiScan.Models
{
    public class MangaSearchResult
    {
        public string _id { get; set; }
        public string Title { get; set; }
        public List<string> Genres { get; set; }
        public string Type { get; set; }
        
        // Propriétés supplémentaires pour la gestion des images dans l'application
        public string ImageUrl { get; set; }
        public Bitmap Image { get; set; }
        
        public MangaSearchResult()
        {
            Genres = new List<string>();
        }
    }
}
