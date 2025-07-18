// Configuration de l'API
const API_URL = "https://api.saumondeluxe.com";
const mangaName = new URLSearchParams(window.location.search).get("slug");

// Variables globales
let currentManga = null;
let isLoading = false;

// Fonction pour obtenir les informations du manga
async function getInfo(mangaName) {
    const endpoint = `${API_URL}/scans/manga/info?manga_name=${encodeURIComponent(mangaName)}`;
    console.info('Fetching manga info from:', endpoint);
    try {
        const response = await fetch(endpoint);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('Error fetching manga info:', error);
        return null;
    }
}

// Fonction pour obtenir les chapitres d'un scan
async function getScan(scanType) {
    if (isLoading) return;

    isLoading = true;
    const chaptersContainer = document.getElementById('chapters-container');

    if (chaptersContainer) {
        chaptersContainer.innerHTML = '<div class="loading">Chargement des chapitres...</div>';
    }

    try {
        const endpoint = `${API_URL}/scans/manga/chapters?manga_name=${encodeURIComponent(mangaName)}&scan_type=${encodeURIComponent(scanType)}`;
        const response = await fetch(endpoint);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        displayChapters(data.chapters || [], scanType);

    } catch (error) {
        console.error('Error fetching chapters:', error);
        if (chaptersContainer) {
            chaptersContainer.innerHTML = '<div class="error-message">Erreur lors du chargement des chapitres.</div>';
        }
    } finally {
        isLoading = false;
    }
}

// Fonction pour afficher les chapitres
function displayChapters(chapters, scanType) {
    const chaptersContainer = document.getElementById('chapters-container');

    if (!chaptersContainer) return;

    if (chapters.length === 0) {
        chaptersContainer.innerHTML = '<div class="no-chapters">Aucun chapitre disponible pour ce scan.</div>';
        return;
    }

    // Trier les chapitres par numéro décroissant
    chapters.sort((a, b) => parseFloat(b.chapter_number) - parseFloat(a.chapter_number));

    const chaptersHTML = chapters.map((chapter, index) => {
        const isNew = index < 3; // Les 3 derniers chapitres sont considérés comme nouveaux
        const chapterNumber = chapter.chapter_number;
        const releaseDate = chapter.release_date ? new Date(chapter.release_date).toLocaleDateString('fr-FR') : 'Date inconnue';

        return `
            <div class="chapter-card" onclick="readChapter('${chapter.chapter_id}', '${scanType}')">
                ${isNew ? '<div class="chapter-new">NOUVEAU</div>' : ''}
                <h3 class="chapter-title">Chapitre ${chapterNumber}</h3>
                <p class="chapter-date">Publié le ${releaseDate}</p>
            </div>
        `;
    }).join('');

    chaptersContainer.innerHTML = chaptersHTML;
}

// Fonction pour lire un chapitre
function readChapter(chapterId, scanType) {
    // Rediriger vers la page de lecture
    window.location.href = `reader.html?manga=${encodeURIComponent(mangaName)}&chapter=${chapterId}&scan=${encodeURIComponent(scanType)}`;
}

// Fonction pour basculer les favoris
function toggleFavorite() {
    if (!currentManga) return;

    const favorites = JSON.parse(localStorage.getItem('favorites') || '[]');
    const mangaSlug = mangaName;

    if (favorites.includes(mangaSlug)) {
        const index = favorites.indexOf(mangaSlug);
        favorites.splice(index, 1);
        showNotification('Retiré des favoris', 'success');
    } else {
        favorites.push(mangaSlug);
        showNotification('Ajouté aux favoris', 'success');
    }

    localStorage.setItem('favorites', JSON.stringify(favorites));
}

// Fonction pour basculer les marque-pages
function toggleBookmark() {
    if (!currentManga) return;

    const bookmarks = JSON.parse(localStorage.getItem('bookmarks') || '[]');
    const mangaSlug = mangaName;

    if (bookmarks.includes(mangaSlug)) {
        const index = bookmarks.indexOf(mangaSlug);
        bookmarks.splice(index, 1);
        showNotification('Marque-page retiré', 'success');
    } else {
        bookmarks.push(mangaSlug);
        showNotification('Marque-page ajouté', 'success');
    }

    localStorage.setItem('bookmarks', JSON.stringify(bookmarks));
}

