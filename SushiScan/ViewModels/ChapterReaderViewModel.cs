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
                }
            }
        }

        public int PageCount => CurrentChapter?.Pages.Count ?? 0;
        public string PageInfo => PageCount > 0 ? $"{PageCount} pages" : "";
        public string ChapterDisplayTitle => CurrentChapter != null ? $"{CurrentChapter.MangaTitle} - Chapitre {CurrentChapter.Number}" : "Chargement...";
        
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

        public ChapterReaderViewModel()
        {
            _apiService = new ApiService();
            NextChapterCommand = new RelayCommand(_ => LoadNextChapter(), _ => CanLoadNextChapter());
            PreviousChapterCommand = new RelayCommand(_ => LoadPreviousChapter(), _ => CanLoadPreviousChapter());
        }
        
        private void LoadAllPages()
        {
            AllPages.Clear();
            
            if (CurrentChapter != null && CurrentChapter.Pages.Any())
            {
                Console.WriteLine($"[DEBUG] Chargement de {CurrentChapter.Pages.Count} pages dans la vue");
                foreach (var page in CurrentChapter.Pages)
                {
                    AllPages.Add(page);
                }
            }
        }

        public async Task LoadChapterAsync(string mangaTitle, string scanName, string chapterNumber)
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
                var chapterDetail = await _apiService.GetChapterDetailAsync(mangaTitle, scanName, chapterNumber);
                
                if (chapterDetail != null)
                {
                    Console.WriteLine($"[DEBUG] Chapitre récupéré avec {chapterDetail.Pages.Count} pages");
                    for (int i = 0; i < chapterDetail.Pages.Count; i++)
                    {
                        var page = chapterDetail.Pages[i];
                        if (page != null)
                        {
                            Console.WriteLine($"[DEBUG] Page {i+1}: Dimensions {page.Size.Width}x{page.Size.Height}, PixelSize {page.PixelSize.Width}x{page.PixelSize.Height}");
                        }
                        else
                        {
                            Console.WriteLine($"[DEBUG] Page {i+1}: NULL!");
                        }
                    }
                    
                    CurrentChapter = chapterDetail;
                    
                    if (CurrentChapter.Pages.Count == 0)
                    {
                        ErrorMessage = "Aucune page trouvée pour ce chapitre.";
                        Console.WriteLine("[DEBUG] Aucune page dans le chapitre");
                    }
                    else
                    {
                        Console.WriteLine($"[DEBUG] Chapitre assigné au ViewModel, affichage de {CurrentChapter.Pages.Count} pages");
                    }
                }
                else
                {
                    ErrorMessage = "Impossible de charger les détails du chapitre.";
                    Console.WriteLine("[DEBUG] chapterDetail est null après l'appel à GetChapterDetailAsync");
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
    }
}

