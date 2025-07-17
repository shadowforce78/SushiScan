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
            <button class="${manga.slug}" onclick='alert("Manga: ${manga.title}")'>
                <img class="cover" src="${manga.image_url}" alt="${manga.title}">
                <p class="title">${manga.title}</p>
                <p class="chapters">${chaptersBadge}</p>

            </button>
        `;
        lastDiv.appendChild(card);
    });
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
            <button class="${manga.slug}" onclick='alert("Manga: ${manga.title}")'>
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
            <button class="${manga.slug}" onclick='alert("Manga: ${manga.title}")'>
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

window.addEventListener('DOMContentLoaded', async () => {
    await displayRecommended();
    await displayClassic();
    await displayLast();
});