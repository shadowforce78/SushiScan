    /**
     * Charge les chapitres depuis la base de données locale
     */
    private void loadChaptersFromDatabase() {
        dbExecutor.execute(() -> {
            // Vérifier s'il y a des chapitres téléchargés pour ce manga
            List<ChapterEntity> dbChapters = chapterDao.getChaptersByMangaSync(nameForInfo, scanType);

            if (dbChapters != null && !dbChapters.isEmpty()) {
                // Convertir les entités de base de données en objets Chapter
                List<Chapter> localChapters = new ArrayList<>();

                for (ChapterEntity entity : dbChapters) {
                    if (entity.isDownloaded()) {
                        // Charger les pages de ce chapitre
                        List<PageImageEntity> pages = pageImageDao.getPagesByChapterSync(entity.getId());
                        if (!pages.isEmpty()) {
                            Chapter chapter = new Chapter();
                            chapter.number = entity.getNumber();
                            chapter.name = entity.getName();
                            chapter.databaseId = entity.getId();
                            chapter.isDownloaded = true;

                            // Charger les chemins locaux des images
                            List<String> localPaths = new ArrayList<>();
                            for (PageImageEntity page : pages) {
                                if (page.isDownloaded() && page.getLocalImagePath() != null) {
                                    localPaths.add("file://" + page.getLocalImagePath());
                                }
                            }

                            if (!localPaths.isEmpty()) {
                                chapter.imageUrls = localPaths;
                                localChapters.add(chapter);
                            }
                        }
                    }
                }

                if (!localChapters.isEmpty()) {
                    // Mettre à jour l'UI avec les chapitres locaux
                    runOnUiThread(() -> {
                        chapters.clear();
                        chapters.addAll(localChapters);
                        setupChapterSpinner();
                        errorTextView.setVisibility(View.GONE);
                        Toast.makeText(this, "Chapitres chargés depuis le stockage local", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        errorTextView.setText("Aucun chapitre téléchargé disponible");
                    });
                }
            }
        });
    }