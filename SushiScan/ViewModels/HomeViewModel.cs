using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Threading.Tasks;
using System.Windows.Input;
using Avalonia.Media.Imaging;
using SushiScan.Commands;
using SushiScan.Models;
using SushiScan.Services;

namespace SushiScan.ViewModels
{
    public class HomeViewModel : ViewModelBase
    {
        private readonly ApiService _apiService;
        private bool _isLoading;
        private string _errorMessage = string.Empty;
        private string _searchQuery = string.Empty;
        private bool _isSearching;
        private bool _showSearchResults;
        private bool _isTrendingLoading;
        private bool _isPopularLoading;
        private bool _isRecommendedLoading;
        private bool _hasTrendingData;
        private bool _hasPopularData;
        private bool _hasRecommendedData;
        
        public string Title { get; } = "SushiScan";
        public string TrendingMangasTitle { get; } = "Tendances";
        public string PopularMangasTitle { get; } = "Populaires";
        public string RecommendedMangasTitle { get; } = "Recommandés";
        
        public ObservableCollection<Manga> TrendingMangas { get; } = new();
        public ObservableCollection<Manga> PopularMangas { get; } = new();
        public ObservableCollection<Manga> RecommendedMangas { get; } = new();
        public ObservableCollection<MangaSearchResult> SearchResults { get; } = new();

        // Événement pour notifier la sélection d'un manga
        public event EventHandler<string>? MangaSelected;
        
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
        
        public bool IsTrendingLoading
        {
            get => _isTrendingLoading;
            private set
            {
                if (_isTrendingLoading != value)
                {
                    _isTrendingLoading = value;
                    OnPropertyChanged();
                }
            }
        }
        
        public bool IsPopularLoading
        {
            get => _isPopularLoading;
            private set
            {
                if (_isPopularLoading != value)
                {
                    _isPopularLoading = value;
                    OnPropertyChanged();
                }
            }
        }
        
        public bool IsRecommendedLoading
        {
            get => _isRecommendedLoading;
            private set
            {
                if (_isRecommendedLoading != value)
                {
                    _isRecommendedLoading = value;
                    OnPropertyChanged();
                }
            }
        }
        
        public bool HasTrendingData
        {
            get => _hasTrendingData;
            private set
            {
                if (_hasTrendingData != value)
                {
                    _hasTrendingData = value;
                    OnPropertyChanged();
                }
            }
        }
        
        public bool HasPopularData
        {
            get => _hasPopularData;
            private set
            {
                if (_hasPopularData != value)
                {
                    _hasPopularData = value;
                    OnPropertyChanged();
                }
            }
        }
        
