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
            if (parameter is string url && MangaDetail != null)
            {
                try
                {
                    var scanName = string.Empty;
                    var chapterNumber = "1"; // Par défaut chapitre 1
                    
                    // Déterminer le nom du scan depuis l'URL du paramètre ou des propriétés du manga
                    if (parameter is string scanUrl)
                    {
                        foreach (var scanType in MangaDetail.ScanTypes)
                        {
                            if (scanType.Url == scanUrl)
                            {
                                scanName = scanType.Name;
                                break;
                            }
                        }
                    }
                    
                    if (string.IsNullOrEmpty(scanName) && MangaDetail.ScanTypes.Count > 0)
                    {
                        // Utiliser le premier type de scan disponible si on ne trouve pas de correspondance
                        scanName = MangaDetail.ScanTypes[0].Name;
                    }
                    
                    Console.WriteLine($"Navigation vers le lecteur de chapitre - Titre: {MangaDetail.Title}, Scan: {scanName}, Chapitre: {chapterNumber}");
                    
                    // Créer une instance du ViewModel du lecteur
                    var chapterReaderViewModel = new ChapterReaderViewModel();
                    
                    // Lancer le chargement du chapitre (asynchrone)
                    _ = chapterReaderViewModel.LoadChapterAsync(MangaDetail.Title, scanName, chapterNumber);
                    
                    // Informer l'application du changement de vue en émettant un événement
                    ChapterSelected?.Invoke(this, chapterReaderViewModel);
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Erreur lors de la navigation vers le chapitre: {ex.Message}");
                    ErrorMessage = "Impossible de charger le chapitre.";
                }
            }
        }
        
        // Événement pour informer le MainViewModel qu'un chapitre a été sélectionné
        public event EventHandler<ChapterReaderViewModel>? ChapterSelected;
    }
}
