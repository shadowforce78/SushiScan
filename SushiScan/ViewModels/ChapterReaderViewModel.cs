using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Input;
using Avalonia.Media.Imaging;
using SushiScan.Commands;
using SushiScan.Models;
using SushiScan.Services;

namespace SushiScan.ViewModels
{
    public class ChapterReaderViewModel : ViewModelBase
    {
        private readonly ApiService _apiService;
        private ChapterDetail? _currentChapter;
        private int _currentPageIndex;
        private bool _isLoading;
        private string _errorMessage = string.Empty;
        private string _mangaTitle = string.Empty;
        private string _scanName = string.Empty;
        private int _chapterNumber = 1;
        
        // Collection de toutes les pages du chapitre
        public ObservableCollection<Bitmap?> AllPages { get; } = new ObservableCollection<Bitmap?>();

        public ChapterDetail? CurrentChapter
        {
            get => _currentChapter;
            private set
            {
                if (_currentChapter != value)
                {
                    _currentChapter = value;
                    OnPropertyChanged();
                    LoadAllPages();
                    OnPropertyChanged(nameof(PageCount));
                    OnPropertyChanged(nameof(ChapterDisplayTitle));
                }
            }
        }

        public int CurrentPageIndex
        {
            get => _currentPageIndex;
            set
            {
                if (_currentPageIndex != value)
                {
                    _currentPageIndex = value;
                    OnPropertyChanged();
                    OnPropertyChanged(nameof(PageInfo));
                    OnPropertyChanged(nameof(CurrentPageImage));
                }
            }
        }        public int PageCount => AllPages.Count;
        public string PageInfo => PageCount > 0 ? $"Page {CurrentPageIndex + 1} sur {PageCount}" : "";
        public string ChapterDisplayTitle => CurrentChapter != null ? $"{CurrentChapter.MangaTitle} - Chapitre {CurrentChapter.Number}" : "Chargement...";
        
        // Propriété pour l'image de la page actuelle
        public Bitmap? CurrentPageImage => CurrentChapter?.Pages.ElementAtOrDefault(CurrentPageIndex);
        
        public string MangaTitle
        {
            get => _mangaTitle;
            set
            {
                if (_mangaTitle != value)
                {
                    _mangaTitle = value;
                    OnPropertyChanged();
                }
            }
        }
        
        public string ScanName
        {
            get => _scanName;
            set
            {
                if (_scanName != value)
                {
                    _scanName = value;
                    OnPropertyChanged();
                }
            }
        }
        
