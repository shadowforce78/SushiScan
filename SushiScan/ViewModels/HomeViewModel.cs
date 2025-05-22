using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Threading.Tasks;
using System.Windows.Input;
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
        private string _searchQuery = string.Empty;
        private bool _isSearching;
        private bool _showSearchResults;
        
        public string Title { get; } = "SushiScan";
        public string TrendingMangasTitle { get; } = "Tendances";
        public string PopularMangasTitle { get; } = "Populaires";
        public string RecommendedMangasTitle { get; } = "Recommandés";
        
        public ObservableCollection<Manga> TrendingMangas { get; } = new();
        public ObservableCollection<Manga> PopularMangas { get; } = new();
        public ObservableCollection<Manga> RecommendedMangas { get; } = new();
        public ObservableCollection<MangaSearchResult> SearchResults { get; } = new();
        
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
        
        public HomeViewModel()
        {
            Console.WriteLine("HomeViewModel: Initialisation");
            _apiService = new ApiService();

            SearchCommand = new RelayCommand(async _ => await SearchMangaAsync());
            ClearSearchCommand = new RelayCommand(_ => ClearSearch());
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
                ErrorMessage = $"Erreur lors du chargement des données: {ex.Message}";
                Console.WriteLine($"HomeViewModel: Exception: {ex}");
            }
            finally
            {
                IsLoading = false;
                Console.WriteLine("HomeViewModel: IsLoading = false");
            }
        }
    }

    // Classe RelayCommand pour implémenter ICommand
    public class RelayCommand : ICommand
    {
        private readonly Action<object> _execute;
        private readonly Predicate<object> _canExecute;

        public RelayCommand(Action<object> execute, Predicate<object> canExecute = null)
        {
            _execute = execute ?? throw new ArgumentNullException(nameof(execute));
            _canExecute = canExecute;
        }

        public bool CanExecute(object parameter)
        {
            return _canExecute == null || _canExecute(parameter);
        }

        public void Execute(object parameter)
        {
            _execute(parameter);
        }

        public event EventHandler CanExecuteChanged;

        public void RaiseCanExecuteChanged()
        {
            CanExecuteChanged?.Invoke(this, EventArgs.Empty);
        }
    }
}
