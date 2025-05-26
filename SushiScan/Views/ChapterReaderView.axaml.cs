using Avalonia;
using Avalonia.Controls;
using Avalonia.Markup.Xaml;
using System;
using SushiScan.ViewModels;

namespace SushiScan.Views
{
    public partial class ChapterReaderView : UserControl
    {
        public ChapterReaderView()
        {
            InitializeComponent();
            
            Console.WriteLine("[DEBUG] ChapterReaderView: Constructeur appelé");
            
            this.AttachedToVisualTree += OnAttachedToVisualTree;
            this.DetachedFromVisualTree += OnDetachedFromVisualTree;
            this.DataContextChanged += OnDataContextChanged;
        }

        public ChapterReaderView(ChapterReaderViewModel viewModel)
        {
            InitializeComponent();
            DataContext = viewModel;
            Console.WriteLine("[DEBUG] ChapterReaderView: Constructeur avec ViewModel appelé");
        }
        
        private void OnAttachedToVisualTree(object? sender, VisualTreeAttachmentEventArgs e)
        {
            Console.WriteLine("[DEBUG] ChapterReaderView: Attaché à l'arbre visuel");
            if (DataContext is ChapterReaderViewModel vm)
            {
                Console.WriteLine($"[DEBUG] DataContext est ChapterReaderViewModel, AllPages: {(vm.AllPages?.Count > 0 ? $"{vm.AllPages.Count} pages" : "aucune page")}");
            }
            else
            {
                Console.WriteLine($"[DEBUG] DataContext n'est pas ChapterReaderViewModel: {DataContext?.GetType().Name ?? "null"}");
            }
        }
        
        private void OnDetachedFromVisualTree(object? sender, VisualTreeAttachmentEventArgs e)
        {
            Console.WriteLine("[DEBUG] ChapterReaderView: Détaché de l'arbre visuel");
        }
        
        private void OnDataContextChanged(object? sender, EventArgs e)
        {
            Console.WriteLine("[DEBUG] ChapterReaderView: DataContext changé");
            if (DataContext is ChapterReaderViewModel vm)
            {
                Console.WriteLine($"[DEBUG] Nouveau DataContext est ChapterReaderViewModel, AllPages: {(vm.AllPages?.Count > 0 ? $"{vm.AllPages.Count} pages" : "aucune page")}");
                vm.PropertyChanged += ViewModel_PropertyChanged;
            }
            else
            {
                Console.WriteLine($"[DEBUG] Nouveau DataContext n'est pas ChapterReaderViewModel: {DataContext?.GetType().Name ?? "null"}");
            }
        }

        private void ViewModel_PropertyChanged(object? sender, System.ComponentModel.PropertyChangedEventArgs e)
        {
            if (e.PropertyName == nameof(ChapterReaderViewModel.CurrentChapter))
            {
                // Défiler vers le haut lorsque le chapitre change
                var scrollViewer = this.FindControl<ScrollViewer>("PageScrollViewer");
                scrollViewer?.ScrollToHome();
            }
        }
    }
}