        public int ChapterNumber
        {
            get => _chapterNumber;
            set
            {
                if (_chapterNumber != value)
                {
                    _chapterNumber = value;
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

        public ICommand NextChapterCommand { get; }
        public ICommand PreviousChapterCommand { get; }
        public ICommand NextPageCommand { get; }
        public ICommand PreviousPageCommand { get; }

        public ChapterReaderViewModel()
        {
            _apiService = new ApiService();
            NextChapterCommand = new RelayCommand(_ => LoadNextChapter(), _ => CanLoadNextChapter());
            PreviousChapterCommand = new RelayCommand(_ => LoadPreviousChapter(), _ => CanLoadPreviousChapter());
            NextPageCommand = new RelayCommand(_ => LoadNextPage(), _ => CanLoadNextPage());
            PreviousPageCommand = new RelayCommand(_ => LoadPreviousPage(), _ => CanLoadPreviousPage());
        }
          private void LoadAllPages()
        {
            // Cette méthode est maintenant utilisée uniquement pour le nettoyage
            // Le chargement progressif se fait dans LoadChapterAsync via le callback
            if (CurrentChapter != null && CurrentChapter.Pages.Any())
            {
                Console.WriteLine($"[DEBUG] Finalisation du chargement de {CurrentChapter.Pages.Count} pages dans la vue");
                
                // S'assurer que AllPages a la bonne taille
                while (AllPages.Count < CurrentChapter.Pages.Count)
                {
                    AllPages.Add(null);
                }
                
                // Mettre à jour toutes les pages (au cas où il y aurait des écarts)
                for (int i = 0; i < CurrentChapter.Pages.Count; i++)
                {
                    if (i < AllPages.Count)
                    {
                        AllPages[i] = CurrentChapter.Pages[i];
                    }
                }
            }
        }        public async Task LoadChapterAsync(string mangaTitle, string scanName, string chapterNumber)
        {
            MangaTitle = mangaTitle;
            ScanName = scanName;
            
            // Convertir le numéro de chapitre en entier si possible
            if (int.TryParse(chapterNumber, out int chNum))
            {
                ChapterNumber = chNum;
            }
            
            IsLoading = true;
            ErrorMessage = string.Empty;
            CurrentChapter = null; // Clear previous chapter
            AllPages.Clear();

            try
            {
                Console.WriteLine($"[DEBUG] ChapterReaderViewModel: Début du chargement du chapitre {mangaTitle}, {scanName}, {chapterNumber}");
                
                // Créer un callback de progression pour mettre à jour les pages au fur et à mesure
                var progressCallback = new Progress<(int index, Bitmap? page)>(progress =>
                {
                    Console.WriteLine($"[DEBUG] Page reçue: index {progress.index}, image: {(progress.page != null ? "OK" : "NULL")}");
                    
                    // S'assurer que la collection AllPages a la bonne taille
                    while (AllPages.Count <= progress.index)
                    {
                        AllPages.Add(null);
                    }
                    
                    // Mettre à jour la page à l'index spécifié
                    AllPages[progress.index] = progress.page;
                    
                    // Forcer la mise à jour des propriétés liées
                    OnPropertyChanged(nameof(PageCount));
                    OnPropertyChanged(nameof(PageInfo));
                });
                
                var chapterDetail = await _apiService.GetChapterPagesAsync(mangaTitle, scanName, chapterNumber, progressCallback);
                
                if (chapterDetail != null)
                {
                    Console.WriteLine($"[DEBUG] Chapitre récupéré avec {chapterDetail.Pages.Count} pages");
                    
                    // Initialiser la collection AllPages avec le bon nombre de placeholders
                    AllPages.Clear();
                    for (int i = 0; i < chapterDetail.ImageUrls.Count; i++)
                    {
                        AllPages.Add(null);
                    }
                    
                    // Notifier immédiatement pour afficher les placeholders
                    OnPropertyChanged(nameof(PageCount));
                    OnPropertyChanged(nameof(PageInfo));
                    
                    // Mettre à jour toutes les pages finales
                    for (int i = 0; i < chapterDetail.Pages.Count; i++)
                    {
                        if (i < AllPages.Count)
                        {
                            AllPages[i] = chapterDetail.Pages[i];
                        }
                    }
                    
                    CurrentChapter = chapterDetail;
                    CurrentPageIndex = 0; // Réinitialiser à la première page
                    
                    if (chapterDetail.Pages.Count == 0)
                    {
                        ErrorMessage = "Aucune page trouvée pour ce chapitre.";
                        Console.WriteLine("[DEBUG] Aucune page dans le chapitre");
                    }
                    else
                    {
                        Console.WriteLine($"[DEBUG] Chapitre assigné au ViewModel, affichage de {chapterDetail.Pages.Count} pages");
                    }
                }
                else
                {
                    ErrorMessage = "Impossible de charger les détails du chapitre.";
                    Console.WriteLine("[DEBUG] chapterDetail est null après l'appel à GetChapterPagesAsync");
                }
            }
            catch (Exception ex)
            {
                ErrorMessage = $"Erreur: {ex.Message}";
                Console.WriteLine($"[DEBUG] Exception lors du chargement: {ex.GetType().Name} - {ex.Message}");
                Console.WriteLine($"[DEBUG] Stack trace: {ex.StackTrace}");
            }
            finally
            {
                IsLoading = false;
                // Update command states
                ((RelayCommand)NextChapterCommand).RaiseCanExecuteChanged();
                ((RelayCommand)PreviousChapterCommand).RaiseCanExecuteChanged();
            }
        }
        
        // Navigation entre chapitres
        private bool CanLoadNextChapter() => !IsLoading;
        private async void LoadNextChapter()
        {
            int nextChapter = ChapterNumber + 1;
            await LoadChapterAsync(MangaTitle, ScanName, nextChapter.ToString());
        }
        
        private bool CanLoadPreviousChapter() => !IsLoading && ChapterNumber > 1;
        private async void LoadPreviousChapter()
        {
            if (ChapterNumber > 1)
            {
                int prevChapter = ChapterNumber - 1;
                await LoadChapterAsync(MangaTitle, ScanName, prevChapter.ToString());
            }
        }

        // Navigation entre les pages
        private bool CanLoadNextPage() => !IsLoading && CurrentPageIndex < PageCount - 1;
        private void LoadNextPage()
        {
            if (CanLoadNextPage())
            {
                CurrentPageIndex++;
            }
        }
        
        private bool CanLoadPreviousPage() => !IsLoading && CurrentPageIndex > 0;
        private void LoadPreviousPage()
        {
            if (CanLoadPreviousPage())
            {
                CurrentPageIndex--;
            }
        }
    }
}

