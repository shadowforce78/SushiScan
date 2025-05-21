using System;
using System.Collections.Generic;
using Avalonia.Media.Imaging;

namespace SushiScan.Models
{
    public class Manga
    {
        public required string Id { get; set; }
        public required string Name { get; set; }
        public required string MainGenre { get; set; }
        public required string ImagePath { get; set; }
        public Bitmap? Image { get; set; }
        
        // Autres propriétés que vous pourriez ajouter plus tard
        // public List<string> Genres { get; set; }
        // public string Author { get; set; }
        // public string Description { get; set; }
        // public int ChaptersCount { get; set; }
    }
}
