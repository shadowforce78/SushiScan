const API_URL = 'https://api.saumondeluxe.com'


async function getRecommended() {
    const endpoint = `${API_URL}/scans/manga/recommended`;

    try {
        const response = await fetch(endpoint);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        // I only want the recommended array where the manga as Scans,Webtoon or Manwha types
        return data.recommended.filter(manga =>
            manga.types.includes('Scans') ||
            manga.types.includes('Webtoon') ||
            manga.types.includes('Manwha')
        );
    } catch (error) {
        console.error('Error fetching recommended:', error);
        return [];
    }
}

async function getClassic() {
    const endpoint = `${API_URL}/scans/manga/classics`;

    try {
        const response = await fetch(endpoint);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        // I only want the classic array where the manga as Scans,Webtoon or Manwha types
        return data.classics.filter(manga =>
            manga.types.includes('Scans') ||
            manga.types.includes('Webtoon') ||
            manga.types.includes('Manwha')
        );
    } catch (error) {
        console.error('Error fetching classic:', error);
        return [];
    }
}

async function getLast() {
    const endpoint = `${API_URL}/scans/manga/last`;

    try {
        const response = await fetch(endpoint);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json()
        return data.last_manga
    } catch (error) {
        console.error('Error fetching last:', error);
        return [];
    }
}

async function displayLast() {
    const lastDiv = document.querySelector('.nouveau .carousel-content');
    const lastMangas = await getLast();

    if (lastMangas.length === 0) {
        lastDiv.innerHTML = '<p>Aucun manga trouvé.</p>';
        return;
    }

    lastMangas.forEach(manga => {

        const card = document.createElement('div');
        card.className = 'card';

        // Créer les badges de chapitre en cours
        const chaptersBadge = manga.latest_chapter // String par exemple : "Chapitre 153 à 155"

        card.innerHTML = `
            <button class="${nameToSlug(manga.title)}" onclick='window.location.href = "./html/manga.html?slug=${nameToSlug(manga.title)}"'>
                <img class="cover" src="${manga.image_url}" alt="${manga.title}">
                <p class="title">${manga.title}</p>
                <p class="chapters">${chaptersBadge}</p>

            </button>
        `;
        lastDiv.appendChild(card);
    });
}

async function searchManga(query) {
    const endpoint = `${API_URL}/scans/manga?query=${query}`;

    try {
        const response = await fetch(endpoint);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        const results = []

        for (const manga of data.results) {
            const title = manga.title;
            const imageUrl = manga.image_url;

            results.push({
                title: title,
                imageUrl: imageUrl
            });
        }

        return results;

    } catch (error) {
        console.error('Error fetching search results:', error);
        return [];
    }
}


async function displayClassic() {
    const classicDiv = document.querySelector('.classique .carousel-content');
    const classicMangas = await getClassic();

    if (classicMangas.length === 0) {
        classicDiv.innerHTML = '<p>Aucun manga classique trouvé.</p>';
        return;
    }

    classicMangas.forEach(manga => {

        const card = document.createElement('div');
        card.className = 'card';

        // Créer les badges de genres
        const genresBadges = manga.genres.map(genre =>
            `<span class="genre-badge">${genre}</span>`
        ).join('');

        card.innerHTML = `
            <button class="${nameToSlug(manga.title)}" onclick='window.location.href = "./html/manga.html?slug=${nameToSlug(manga.title)}"'>
                <img class="cover" src="${manga.image_url}" alt="${manga.title}">
                <p class="title">${manga.title}</p>
                <div class="genres">
                    ${genresBadges}
                </div>
            </button>
        `;
        classicDiv.appendChild(card);
    });
}

async function displayRecommended() {
    const recommendedDiv = document.querySelector('.recommandation .carousel-content');
    const recommendedMangas = await getRecommended();

    if (recommendedMangas.length === 0) {
        recommendedDiv.innerHTML = '<p>Aucun manga recommandé trouvé.</p>';
        return;
    }

    recommendedMangas.forEach(manga => {

        const card = document.createElement('div');
        card.className = 'card';

        // Créer les badges de genres
        const genresBadges = manga.genres.map(genre =>
            `<span class="genre-badge">${genre}</span>`
        ).join('');

        card.innerHTML = `
            <button class="${nameToSlug(manga.title)}" onclick='window.location.href = "./html/manga.html?slug=${nameToSlug(manga.title)}"'>
                <img class="cover" src="${manga.image_url}" alt="${manga.title}">
                <p class="title">${manga.title}</p>
                <div class="genres">
                    ${genresBadges}
                </div>
            </button>
        `;
        recommendedDiv.appendChild(card);
    });
}

// Fonction pour faire défiler le carrousel
function scrollCarousel(section, direction) {
    const carousel = document.querySelector(`.${section} .carousel-content`);
    const cardWidth = 280 + 30; // largeur de la card + gap
    const scrollAmount = cardWidth * 3; // Défiler de 3 cards à la fois

    carousel.scrollBy({
        left: direction * scrollAmount,
        behavior: 'smooth'
    });
}

function nameToSlug(name) {
    return encodeURIComponent(name)
}

function displaySearchResults(results) {
    const searchResults = document.getElementById('searchResults');
    const searchResultsContent = document.querySelector('.search-results-content');
    
    if (results.length === 0) {
        searchResultsContent.innerHTML = '<p style="color: #fff; text-align: center; grid-column: 1 / -1;">Aucun manga trouvé.</p>';
    } else {
        searchResultsContent.innerHTML = results.map(manga => `
            <div class="search-result-item" onclick="window.location.href = './html/manga.html?slug=${nameToSlug(manga.title)}'">
                <img src="${manga.imageUrl}" alt="${manga.title}">
                <p class="title">${manga.title}</p>
            </div>
        `).join('');
    }
    
    searchResults.style.display = 'block';
    searchResults.scrollIntoView({ behavior: 'smooth' });
}

function closeSearchResults() {
    const searchResults = document.getElementById('searchResults');
    const searchInput = document.getElementById('searchInput');
    
    searchResults.style.display = 'none';
    searchInput.value = '';
}

function performSearch() {
    const searchInput = document.getElementById('searchInput');
    const query = searchInput.value.trim();
    
    if (query) {
        searchManga(query).then(results => {
            displaySearchResults(results);
        });
    }
}

window.addEventListener('DOMContentLoaded', async () => {
    await displayRecommended();
    await displayClassic();
    await displayLast();
    
    // Configuration de la barre de recherche
    const searchInput = document.getElementById('searchInput');
    const searchButton = document.getElementById('searchButton');

    if (searchButton) {
        searchButton.addEventListener('click', performSearch);
    }

    if (searchInput) {
        searchInput.addEventListener('keypress', (event) => {
            if (event.key === 'Enter') {
                performSearch();
            }
        });
        
        // Fermer les résultats si on clique sur Escape
        searchInput.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') {
                closeSearchResults();
            }
        });
    }
});