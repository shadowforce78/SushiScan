# 🍣 Sushi Scan - Structure CSS Refactorisée

## 📁 Architecture des fichiers CSS

### 1. `common.css` - Styles partagés
**Responsabilité** : Tous les styles communs entre les pages

#### Contenu :
- **Variables CSS** : Couleurs, espacements, typographie, transitions
- **Reset CSS** : Normalisation des styles de base
- **Header & Navigation** : Menu principal et mobile
- **Footer** : Pied de page
- **Composants communs** : Boutons, cartes en verre, états de chargement
- **Classes utilitaires** : Texte, marges, padding
- **Responsive design** : Media queries communes
- **Animations** : Keyframes et classes d'animation
- **Scrollbar personnalisée**

#### Variables principales :
```css
:root {
    /* Couleurs */
    --primary-gradient: linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%);
    --accent-gradient: linear-gradient(90deg, #ff6b6b 0%, #ffa500 100%);
    --secondary-gradient: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
    
    /* Effets de verre */
    --glass-light: rgba(255, 255, 255, 0.07);
    --glass-medium: rgba(255, 255, 255, 0.12);
    --glass-strong: rgba(255, 255, 255, 0.18);
    
    /* Espacements */
    --spacing-xs: 0.5rem;
    --spacing-sm: 1rem;
    --spacing-md: 1.5rem;
    --spacing-lg: 2rem;
    --spacing-xl: 3rem;
    --spacing-xxl: 4rem;
    
    /* Autres... */
}
```

### 2. `index.css` - Page d'accueil
**Responsabilité** : Styles spécifiques à la page d'accueil

#### Contenu :
- **Section de recherche** : Barre de recherche et résultats
- **Sections de contenu** : Recommandations, classiques, nouveautés
- **Carrousel** : Navigation par flèches
- **Cartes manga** : Design des cartes avec hover effects
- **Responsive spécifique** à la page d'accueil

### 3. `manga.css` - Page de détail manga
**Responsabilité** : Styles spécifiques aux pages de détail des mangas

#### Contenu :
- **Informations manga** : Layout grid desktop/mobile
- **Section chapitres** : Grille de chapitres
- **Boutons de scan** : Types de scan disponibles
- **États spéciaux** : Chapitres lus, nouveaux, chargement
- **Filtres** : Tri des chapitres
- **Responsive spécifique** à la page manga

## 🎨 Design System

### Palette de couleurs
- **Arrière-plan principal** : Dégradé sombre (0f2027 → 203a43 → 2c5364)
- **Accent principal** : Rouge-orange (#ff6b6b → #ffa500)
- **Accent secondaire** : Bleu-violet (#667eea → #764ba2)
- **Effets de verre** : Blancs transparents avec flou

### Typographie
- **Famille** : 'Inter', 'Segoe UI', system fonts
- **Poids** : 400 (normal) à 800 (extra-bold)
- **Échelle** : Responsive avec rem

### Espacements
- **Système 8pt** : 0.5rem à 4rem
- **Responsive** : Adaptation automatique mobile/desktop

### Animations
- **Durées** : Fast (0.2s), Medium (0.3s), Slow (0.5s)
- **Easings** : ease, ease-out, cubic-bezier personnalisés
- **Effets** : fadeIn, fadeInUp, pulse, shimmer

## 📱 Responsive Breakpoints

- **Mobile small** : ≤ 480px
- **Mobile large** : ≤ 768px
- **Tablet** : ≤ 1024px
- **Desktop** : > 1024px

## 🔧 Utilisation

### Ordre d'importation dans HTML :
```html
<link rel="stylesheet" href="css/common.css">
<link rel="stylesheet" href="css/[page-specific].css">
```

### Classes utilitaires disponibles :
```css
/* Texte */
.text-center, .text-left, .text-right

/* Typography */
.font-light, .font-medium, .font-semibold, .font-bold, .font-extrabold

/* Espacements */
.mb-sm, .mb-md, .mb-lg, .mb-xl
.mt-sm, .mt-md, .mt-lg, .mt-xl
.p-sm, .p-md, .p-lg, .p-xl

/* Composants */
.glass-card /* Carte avec effet de verre */
.btn, .btn-primary, .btn-accent /* Boutons */
.loading /* État de chargement */
.skeleton /* Placeholder de chargement */

/* Animations */
.fade-in, .fade-in-up, .pulse
```

## 🆕 Nouveautés de la refactorisation

### ✅ Améliorations apportées :
1. **Variables CSS** pour une cohérence totale
2. **Architecture modulaire** (common + spécifique)
3. **Design system unifié**
4. **Effets de verre** modernes avec backdrop-filter
5. **Animations fluides** et performantes
6. **Responsive mobile-first**
7. **Classes utilitaires** réutilisables
8. **Scrollbar personnalisée**
9. **États de chargement** élégants
10. **Accessibilité améliorée** (focus, contraste)

### 🎯 Avantages :
- **Maintenabilité** : Un seul endroit pour modifier les couleurs/espacements
- **Cohérence** : Design uniforme sur toutes les pages
- **Performance** : CSS optimisé et minimaliste
- **Scalabilité** : Facile d'ajouter de nouvelles pages
- **Developer Experience** : Variables explicites et documentation

### 🚀 Migration :
- ✅ `common.css` : Structure complète avec variables
- ✅ `index.css` : Page d'accueil refactorisée
- ✅ `manga.css` : Page manga refactorisée
- ✅ Compatibilité mobile/desktop maintenue
- ✅ Fonctionnalités existantes préservées

## 🔮 Prochaines étapes suggérées

1. **Tests** sur différents navigateurs/appareils
2. **Optimisation** des performances (compression CSS)
3. **Thème sombre/clair** (basé sur les variables CSS)
4. **Composants additionnels** (modales, toasts, etc.)
5. **Animations avancées** (page transitions)

---
*Documentation générée lors de la refactorisation CSS - Juillet 2025*