        public bool HasRecommendedData
        {
            get => _hasRecommendedData;
            private set
            {
                if (_hasRecommendedData != value)
                {
                    _hasRecommendedData = value;
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

        public string SearchQuery
        {
            get => _searchQuery;
            set
            {
                if (_searchQuery != value)
                {
                    _searchQuery = value;
                    OnPropertyChanged();
                }
            }
        }

        public bool IsSearching
        {
            get => _isSearching;
            private set
            {
                if (_isSearching != value)
                {
                    _isSearching = value;
                    OnPropertyChanged();
                }
            }
        }

        public bool ShowSearchResults
        {
            get => _showSearchResults;
            private set
            {
                if (_showSearchResults != value)
                {
                    _showSearchResults = value;
                    OnPropertyChanged();
                }
            }
        }

        public ICommand SearchCommand { get; }
        public ICommand ClearSearchCommand { get; }
        public ICommand ShowMangaDetailCommand { get; }
        
        public HomeViewModel()
        {
            Console.WriteLine("HomeViewModel: Initialisation");
            _apiService = new ApiService();

            SearchCommand = new RelayCommand(async _ => await SearchMangaAsync());
            ClearSearchCommand = new RelayCommand(_ => ClearSearch());
            ShowMangaDetailCommand = new RelayCommand(param => OnMangaSelected(param as string));
        }
        
        private async Task SearchMangaAsync()
        {
            if (string.IsNullOrWhiteSpace(SearchQuery))
            {
                return;
            }

            try
            {
                ErrorMessage = string.Empty;
                IsSearching = true;
                ShowSearchResults = true;
                
                Console.WriteLine($"Recherche de mangas avec query: {SearchQuery}");
                
                SearchResults.Clear();
                var results = await _apiService.SearchMangaAsync(SearchQuery);
                
                foreach (var result in results)
                {
                    SearchResults.Add(result);
                }
                
                Console.WriteLine($"Recherche terminée, {results.Count} résultats trouvés");
            }
            catch (Exception ex)
            {
                ErrorMessage = "Erreur lors de la recherche : " + ex.Message;
                Console.WriteLine($"Erreur de recherche: {ex}");
            }
            finally
            {
                IsSearching = false;
            }
        }
        
        private void ClearSearch()
        {
            SearchQuery = string.Empty;
            SearchResults.Clear();
            ShowSearchResults = false;
            ErrorMessage = string.Empty;
        }
        
        public async Task LoadDataAsync()
        {
            Console.WriteLine("HomeViewModel: Début du chargement des données");
            try
            {
                IsLoading = true;
                IsTrendingLoading = true;
                IsPopularLoading = true;
                IsRecommendedLoading = true;
                
                Console.WriteLine("HomeViewModel: IsLoading = true");
                ErrorMessage = string.Empty;
                
                // Lancer les trois tâches de chargement en parallèle
                var trendingTask = LoadTrendingAsync();
                var popularTask = LoadPopularAsync();
                var recommendedTask = LoadRecommendedAsync();
                
                // Attendre que toutes les tâches se terminent
                await Task.WhenAll(trendingTask, popularTask, recommendedTask);
            }
            catch (Exception ex)
            {
                ErrorMessage = $"Erreur lors du chargement des données: {ex.Message}";
                Console.WriteLine($"HomeViewModel: Exception: {ex}");
            }
            finally
            {
                IsLoading = false;
                Console.WriteLine("HomeViewModel: IsLoading = false");
            }
        }
        
        private async Task LoadTrendingAsync()
        {
            try
            {
                Console.WriteLine("HomeViewModel: Chargement des tendances");
                TrendingMangas.Clear();
                
                var homePageData = await _apiService.GetHomePageDataAsync();
                
                if (homePageData != null && homePageData.Trending != null)
                {
                    foreach (var manga in homePageData.Trending)
                    {
                        TrendingMangas.Add(manga);
                    }
                    HasTrendingData = TrendingMangas.Count > 0;
                    Console.WriteLine($"HomeViewModel: {TrendingMangas.Count} mangas ajoutés à TrendingMangas");
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors du chargement des tendances: {ex.Message}");
            }
            finally
            {
                IsTrendingLoading = false;
            }
        }
        
        private async Task LoadPopularAsync()
        {
            try
            {
                Console.WriteLine("HomeViewModel: Chargement des populaires");
                PopularMangas.Clear();
                
                var homePageData = await _apiService.GetHomePageDataAsync();
                
                if (homePageData != null && homePageData.Popular != null)
                {
                    foreach (var manga in homePageData.Popular)
                    {
                        PopularMangas.Add(manga);
                    }
                    HasPopularData = PopularMangas.Count > 0;
                    Console.WriteLine($"HomeViewModel: {PopularMangas.Count} mangas ajoutés à PopularMangas");
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors du chargement des populaires: {ex.Message}");
            }
            finally
            {
                IsPopularLoading = false;
            }
        }
        
        private async Task LoadRecommendedAsync()
        {
            try
            {
                Console.WriteLine("HomeViewModel: Chargement des recommandations");
                RecommendedMangas.Clear();
                
                var homePageData = await _apiService.GetHomePageDataAsync();
                
                if (homePageData != null && homePageData.Recommended != null)
                {
                    foreach (var manga in homePageData.Recommended)
                    {
                        RecommendedMangas.Add(manga);
                    }
                    HasRecommendedData = RecommendedMangas.Count > 0;
                    Console.WriteLine($"HomeViewModel: {RecommendedMangas.Count} mangas ajoutés à RecommendedMangas");
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur lors du chargement des recommandations: {ex.Message}");
            }
            finally
            {
                IsRecommendedLoading = false;
            }
        }

        private void OnMangaSelected(string? mangaId)
        {
            if (!string.IsNullOrEmpty(mangaId))
            {
                MangaSelected?.Invoke(this, mangaId);
            }
        }
    }
}
