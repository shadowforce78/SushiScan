using System;
using CommunityToolkit.Mvvm.ComponentModel;

namespace SushiScan.ViewModels;

public partial class MainViewModel : ViewModelBase
{
    private bool _showMangaDetail;
    private bool _showChapterReader;
    private string _selectedMangaTitle = string.Empty;
    
    public HomeViewModel HomeViewModel { get; }
    public MangaDetailViewModel MangaDetailViewModel { get; }
    public ChapterReaderViewModel? ChapterReaderViewModel { get; private set; }
    
    public bool ShowMangaDetail
    {
        get => _showMangaDetail;
        set
        {
            if (_showMangaDetail != value)
            {
                _showMangaDetail = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(ShowHomeView));
                if (_showMangaDetail)
                {
                    ShowChapterReader = false;
                }
            }
        }
    }
    
    public bool ShowChapterReader
    {
        get => _showChapterReader;
        set
        {
            if (_showChapterReader != value)
            {
                _showChapterReader = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(ShowHomeView));
                OnPropertyChanged(nameof(ShowMangaDetail));
                if (_showChapterReader)
                {
                    _showMangaDetail = false;
                }
            }
        }
    }

    // Propriété pour contrôler la visibilité de HomeView
    public bool ShowHomeView => !_showMangaDetail && !_showChapterReader;

    public MainViewModel()
    {
        HomeViewModel = new HomeViewModel();
        MangaDetailViewModel = new MangaDetailViewModel();
        
        // S'abonner à l'événement de sélection d'un manga
        HomeViewModel.MangaSelected += OnMangaSelected;
        
        // S'abonner à l'événement de sélection d'un chapitre
        MangaDetailViewModel.ChapterSelected += OnChapterSelected;
    }
    
    private void OnMangaSelected(object? sender, string mangaTitle)
    {
        Console.WriteLine($"MainViewModel: Manga sélectionné: {mangaTitle}");
        
        _selectedMangaTitle = mangaTitle;
        MangaDetailViewModel.MangaTitle = mangaTitle;
        ShowMangaDetail = true;
        ShowChapterReader = false;
    }
    
    private void OnChapterSelected(object? sender, ChapterReaderViewModel chapterReaderViewModel)
    {
        Console.WriteLine($"[DEBUG] MainViewModel.OnChapterSelected: Chapitre sélectionné");
        
        ChapterReaderViewModel = chapterReaderViewModel;
        
        // Forcer l'état de visibilité
        _showMangaDetail = false;
        _showChapterReader = true;
        
        // Notifier tous les changements de propriétés
        OnPropertyChanged(nameof(ShowMangaDetail));
        OnPropertyChanged(nameof(ShowChapterReader));
        OnPropertyChanged(nameof(ShowHomeView));
        OnPropertyChanged(nameof(ChapterReaderViewModel));
        
        Console.WriteLine($"[DEBUG] MainViewModel: État des propriétés - ShowHomeView={ShowHomeView}, ShowMangaDetail={ShowMangaDetail}, ShowChapterReader={ShowChapterReader}");
    }
    
    public void NavigateToHome()
    {
        ShowMangaDetail = false;
        ShowChapterReader = false;
    }
}
