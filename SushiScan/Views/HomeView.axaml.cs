using Avalonia;
using Avalonia.Controls;
using Avalonia.Markup.Xaml;
using SushiScan.ViewModels;
using System;
using System.Threading.Tasks;
using Avalonia.Threading;

namespace SushiScan.Views
{
    public partial class HomeView : UserControl
    {
        private HomeViewModel? ViewModel => DataContext as HomeViewModel;

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
            LoadData();
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
