
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using Avalonia.Media.Imaging;
using SushiScan.Models;

namespace SushiScan.ViewModels
{
    public class HomeViewModel : ViewModelBase
    {
        public string Title { get; } = "ScanVerse";
        public string PopularMangasTitle { get; } = "Mangas Populaires";
        
        public ObservableCollection<Manga> PopularMangas { get; }
        
        public HomeViewModel()
        {
            // Données mockées pour les mangas populaires
            PopularMangas = new ObservableCollection<Manga>
            {
                new Manga 
                { 
                    Id = "1", 
                    Name = "One Piece", 
                    MainGenre = "Action/Aventure", 
                    ImagePath = "avares://SushiScan/Assets/manga1.jpg" 
                },
                new Manga 
                { 
                    Id = "2", 
                    Name = "Demon Slayer", 
                    MainGenre = "Action/Fantastique", 
                    ImagePath = "avares://SushiScan/Assets/manga2.jpg" 
                },
                new Manga 
                { 
                    Id = "3", 
                    Name = "Jujutsu Kaisen", 
                    MainGenre = "Action/Surnaturel", 
                    ImagePath = "avares://SushiScan/Assets/manga3.jpg" 
                },
                new Manga 
                { 
                    Id = "4", 
                    Name = "Chainsaw Man", 
                    MainGenre = "Action/Horreur", 
                    ImagePath = "avares://SushiScan/Assets/manga4.jpg" 
                },
                new Manga 
                { 
                    Id = "5", 
                    Name = "My Hero Academia", 
                    MainGenre = "Action/Super-héros", 
                    ImagePath = "avares://SushiScan/Assets/manga5.jpg" 
                },
                new Manga 
                { 
                    Id = "6", 
                    Name = "Tokyo Revengers", 
                    MainGenre = "Action/Drame", 
                    ImagePath = "avares://SushiScan/Assets/manga6.jpg" 
                }
            };
        }
        
        public void ViewMoreMangas()
        {
            // À implémenter: logique pour voir plus de mangas
            // Cette méthode sera associée au bouton "Voir plus"
        }
    }
}
