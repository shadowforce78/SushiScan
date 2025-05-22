using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Threading.Tasks;
using Avalonia.Media.Imaging;
using SushiScan.Models;
using SushiScan.Services;

namespace SushiScan.ViewModels
{
    public class HomeViewModel : ViewModelBase
    {
        private readonly ApiService _apiService;
        private bool _isLoading;
        private string _errorMessage = string.Empty;
        
        public string Title { get; } = "SushiScan";
        public string TrendingMangasTitle { get; } = "Tendances";
        public string PopularMangasTitle { get; } = "Populaires";
        public string RecommendedMangasTitle { get; } = "Recommandés";
        
        public ObservableCollection<Manga> TrendingMangas { get; } = new();
        public ObservableCollection<Manga> PopularMangas { get; } = new();
        public ObservableCollection<Manga> RecommendedMangas { get; } = new();
        
        public bool IsLoading 
        { 
            get => _isLoading;
            private set
            {
                if (_isLoading != value)
                {
                    _isLoading = value;
                    OnPropertyChanged();
                }
            }
        }
        
        public string ErrorMessage 
        { 
            get => _errorMessage;
            private set
            {
                if (_errorMessage != value)
                {
                    _errorMessage = value;
                    OnPropertyChanged();
                }
            }
        }
        
        public HomeViewModel()
        {
            Console.WriteLine("HomeViewModel: Initialisation");
            _apiService = new ApiService();
        }
        
        public async Task LoadDataAsync()
        {
            Console.WriteLine("HomeViewModel: Début du chargement des données");
            try
            {
                IsLoading = true;
                Console.WriteLine("HomeViewModel: IsLoading = true");
                
                // Charger les données depuis l'API
                Console.WriteLine("HomeViewModel: Appel de GetHomePageDataAsync()");
                var homePageData = await _apiService.GetHomePageDataAsync();
                Console.WriteLine($"HomeViewModel: Données reçues de l'API: {(homePageData != null ? "OK" : "NULL")}");
                
                if (homePageData != null)
                {
                    // Effacer les collections existantes
                    TrendingMangas.Clear();
                    PopularMangas.Clear();
                    RecommendedMangas.Clear();
                    Console.WriteLine("HomeViewModel: Collections vidées");
                    
                    // Ajouter les nouvelles données
                    foreach (var manga in homePageData.Trending)
                    {
                        TrendingMangas.Add(manga);
                    }
                    Console.WriteLine($"HomeViewModel: {homePageData.Trending.Count} mangas ajoutés à TrendingMangas");
                    
                    foreach (var manga in homePageData.Popular)
                    {
                        PopularMangas.Add(manga);
                    }
                    Console.WriteLine($"HomeViewModel: {homePageData.Popular.Count} mangas ajoutés à PopularMangas");
                    
                    foreach (var manga in homePageData.Recommended)
                    {
                        RecommendedMangas.Add(manga);
                    }
                    Console.WriteLine($"HomeViewModel: {homePageData.Recommended.Count} mangas ajoutés à RecommendedMangas");
                }
                else
                {
                    ErrorMessage = "Impossible de récupérer les données. Veuillez réessayer plus tard.";
                    Console.WriteLine($"HomeViewModel: Erreur définie: {ErrorMessage}");
                }
            }
            catch (Exception ex)
            {
                ErrorMessage = $"Une erreur s'est produite: {ex.Message}";
                Console.WriteLine($"HomeViewModel: Exception: {ex.GetType().Name} - {ex.Message}");
                if (ex.InnerException != null)
                {
                    Console.WriteLine($"HomeViewModel: Exception interne: {ex.InnerException.Message}");
                }
                Console.WriteLine($"HomeViewModel: Stack trace: {ex.StackTrace}");
            }
            finally
            {
                IsLoading = false;
                Console.WriteLine("HomeViewModel: IsLoading = false");
            }
        }
    }
}
