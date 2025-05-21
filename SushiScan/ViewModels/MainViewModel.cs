using CommunityToolkit.Mvvm.ComponentModel;

namespace SushiScan.ViewModels;

public partial class MainViewModel : ViewModelBase
{
    public HomeViewModel HomeViewModel { get; }
    
    public MainViewModel()
    {
        HomeViewModel = new HomeViewModel();
    }
}

