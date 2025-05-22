using System;
using CommunityToolkit.Mvvm.ComponentModel;

namespace SushiScan.ViewModels;

public partial class MainViewModel : ViewModelBase
{
    private bool _showMangaDetail;
    private string _selectedMangaTitle = string.Empty;
    
    public HomeViewModel HomeViewModel { get; }
    public MangaDetailViewModel MangaDetailViewModel { get; }
    
    public bool ShowMangaDetail
    {
        get => _showMangaDetail;
        set
        {
            if (_showMangaDetail != value)
            {
                _showMangaDetail = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(ShowHomeView)); // Notifier le changement pour la propriété dépendante
            }
        }
    }

    // Nouvelle propriété pour contrôler la visibilité de HomeView
    public bool ShowHomeView => !_showMangaDetail;

    public MainViewModel()
    {
        HomeViewModel = new HomeViewModel();
        MangaDetailViewModel = new MangaDetailViewModel();
        
        // S'abonner à l'événement de sélection d'un manga
        HomeViewModel.MangaSelected += OnMangaSelected;
    }
    
    private void OnMangaSelected(object? sender, string mangaTitle)
    {
        Console.WriteLine($"MainViewModel: Manga sélectionné: {mangaTitle}");
        
        _selectedMangaTitle = mangaTitle;
        MangaDetailViewModel.MangaTitle = mangaTitle;
        ShowMangaDetail = true;
    }
    
    public void NavigateToHome()
    {
        ShowMangaDetail = false;
    }
}
