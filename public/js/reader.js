const API_URL = 'https://api.saumondeluxe.com';
const URL_PARAMS = new URLSearchParams(window.location.search);

const mangaName = URL_PARAMS.get('manga');
const chapter = URL_PARAMS.get('chapter');
const scanType = URL_PARAMS.get('scan');

async function fetchScanData(mangaName, chapter, scanType) {
    try {
        const response = await fetch(`${API_URL}/scans/chapter/pages?manga_name=${decodeURIComponent(mangaName)}&scans_type=${decodeURIComponent(scanType)}&chapter=${encodeURIComponent(chapter)}`);
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return await response.json();
    } catch (error) {
        console.error('Error fetching scan data:', error);
        return null;
    }
}

function createHeader() {
    const header = document.createElement('header');
    header.classList.add('header');

    const title = document.createElement('h1');
    title.textContent = `Lecture de ${mangaName} - Chapitre ${chapter}`;
    header.appendChild(title);

    const nav = document.createElement('nav');
    nav.classList.add('nav');

    const backLink = document.createElement('a');
    backLink.href = `../html/manga.html?slug=${encodeURIComponent(mangaName)}`;
    backLink.textContent = 'Retour à la liste des chapitres';
    nav.appendChild(backLink);

    header.appendChild(nav);

    return header;
}

function displayImgs(scanData) {
    const imgDiv = document.createElement('div');
    imgDiv.classList.add('scan-images');
    if (scanData && scanData.pages) {
        for (const page of scanData.pages) {
            const img = document.createElement('img');
            img.src = page;
            img.alt = 'Scan Image';
            img.classList.add('scan-image');
            img.style = `
                width: 100%;
                max-width: 800px; /* Adjust as needed */
                height: auto;
                display: block;
                margin: 0px auto;
            `;
            imgDiv.appendChild(img);
        }
    } else {
        imgDiv.textContent = 'No images available for this chapter.';
    }
    document.querySelector('.main').appendChild(imgDiv);
}

window.addEventListener('DOMContentLoaded', async () => {
    const scanData = await fetchScanData(mangaName, chapter, scanType);
    document.querySelector('.main').appendChild(createHeader());
    displayImgs(scanData);
});