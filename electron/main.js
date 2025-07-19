const { app, BrowserWindow } = require('electron');
const path = require('path');

function createWindow() {
    const win = new BrowserWindow({
        width: 1200,
        height: 800,
        webPreferences: {
            contextIsolation: true
        },
        autoHideMenuBar: true
    });

    win.loadFile(path.join(__dirname, '../public', 'index.html'));
}

app.whenReady().then(createWindow);
