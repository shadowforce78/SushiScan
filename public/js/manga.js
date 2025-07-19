// Configuration de l'API
const API_URL = "https://api.saumondeluxe.com";
const mangaName = new URLSearchParams(window.location.search).get("slug");

// Variables globales
let currentManga = null;
let isLoading = false;

// Fonction utilitaire pour formater les genres avec limite
function formatGenres(genres, isMobile = false) {
    if (!genres || genres.length === 0) return '<span class="no-genres">Aucun genre spécifié</span>';
    
    const maxGenres = isMobile ? 6 : 8; // Limiter davantage sur mobile
    
    if (genres.length <= maxGenres) {
        return genres.map(genre => `<span class="genre-tag">${genre}</span>`).join('');
    }
    
    const visibleGenres = genres.slice(0, maxGenres - 1);
    const remainingCount = genres.length - visibleGenres.length;
    
    const visibleGenresHtml = visibleGenres.map(genre => `<span class="genre-tag">${genre}</span>`).join('');
    const moreGenresHtml = `<span class="genre-tag more-genres" title="${genres.slice(maxGenres - 1).join(', ')}">+${remainingCount} autres</span>`;
    
    return visibleGenresHtml + moreGenresHtml;
}

// Fonction utilitaire pour détecter si on est sur mobile
function isMobileDevice() {
    return window.innerWidth <= 768;
}

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
async function getScan(scanType, chaptersCount) {
    if (isLoading) return;

    isLoading = true;
    const chaptersContainer = document.getElementById('chapters-container');

    try {
        displayChapters(scanType, chaptersCount);

    } catch (error) {
        console.error('Error fetching chapters:', error);
        if (chaptersContainer) {
            chaptersContainer.innerHTML = '<div class="loading">Erreur lors du chargement des chapitres.</div>';
        }
    } finally {
        isLoading = false;
    }
}

// Fonction pour afficher les chapitres
function displayChapters(scanType, chaptersCount) {
    const chaptersContainer = document.getElementById('chapters-container');

    if (!chaptersContainer) return;

    if (chaptersCount === 0) {
        chaptersContainer.innerHTML = '<div class="loading">Aucun chapitre disponible pour ce scan.</div>';
        return;
    }

    try {

        chaptersContainer.innerHTML = ''; // Clear previous content
        for (let i = 0; i < chaptersCount; i++) {
            const button = document.createElement('button');
            button.className = 'chapter-button';
            button.textContent = `Chapitre ${i + 1}`;
            button.onclick = () => {
                window.location.href = `reader.html?manga=${encodeURIComponent(mangaName)}&chapter=${i + 1}&scan=${encodeURIComponent(scanType)}`;
            };
            chaptersContainer.appendChild(button);
        }
    } catch (error) {
        console.error('Error displaying chapters:', error);
    } finally {
        isLoading = false;
    }
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
            <div>
                <h2>Erreur</h2>
                <p>Aucun manga spécifié.</p>
                <a href="../index.html">Retour à l'accueil</a>
            </div>
        `;
        return;
    }

    const mangaInfo = await getInfo(mangaName);

    if (!mangaInfo) {
        document.querySelector('.main').innerHTML = `
            <div>
                <h2>Erreur</h2>
                <p>Impossible de charger les informations du manga.</p>
                <a href="../index.html">Retour à l'accueil</a>
            </div>
        `;
        return;
    }

    currentManga = mangaInfo.manga;
    const isMobile = isMobileDevice();
    const genres = formatGenres(mangaInfo.manga.genres, isMobile);

    const scansType = [];
    for (const type of mangaInfo.manga.scan_chapters) {
        scansType.push(`
            <button class="scan-type-btn" onclick="getScan('${type.name}', ${type.chapters_count})">
                ${type.name} (${type.chapters_count} chapitres)
            </button>
        `);
    }

    // Ajouter une description si elle existe dans les données
    const descriptionHtml = mangaInfo.manga.description ? `
        <div class="manga-description">
            <p>${mangaInfo.manga.description}</p>
        </div>
    ` : '';

    // Ajouter des statistiques si elles existent
    const statsHtml = (mangaInfo.manga.rating || mangaInfo.manga.status || mangaInfo.manga.year) ? `
        <div class="manga-stats">
            ${mangaInfo.manga.rating ? `<div class="stat-item"><div class="stat-label">Note</div><div class="stat-value">${mangaInfo.manga.rating}/10</div></div>` : ''}
            ${mangaInfo.manga.status ? `<div class="stat-item"><div class="stat-label">Statut</div><div class="stat-value">${mangaInfo.manga.status}</div></div>` : ''}
            ${mangaInfo.manga.year ? `<div class="stat-item"><div class="stat-label">Année</div><div class="stat-value">${mangaInfo.manga.year}</div></div>` : ''}
        </div>
    ` : '';

    document.querySelector('.main').innerHTML = `
        <div class="manga-info">
            <img src="${mangaInfo.manga.image_url}" 
                 alt="${mangaInfo.manga.title} cover" 
                 class="manga-cover"
                 onerror="this.src='https://via.placeholder.com/400x520/2c5364/ffffff?text=Couverture+non+disponible'">
            
            <div class="manga-info-content">
                <h1 class="manga-title">${mangaInfo.manga.title}</h1>
                
                ${descriptionHtml}
                
                <div class="manga-genres">
                    <strong>Genres :</strong>
                    ${genres}
                </div>
                
                ${statsHtml}
                
                <div class="scan-types">
                    <h3>Types de scan disponibles :</h3>
                    ${scansType.join('')}
                </div>
            </div>
        </div>
        
        <div class="chapters-section">
            <h2>Chapitres</h2>
            <div id="chapters-container" class="chapters-grid">
                <div class="loading">Sélectionnez un type de scan pour voir les chapitres disponibles.</div>
            </div>
        </div>
    `;

    // Ajouter un gestionnaire pour l'indicateur "+X autres" genres
    const moreGenresBtn = document.querySelector('.genre-tag.more-genres');
    if (moreGenresBtn) {
        moreGenresBtn.addEventListener('click', () => {
            const allGenres = mangaInfo.manga.genres.map(genre => `<span class="genre-tag">${genre}</span>`).join('');
            const genresContainer = document.querySelector('.manga-genres');
            const strong = genresContainer.querySelector('strong');
            genresContainer.innerHTML = '';
            genresContainer.appendChild(strong);
            genresContainer.insertAdjacentHTML('beforeend', allGenres);
        });
    }
});

// Gestionnaire de redimensionnement pour adapter l'affichage des genres
window.addEventListener('resize', () => {
    if (currentManga && currentManga.genres) {
        const genresContainer = document.querySelector('.manga-genres');
        if (genresContainer && !genresContainer.querySelector('.genre-tag.more-genres')) {
            // Seulement si on n'a pas déjà étendu la vue
            const isMobile = isMobileDevice();
            const genres = formatGenres(currentManga.genres, isMobile);
            const strong = genresContainer.querySelector('strong');
            if (strong) {
                genresContainer.innerHTML = '';
                genresContainer.appendChild(strong);
                genresContainer.insertAdjacentHTML('beforeend', genres);
            }
        }
    }
});