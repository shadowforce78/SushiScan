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
    const genres = mangaInfo.manga.genres.map(genre => `<span class="genre-tag">${genre}</span>`).join('');

    const scansType = [];
    for (const type of mangaInfo.manga.scan_chapters) {
        scansType.push(`
            <button class="scan-type-btn" onclick="getScan('${type.name}', ${type.chapters_count})">
                ${type.name} (${type.chapters_count} chapitres)
            </button>
        `);
    }

    document.querySelector('.main').innerHTML = `
        <div class="manga-info">
            <img src="${mangaInfo.manga.image_url}" 
                 alt="${mangaInfo.manga.title} cover" 
                 class="manga-cover"
                 onerror="this.src='https://via.placeholder.com/200x280?text=No+Cover'">
            
            <div class="manga-info-content">
                <h1 class="manga-title">${mangaInfo.manga.title}</h1>
                
                <div class="manga-genres">
                    <strong>Genres :</strong> ${genres}
                </div>
                
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
});