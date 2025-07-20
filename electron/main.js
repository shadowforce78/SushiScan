const { app, BrowserWindow } = require('electron');
const path = require('path');

function createWindow() {
    const win = new BrowserWindow({
        width: 1200,
        height: 800,
        webPreferences: {
            contextIsolation: true
        },
        autoHideMenuBar: true,
        icon: path.join(__dirname, '../resources', 'icon.png')
    });

    win.loadFile(path.join(__dirname, '../public', 'index.html'));
}

app.whenReady().then(createWindow);
