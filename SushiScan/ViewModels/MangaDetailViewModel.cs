using System;
using System.Threading.Tasks;
using System.Windows.Input;
using Avalonia.Media.Imaging;
using SushiScan.Commands;
using SushiScan.Models;
using SushiScan.Services;

namespace SushiScan.ViewModels
{
    public class MangaDetailViewModel : ViewModelBase
    {
        private readonly ApiService _apiService;
        private MangaDetail? _mangaDetail;
        private bool _isLoading;
        private string _errorMessage = string.Empty;
        private string _mangaTitle = string.Empty;
        
        public string MangaTitle
        {
            get => _mangaTitle;
            set
            {
                if (_mangaTitle != value)
                {
                    _mangaTitle = value;
                    OnPropertyChanged();
                    // Charger les détails quand le titre change
                    if (!string.IsNullOrEmpty(_mangaTitle))
                    {
                        LoadMangaDetailAsync().ConfigureAwait(false);
                    }
                }
            }
        }
        
        public MangaDetail? MangaDetail
        {
            get => _mangaDetail;
            private set
            {
                if (_mangaDetail != value)
                {
                    _mangaDetail = value;
                    OnPropertyChanged();
                }
            }
        }
        
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
        
        // Propriété pour indiquer la disponibilité des données
        public bool HasData => MangaDetail != null;
        
        // URL pour ouvrir la lecture du manga
        public ICommand OpenReadingUrlCommand { get; }
        
        // Constructeur
        public MangaDetailViewModel()
        {
            _apiService = new ApiService();
            OpenReadingUrlCommand = new RelayCommand(OpenReadingUrl);
        }
        
        private async Task LoadMangaDetailAsync()
        {
            try
            {
                IsLoading = true;
                ErrorMessage = string.Empty;
                
                Console.WriteLine($"Chargement des détails du manga: {MangaTitle}");
                var mangaDetail = await _apiService.GetMangaDetailAsync(MangaTitle);
                
                if (mangaDetail != null)
                {
                    MangaDetail = mangaDetail;
                    Console.WriteLine("Détails du manga chargés avec succès");
                    // Notification du changement de HasData
                    OnPropertyChanged(nameof(HasData));
                }
                else
                {
                    ErrorMessage = "Impossible de charger les détails du manga. Veuillez réessayer plus tard.";
                    Console.WriteLine("Échec du chargement des détails du manga");
                }
            }
            catch (Exception ex)
            {
                ErrorMessage = $"Erreur lors du chargement des détails du manga: {ex.Message}";
                Console.WriteLine($"Exception: {ex}");
            }
            finally
            {
                IsLoading = false;
            }
        }
        
        private void OpenReadingUrl(object? parameter)
        {
            if (MangaDetail?.Url != null)
            {
                try
                {
                    // Ouvrir l'URL dans le navigateur par défaut
                    var processStartInfo = new System.Diagnostics.ProcessStartInfo
                    {
                        FileName = MangaDetail.Url,
                        UseShellExecute = true
                    };
                    System.Diagnostics.Process.Start(processStartInfo);
                    Console.WriteLine($"URL ouverte: {MangaDetail.Url}");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Erreur lors de l'ouverture de l'URL: {ex.Message}");
                    ErrorMessage = "Impossible d'ouvrir l'URL de lecture.";
                }
            }
        }
    }
}
