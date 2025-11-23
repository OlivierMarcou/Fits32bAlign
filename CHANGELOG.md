# Changelog - FITS Stacker

Toutes les modifications notables de ce projet seront documentées dans ce fichier.

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/).

---

## [1.1.0] - 2024-11-23

### ✨ Ajouté

#### Support des Images RGB (Couleur)
- Support complet des images FITS RGB à 3 canaux
- Détection automatique du format RGB vs Mono
- Empilement séparé des canaux R, G, B avec progression individuelle
- Formats supportés : `float[3][H][W]`, `short[3][H][W]`, `int[3][H][W]`, `double[3][H][W]`
- Détection des étoiles sur le canal vert (G) pour les images RGB
- Sauvegarde en format FITS RGB natif
- Messages de log détaillés pour le traitement RGB

**Fichiers concernés** :
- `FitsImage.java` : Ajout de `colorData[3][H][W]`, méthodes `getPixel(channel, x, y)`, `setPixel(channel, x, y, value)`, `isColor()`, `getColorData()`
- `StackingEngine.java` : Détection auto RGB/Mono, méthodes d'empilement par canal, progression séparée
- `TestImageGeneratorRGB.java` : Nouveau générateur d'images de test RGB avec étoiles colorées

#### Canvas Maximisé (Pas de Rognage)
- Calcul automatique d'un canvas élargi pour contenir toutes les images alignées
- Conservation de 100% des données même avec rotations importantes
- Nouvelle classe `ImageAligner.CanvasInfo` pour stocker les dimensions du canvas
- Méthode `calculateExpandedCanvas()` pour calculer le canvas optimal
- Support du canvas élargi dans `createAlignedCopy()` avec offsets
- Zones non couvertes remplies de pixels à 0 (fond noir)
- Messages de log détaillés sur les dimensions du canvas

**Fichiers concernés** :
- `ImageAligner.java` : Classe `CanvasInfo`, méthode `calculateExpandedCanvas()`, stockage dans les images
- `FitsImage.java` : Méthodes `setCanvasInfo()`, `getCanvasInfo()`, signature modifiée de `createAlignedCopy()`
- `StackingEngine.java` : Utilisation du `CanvasInfo` pour créer les images alignées

#### Sauvegarde des Images Alignées
- Nouveau bouton "Sauvegarder Alignées" dans l'interface
- Sauvegarde de chaque image alignée individuellement
- Noms de fichiers incluant les paramètres : `aligned_XXX_[nom]_dxX_dyY_rotR_scaleS.fits`
- Génération automatique d'un fichier `alignment_params.txt` avec tous les détails
- Paramètres inclus : décalage (dx, dy), rotation (degrés), échelle (facteur), translation (tx, ty)
- Interface activée automatiquement après alignement réussi
- Progression de sauvegarde avec barre de progression

**Fichiers concernés** :
- `FitsStackerApp.java` : Nouveau bouton, méthode `saveAlignedImages()`, génération du fichier de métadonnées

### 🔧 Amélioré

#### Gestion des Formats FITS
- Support étendu pour les cubes 3D RGB
- Meilleure détection des formats de données
- Messages d'erreur plus explicites pour formats non supportés

#### Interpolation
- Interpolation bilinéaire séparée pour chaque canal RGB
- Méthode `interpolateChannel()` pour traitement RGB optimal
- Précision sub-pixel maintenue pour tous les canaux

#### Interface Utilisateur
- Ajout du bouton "Sauvegarder Alignées" (actif après alignement)
- Messages de statut plus détaillés
- Logs améliorés avec informations RGB
- Barre de progression par canal pour RGB

#### Performance
- Optimisation de la création des images alignées
- Gestion mémoire améliorée pour grands canvas
- Progression plus fine pour empilement RGB

### 📚 Documentation

- **START_HERE.md** : Guide ultra-rapide de démarrage
- **README.md** : Liste complète de tous les fichiers
- **INDEX.md** : Vue d'ensemble et guide de lecture
- **RECAPITULATIF.md** : Résumé complet des 3 fonctionnalités
- **INSTALLATION.md** : Guide d'installation pas à pas
- **INSTALL_SAUVEGARDE.md** : Installation uniquement de la sauvegarde
- **GUIDE_INTEGRATION.md** : Guide technique détaillé
- **README_AMELIORATIONS.md** : Comparaisons et performances
- **DOC_SAUVEGARDE_ALIGNEES.md** : Documentation complète de la sauvegarde
- **CHANGELOG.md** : Ce fichier

