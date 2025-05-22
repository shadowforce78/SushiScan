using System;
using System.Globalization;
using Avalonia.Data.Converters;

namespace SushiScan.Converters
{
    public static class MathConverters
    {
        public static readonly IValueConverter Subtract = new SubtractValueConverter();
        
        private class SubtractValueConverter : IValueConverter
        {
            public object? Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
            {
                if (value is int intValue && parameter is string paramString && int.TryParse(paramString, out int paramValue))
                {
                    return intValue - paramValue;
                }
                
                if (value is int intVal && parameter == null)
                {
                    return intVal - 1; // Default subtract 1
                }
                
                return value;
            }

            public object? ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture)
            {
                if (value is int intValue && parameter is string paramString && int.TryParse(paramString, out int paramValue))
                {
                    return intValue + paramValue;
                }
                
                if (value is int intVal && parameter == null)
                {
                    return intVal + 1; // Default add 1
                }
                
                return value;
            }
        }
    }
}