// Fonction pour afficher les notifications
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${type === 'success' ? '#4CAF50' : '#2196F3'};
        color: white;
        padding: 15px 20px;
        border-radius: 5px;
        z-index: 1000;
        animation: slideIn 0.3s ease;
    `;

    document.body.appendChild(notification);

    setTimeout(() => {
        notification.remove();
    }, 3000);
}

// Fonction pour gérer le menu burger
function toggleMobileMenu() {
    const burger = document.querySelector('.burger');
    const mobileMenu = document.getElementById('mobileMenu');

    burger.classList.toggle('active');
    mobileMenu.classList.toggle('active');
}

function closeMobileMenu() {
    const burger = document.querySelector('.burger');
    const mobileMenu = document.getElementById('mobileMenu');

    burger.classList.remove('active');
    mobileMenu.classList.remove('active');
}

// Initialisation au chargement de la page
window.addEventListener('DOMContentLoaded', async () => {
    if (!mangaName) {
        document.querySelector('.main').innerHTML = `
            <div class="error-message">
                <h2>Erreur</h2>
                <p>Aucun manga spécifié.</p>
                <button onclick="window.location.href='../index.html'" class="action-btn primary">
                    Retour à l'accueil
                </button>
            </div>
        `;
        return;
    }

    const mangaInfo = await getInfo(mangaName);

    if (!mangaInfo) {
        document.querySelector('.main').innerHTML = `
            <div class="error-message">
                <h2>Erreur</h2>
                <p>Impossible de charger les informations du manga.</p>
                <button onclick="window.location.href='../index.html'" class="action-btn primary">
                    Retour à l'accueil
                </button>
            </div>
        `;
        return;
    }

    currentManga = mangaInfo.manga;
    const genres = mangaInfo.manga.genres.map(genre => `<span class="genre-tag">${genre}</span>`).join('');

    const scansType = [];
    for (const type of mangaInfo.manga.scan_chapters) {
        scansType.push(`
            <button class="scan-type-btn" onclick="getScan('${type.name}')">
                ${type.name} <span class="chapter-count">${type.chapters_count} chapitres</span>
            </button>
        `);
    }

    document.querySelector('.main').innerHTML = `
        <div class="manga-hero">
            <div class="manga-cover-container">
                <img src="${mangaInfo.manga.image_url}" 
                     alt="${mangaInfo.manga.title} cover" 
                     class="manga-cover"
                     onerror="this.src='https://via.placeholder.com/300x400?text=No+Cover'">
            </div>
            <div class="manga-info">
                <h1 class="manga-title">${mangaInfo.manga.title}</h1>
                
                <div class="manga-stats">
                    <div class="stat-item">
                        <span class="stat-value">${mangaInfo.manga.scan_chapters.reduce((total, scan) => total + scan.chapters_count, 0)}</span>
                        <span class="stat-label">Chapitres</span>
                    </div>
                </div>
                
                <div class="manga-genres">
                    ${genres}
                </div>
                
                <div class="manga-actions">
                    <button onclick="toggleFavorite()" class="action-btn primary">
                        ❤️ Favoris
                    </button>
                    <button onclick="toggleBookmark()" class="action-btn secondary">
                        🔖 Marquer
                    </button>
                </div>
            </div>
        </div>
        
        <div class="scan-types-section">
            <h2 class="section-title">Types de scan disponibles</h2>
            <div class="scan-types">
                ${scansType.join('')}
            </div>
        </div>
        
        <div class="chapters-section">
            <h2 class="section-title">Chapitres</h2>
            <div id="chapters-container" class="chapters-grid">
                <div class="select-scan-message">
                    <p>Sélectionnez un type de scan pour voir les chapitres disponibles.</p>
                </div>
            </div>
        </div>
    `;
});

// Ajouter les styles CSS pour les animations et nouveaux éléments
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    .error-message {
        text-align: center;
        color: white;
        padding: 50px;
        background: rgba(255, 255, 255, 0.1);
        backdrop-filter: blur(15px);
        border-radius: 20px;
        margin: 50px auto;
        max-width: 500px;
    }
    
    .error-message h2 {
        margin-bottom: 20px;
        font-size: 24px;
    }
    
    .error-message p {
        margin-bottom: 30px;
        font-size: 16px;
    }
    
    .no-chapters {
        text-align: center;
        color: white;
        padding: 50px;
        grid-column: 1 / -1;
    }
    
    .select-scan-message {
        text-align: center;
        color: white;
        padding: 50px;
        grid-column: 1 / -1;
        opacity: 0.8;
    }
    
    .scan-types-section {
        margin: 40px 0;
        text-align: center;
    }
    
    .scan-types {
        display: flex;
        flex-wrap: wrap;
        gap: 15px;
        justify-content: center;
        margin-top: 20px;
    }
    
    .scan-type-btn {
        background: rgba(255, 255, 255, 0.1);
        backdrop-filter: blur(15px);
        border: 2px solid rgba(255, 255, 255, 0.2);
        border-radius: 25px;
        padding: 15px 25px;
        color: white;
        cursor: pointer;
        font-size: 16px;
        font-weight: 600;
        transition: all 0.3s ease;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 5px;
    }
    
    .scan-type-btn:hover {
        background: rgba(255, 255, 255, 0.2);
        border-color: rgba(255, 255, 255, 0.4);
        transform: translateY(-2px);
        box-shadow: 0 10px 25px rgba(0, 0, 0, 0.3);
    }
    
    .chapter-count {
        font-size: 12px;
        color: #ffd700;
        font-weight: 400;
    }
    
    @media (max-width: 768px) {
        .scan-types {
            flex-direction: column;
            align-items: center;
        }
        
        .scan-type-btn {
            width: 100%;
            max-width: 300px;
        }
    }
`;
document.head.appendChild(style);