### 🐛 Corrigé

#### Perte de Données aux Bords
- **Avant** : Rognage des images lors de rotations importantes
- **Après** : Canvas élargi conserve 100% des données
- **Impact** : Meilleure qualité d'image, surtout aux coins

#### Support RGB Manquant
- **Avant** : Impossibilité de traiter les images FITS RGB
- **Après** : Support complet avec empilement par canal
- **Impact** : Utilisable avec DSLR, OSC, images débayerisées

#### Pas de Traçabilité d'Alignement
- **Avant** : Impossible de vérifier les paramètres d'alignement
- **Après** : Sauvegarde individuelle + fichier de métadonnées
- **Impact** : Débogage facilité, quality control, analyse statistique

---

## [1.0.0] - Version Originale

### Fonctionnalités de Base

- Chargement d'images FITS monochromes
- Détection automatique des étoiles
- Alignement basé sur correspondance de triangles (RANSAC)
- 6 méthodes d'empilement :
  - Moyenne (Average)
  - Médiane (Median)
  - Sigma Clipping
  - Maximum
  - Minimum
  - Moyenne Pondérée (Weighted Average)
- Interface Swing moderne avec FlatLaf Dark theme
- Barre de progression et journal en temps réel
- Support multi-formats FITS (float, short, int, double)

---

## Roadmap / Futures Améliorations Possibles

### v1.2 (Potentiel)
- [ ] Prévisualisation des images avec zoom
- [ ] Histogramme et statistiques d'image
- [ ] Support du debayering intégré
- [ ] Calibration avec darks, flats et bias
- [ ] Alignement par corrélation croisée (alternative)
- [ ] Support multi-threading pour traitement parallèle
- [ ] Export en TIFF 32-bit en plus de FITS
- [ ] Sauvegarde/chargement de sessions
- [ ] Drizzle algorithm (suréchantillonnage)
- [ ] Rejection de pixels cosmiques

### v1.3 (Potentiel)
- [ ] Interface de réglage des paramètres d'alignement
- [ ] Graphs de qualité d'alignement
- [ ] Détection automatique d'images aberrantes
- [ ] Batch processing (traitement par lots)
- [ ] Plugin system pour méthodes d'empilement custom
- [ ] Support de formats additionnels (XISF, TIFF)

---

## Comparaison avec Autres Logiciels

### vs Siril
- ✅ FITS Stacker : Plus simple, interface unique
- ✅ FITS Stacker : Canvas maximisé automatique
- ✅ Siril : Plus de fonctionnalités (calibration, etc.)
- ✅ Siril : Plus mature et testé

### vs DeepSkyStacker
- ✅ FITS Stacker : Support FITS natif
- ✅ FITS Stacker : Canvas maximisé
- ✅ DSS : Support RAW direct
- ✅ DSS : Plus d'options de traitement

### vs PixInsight
- ✅ FITS Stacker : Gratuit et open
- ✅ FITS Stacker : Plus simple à utiliser
- ✅ PixInsight : Professionnel, très complet
- ✅ PixInsight : Scripting avancé

---

## Notes Techniques

### Compatibilité
- **Java** : 21 ou supérieur requis
- **OS** : Windows, macOS, Linux
- **RAM** : 
  - Minimum : 2 GB
  - Recommandé : 4-8 GB
  - RGB 4K : 12-16 GB
- **Processeur** : Multi-core recommandé

### Dépendances
- FlatLaf 3.2.5 (interface)
- nom-tam-fits 1.20.1 (FITS I/O)
- Java 21 SDK

### Performance
- **Mono 2K** : ~25s pour 10 images
- **RGB 2K** : ~60s pour 10 images
- **Échelle** : Linéaire avec nombre d'images

---

## Contributeurs

Ce projet a été développé pour améliorer le traitement d'images astronomiques avec Java moderne.

### Améliorations v1.1
- Support RGB complet
- Canvas maximisé pour conservation des données
- Sauvegarde des images alignées avec métadonnées
- Documentation exhaustive (140+ KB)

---

## Licence

Ce projet est fourni tel quel pour un usage personnel et éducatif.

---

**FITS Stacker** - Alignement et Empilement d'Images Astronomiques  
Version actuelle : **1.1.0**  
Date : 23 Novembre 2024
