const API_URL = "https://api.saumondeluxe.com";
const mangaName = new URLSearchParams(window.location.search).get("slug");

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

window.addEventListener('DOMContentLoaded', async () => {
    const mangaInfo = await getInfo(mangaName);
    const genres = mangaInfo.manga.genres.map(genre => `<span class="genre">${genre}</span>`).join(', ');


    const scansType = []
    for (const type of mangaInfo.manga.scan_chapters) {
        scansType.push(`<button class="scan-type" onclick="getScan('${type.name}')">${type.name} : ${type.chapters_count} chapitres disponibles</button>`);
    }

    if (mangaInfo) {
        document.querySelector('.main').innerHTML = `
            <img src="${mangaInfo.manga.image_url}" alt="${mangaInfo.manga.title} cover" class="cover">
            <p class="title">${mangaInfo.manga.title}</p>
            <div class="genres">${genres}</div>
            <div class="scan-types">${scansType.join(' ')}</div>
        `;
    } else {
        document.querySelector('.main').innerHTML = '<p>Error loading manga information.</p>';
    }
});