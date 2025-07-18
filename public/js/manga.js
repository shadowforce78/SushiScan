const API_URL = "https://api.saumondeluxe.com";
const mangaName = new URLSearchParams(window.location.search).get("slug");

async function getInfo(mangaName){
    const endpoint = `${API_URL}/scans/manga/info?manga_name=${mangaName}`;

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
    if (mangaInfo) {
        document.querySelector('.main').innerHTML = `
            <h1>${mangaInfo.manga.title}</h1>
        `;
    } else {
        document.querySelector('.main').innerHTML = '<p>Error loading manga information.</p>';
    }
});