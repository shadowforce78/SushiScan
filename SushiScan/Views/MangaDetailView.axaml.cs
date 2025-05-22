using Avalonia;
using Avalonia.Controls;
using Avalonia.Markup.Xaml;
using SushiScan.ViewModels;
using System;
using Avalonia.Interactivity;

namespace SushiScan.Views
{
    public partial class MangaDetailView : UserControl
    {
        private MangaDetailViewModel? ViewModel => DataContext as MangaDetailViewModel;

        public MangaDetailView()
        {
            InitializeComponent();
        }

        private void InitializeComponent()
        {
            AvaloniaXamlLoader.Load(this);
        }
        
        public void SetMangaTitle(string title)
        {
            if (ViewModel != null)
            {
                Console.WriteLine($"MangaDetailView: Définition du titre du manga: {title}");
                ViewModel.MangaTitle = title;
            }
            else
            {
                Console.WriteLine("MangaDetailView: ViewModel est null lors de la tentative de définition du titre");
            }
        }
    }
}
