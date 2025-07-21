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

async function getMaxChapter(mangaName, scanType) {
    try {
        const response = await fetch(`${API_URL}/scans/manga/info/?manga_name=${decodeURIComponent(mangaName)}`);
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        const mangaInfo = await response.json();
        const scanData = mangaInfo.manga.scan_chapters;
        if (scanData && scanData.length > 0) {
            const scan = scanData.find(s => s.name.toLowerCase() === scanType.toLowerCase());
            if (scan) {
                return await scan.chapters_count;
            }
        }
        throw new Error('Scan type not found or no chapters available');
    } catch (error) {
        console.error('Error fetching scan data:', error);
        return null;
    }
}

async function createHeader() {
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

    // Navigate to the previous chapter or the next chapter
    if (parseInt(chapter) <= 1) {
        // Does not exist
        const prevLink = document.createElement('span');
        prevLink.textContent = 'Chapitre Précédent (Non disponible)';
        nav.appendChild(prevLink);
    } else {
        const prevLink = document.createElement('a');
        prevLink.href = `../html/reader.html?manga=${encodeURIComponent(mangaName)}&chapter=${encodeURIComponent(parseInt(chapter) - 1)}&scan=${encodeURIComponent(scanType)}`;
        prevLink.textContent = 'Chapitre Précédent';
        nav.appendChild(prevLink);
    }

    if (chapter >= await getMaxChapter(mangaName, scanType)) {
        // Does not exist
        const nextLink = document.createElement('span');
        nextLink.textContent = 'Chapitre Suivant (Non disponible)';
        nav.appendChild(nextLink);
    } else {
        const nextLink = document.createElement('a');
        nextLink.href = `../html/reader.html?manga=${encodeURIComponent(mangaName)}&chapter=${encodeURIComponent(parseInt(chapter) + 1)}&scan=${encodeURIComponent(scanType)}`;
        nextLink.textContent = 'Chapitre Suivant';
        nav.appendChild(nextLink);

    }
    header.appendChild(nav);
    return await header;
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
    document.querySelector('.main').appendChild(await createHeader());
    displayImgs(scanData);
});