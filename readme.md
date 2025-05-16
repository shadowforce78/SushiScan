# 🥷 SushiScan — Manga & Scan Reader (Avalonia)

**SushiScan** est une application cross-platform développée avec **Avalonia**. Elle permet de **rechercher**, **lire**, **télécharger** et **gérer** vos scans de manga facilement depuis une interface moderne, rapide, et minimaliste.

---

## 🧩 Fonctionnalités

- 🔍 **Recherche** rapide de scans via une API personnalisée
- 📖 **Lecteur intégré** avec navigation fluide entre les pages
- 💾 **Téléchargement local** des scans pour lecture hors-ligne
- ⭐ **Liste personnalisée** de scans à lire plus tard ou favoris
- 📚 **Bibliothèque** avec progression sauvegardée
- 🌙 Interface épurée, **dark mode** par défaut

---

## 🖼️ Aperçu de l'application

> *(À venir après les premières captures d’écran)*
Screenshots : `home`, `search`, `detail`, `reader`, `library`, `list`

---

## 🔧 Technologies utilisées

- **🖥️ UI** : [Avalonia UI](https://avaloniaui.net/) — MVVM, AXAML
- **🧠 Backend API** : FastAPI
- **📦 Stockage local** : JSON
- **🔌 HTTP Client** : `HttpClient` asynchrone (avec cache et fallback)

---

## 🗂️ Structure du projet

```

SushiScan/
│
├── Models/            # Classes représentant les entités (Scan, Chapitre, etc.)
├── ViewModels/        # Logique de présentation (MVVM)
├── Views/             # Pages XAML
├── Services/          # Appels API, gestion locale
├── Assets/            # Icônes, logos, ressources
├── App.xaml           # Ressources globales et styles
└── Program.cs         # Point d'entrée de l'application

````

---

## 🚀 Installation

### 1. Cloner le projet

```bash
git clone https://github.com/votre-utilisateur/SushiScan.git
cd SushiScan
````

### 2. Lancer le projet

Assurez-vous d’avoir [.NET 8 SDK](https://dotnet.microsoft.com/en-us/download) installé :

```bash
dotnet restore
dotnet run
```

### 3. Compiler pour release

```bash
dotnet publish -c Release -r win-x64 --self-contained
```

> Changez `win-x64` selon votre OS : `linux-x64`, `osx-arm64`, etc.

---

## 📡 Endpoints de l’API (WIP)

| Endpoint              | Méthode         | Description            |
| --------------------- | --------------- | ---------------------- |
| `/search?q=`          | GET             | Rechercher un scan     |
| `/scan/{id}`          | GET             | Détails d’un scan      |
| `/scan/{id}/read`     | GET             | Pages du chapitre      |
| `/scan/{id}/download` | POST            | Télécharger            |
| `/user/list`          | GET/POST/DELETE | Liste personnalisée    |
| `/user/library`       | GET             | Scans téléchargés      |
| `/progress/{id}`      | POST            | Sauvegarde progression |

> ⚠️ L’API est en cours de développement — endpoints susceptibles d’évoluer.

---

## ✅ TODO / Roadmap

* [ ] Design UI maquettes
* [ ] Navigation entre les pages
* [ ] Finalisation API
* [ ] Intégration complète des appels API
* [ ] Téléchargement local + progression
* [ ] Gestion de la liste utilisateur
* [ ] Builds cross-platform

---

## 🧑‍💻 Auteur

Développé avec ❤️ par **Adam Planque**<br>
📍 Saint-Quentin-en-Yvelines<br>
🎓 Étudiant en informatique (Bac+1)<br>

---

## 📃 Licence

Ce projet est open-source sous [MIT License](./LICENSE).
(Pas encore mais faite comme si)

---

## 💬 Contribuer

Les PRs sont les bienvenues si tu veux aider à enrichir la plateforme, corriger des bugs ou ajouter des fonctionnalités.

---

> 🍣 *SushiScan : parce que lire des scans, c’est encore meilleur avec du style.*
