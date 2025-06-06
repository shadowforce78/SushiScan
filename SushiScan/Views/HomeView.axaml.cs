using Avalonia;
using Avalonia.Controls;
using Avalonia.Markup.Xaml;
using SushiScan.ViewModels;
using System;
using System.Threading.Tasks;
using Avalonia.Threading;
using Avalonia.Input;

namespace SushiScan.Views
{
    public partial class HomeView : UserControl
    {
        private HomeViewModel? ViewModel => DataContext as HomeViewModel;
        
        // Événement déclenché lorsqu'un manga est sélectionné
        public event EventHandler<string>? OnMangaSelected;

        public HomeView()
        {
            Console.WriteLine("HomeView: Constructeur appelé");
            InitializeComponent();
            
            // S'abonner à l'événement DataContextChanged pour savoir quand le ViewModel est défini
            DataContextChanged += HomeView_DataContextChanged;
            Console.WriteLine("HomeView: Abonnement à DataContextChanged");
            
            // Essayer de charger si le DataContext est déjà défini
            if (DataContext != null)
            {
                Console.WriteLine("HomeView: DataContext déjà défini dans le constructeur");
                LoadData();
                SetupEventHandlers();
            }
            else
            {
                Console.WriteLine("HomeView: DataContext est null dans le constructeur");
            }
        }

        private void HomeView_DataContextChanged(object? sender, System.EventArgs e)
        {
            // Lancer le chargement des données lorsque le DataContext (ViewModel) est défini
            Console.WriteLine($"HomeView: DataContextChanged déclenché, ViewModel est {(ViewModel != null ? "défini" : "null")}");
            
            if (ViewModel != null)
            {
                SetupEventHandlers();
            }
            
            LoadData();
        }
        
        // Méthode pour gérer l'événement KeyDown sur la barre de recherche
        private void OnSearchBoxKeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key == Key.Enter && ViewModel != null)
            {
                ViewModel.SearchCommand.Execute(null);
                e.Handled = true;
            }
        }
        
        private void SetupEventHandlers()
        {
            if (ViewModel != null)
            {
                // S'abonner à l'événement MangaSelected du ViewModel
                ViewModel.MangaSelected += (sender, mangaTitle) =>
                {
                    Console.WriteLine($"HomeView: Manga sélectionné dans le ViewModel: {mangaTitle}");
                    // Propager l'événement
                    OnMangaSelected?.Invoke(this, mangaTitle);
                };
            }
        }

        private void LoadData()
        {
            if (ViewModel != null)
            {
                Console.WriteLine("HomeView: Démarrage du chargement des données");
                
                // Exécuter le chargement des données sur le thread UI
                Dispatcher.UIThread.Post(async () =>
                {
                    try 
                    {
                        Console.WriteLine("HomeView: Appel de LoadDataAsync");
                        await ViewModel.LoadDataAsync();
                        Console.WriteLine("HomeView: LoadDataAsync terminé avec succès");
                        
                        Console.WriteLine($"HomeView: Nombre de mangas Trending: {ViewModel.TrendingMangas.Count}");
                        Console.WriteLine($"HomeView: Nombre de mangas Popular: {ViewModel.PopularMangas.Count}");
                        Console.WriteLine($"HomeView: Nombre de mangas Recommended: {ViewModel.RecommendedMangas.Count}");
                        
                        if (!string.IsNullOrEmpty(ViewModel.ErrorMessage))
                        {
                            Console.WriteLine($"HomeView: Message d'erreur: {ViewModel.ErrorMessage}");
                        }
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine($"HomeView: Exception dans LoadData: {ex.Message}");
                        Console.WriteLine($"HomeView: Stack trace: {ex.StackTrace}");
                    }
                });
            }
            else
            {
                Console.WriteLine("HomeView: LoadData appelé mais ViewModel est null");
            }
        }

        private void InitializeComponent()
        {
            Console.WriteLine("HomeView: InitializeComponent appelé");
            AvaloniaXamlLoader.Load(this);
            Console.WriteLine("HomeView: InitializeComponent terminé");
        }
    }
}
