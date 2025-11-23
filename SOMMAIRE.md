# 🌟 FITS Stacker - Application d'Astrophotographie

## 📦 Contenu du Projet

Vous avez reçu une application Java 21 complète pour l'alignement et l'empilement d'images astronomiques au format FITS.

### 📂 Structure du Projet

```
fits-stacker/
├── pom.xml                      # Configuration Maven
├── compile.sh                   # Script de compilation alternatif
├── README.md                    # Documentation principale
├── GUIDE_UTILISATION.md        # Guide utilisateur détaillé
├── TESTS.md                     # Documentation des tests
└── src/main/java/com/astro/
    ├── FitsStackerApp.java      # Application principale (interface)
    ├── FitsImage.java           # Gestion des images FITS
    ├── Star.java                # Représentation d'une étoile
    ├── StarDetector.java        # Détection automatique d'étoiles
    ├── ImageAligner.java        # Alignement des images
    ├── StackingEngine.java      # Moteur d'empilement
    ├── Config.java              # Configuration
    └── TestImageGenerator.java  # Générateur d'images de test
```

## 🚀 Démarrage Rapide

### Option 1 : Compilation avec Maven (recommandé)

```bash
cd fits-stacker
mvn clean package
java -jar target/fits-stacker-1.0-SNAPSHOT.jar
```

### Option 2 : Compilation manuelle

```bash
cd fits-stacker
chmod +x compile.sh
./compile.sh
cd build
java -jar fits-stacker.jar
```

## ✨ Fonctionnalités Principales

### 🎯 Interface Moderne
- **FlatLaf Dark Theme** : Interface élégante et professionnelle
- **Sélection flexible** : Fichiers individuels ou dossiers complets
- **Progression en temps réel** : Barre de progression et journal détaillé

### 🌠 Détection d'Étoiles
- Détection automatique par seuillage statistique
- Calcul de centroïdes pour précision sub-pixel
- Tri par intensité (flux)

### 🎯 Alignement Intelligent
- Basé sur la correspondance de triangles d'étoiles
- Robuste aux rotations et translations
- Calcul automatique des décalages

### 📊 Méthodes d'Empilement

| Méthode | Description | Usage |
|---------|-------------|-------|
| **Moyenne** | Moyenne arithmétique simple | Images propres sans artefacts |
| **Médiane** | Valeur médiane | Présence de satellites, avions |
| **Sigma Clipping** | Élimine valeurs > 2σ | **RECOMMANDÉ** pour astrophoto |
| **Maximum** | Valeur maximale | Capture de météores |
| **Minimum** | Valeur minimale | Élimination de traces |
| **Moyenne Pondérée** | Privilégie les meilleures images | Qualité variable |

## 📚 Documentation Complète

### README.md
- Architecture détaillée du code
- Explication des algorithmes
- Installation et configuration
- Comparaison avec Siril

### GUIDE_UTILISATION.md
- Workflow typique pas à pas
- Conseils pratiques
- Résolution de problèmes
- Exemples de commandes

### TESTS.md
- Génération d'images de test
- Procédures de validation
- Cas d'usage réels
- Scripts d'analyse de qualité

## 🛠️ Technologies Utilisées

- **Java 21** : Dernière version LTS
- **Swing + FlatLaf** : Interface moderne
- **nom-tam-fits** : Lecture/écriture FITS
- **Maven** : Gestion de dépendances

## 🎨 Captures d'Écran (ASCII)

```
╔════════════════════════════════════════════════════╗
║  FITS Stacker                                      ║
║  Alignement et empilement d'images astronomiques  ║
╠════════════════════════════════════════════════════╣
║                                                    ║
║  Images FITS              Méthode d'empilement    ║
║  ┌──────────────────┐    ┌──────────────────────┐ ║
║  │ 1. image_001.fits│    │ Sigma Clipping    ▼ │ ║
║  │ 2. image_002.fits│    └──────────────────────┘ ║
║  │ 3. image_003.fits│                             ║
║  │ ...              │    ┌──────────────────────┐ ║
║  └──────────────────┘    │  ALIGNER IMAGES      │ ║
║                          └──────────────────────┘ ║
║  [Ajouter] [Dossier]     ┌──────────────────────┐ ║
║  [Retirer] [Effacer]     │  EMPILER IMAGES      │ ║
║                          └──────────────────────┘ ║
║                                                    ║
║  Status: Prêt                                     ║
║  ▓▓▓▓▓▓▓▓▓░░░░░░░░░░  45%                        ║
╚════════════════════════════════════════════════════╝
```

## 🔬 Exemple d'Utilisation

```java
// Générer des images de test
java -cp target/fits-stacker-1.0-SNAPSHOT.jar \
  com.astro.TestImageGenerator test_images 10

// Lancer l'application
java -jar target/fits-stacker-1.0-SNAPSHOT.jar

// Avec plus de mémoire pour grandes images
java -Xmx8G -jar target/fits-stacker-1.0-SNAPSHOT.jar
```

## 📈 Workflow Typique

1. **Acquisition** : Prendre 10-50 images de votre sujet
2. **Chargement** : Ajouter les images dans FITS Stacker
3. **Alignement** : Laisser l'algorithme détecter et aligner
4. **Empilement** : Choisir Sigma Clipping
5. **Résultat** : Image finale avec meilleur rapport S/N

## 🎓 Concepts Clés

### Pourquoi empiler ?
L'empilement d'images astronomiques permet de :
- **Réduire le bruit** : √N fois moins de bruit avec N images
- **Augmenter le signal** : Meilleure détection des objets faibles
- **Éliminer les artefacts** : Satellites, avions, pixels chauds

### Sigma Clipping
Méthode statistique qui :
1. Calcule la moyenne et écart-type pour chaque pixel
2. Élimine les valeurs à plus de 2σ de la moyenne
3. Recalcule la moyenne sur les valeurs conservées
4. Résultat : meilleur rapport signal/bruit

## 🌟 Améliorations Futures

- [ ] Prévisualisation avec zoom
- [ ] Histogramme et statistiques
- [ ] Support du debayering couleur
- [ ] Calibration (dark, flat, bias)
- [ ] Export TIFF 32-bit
- [ ] Multi-threading
- [ ] Mode batch en ligne de commande

## 🤝 Contribution

Ce projet est fourni tel quel pour usage personnel et éducatif.
Suggestions et améliorations bienvenues !

## 📝 Licence

Projet éducatif open-source.

## 🙏 Remerciements

Inspiré par des logiciels comme :
- Siril (Free Astronomical Image Processing Tool)
- DeepSkyStacker
- PixInsight

## 📞 Support

Pour toute question ou problème :
- Consultez d'abord README.md et GUIDE_UTILISATION.md
- Vérifiez TESTS.md pour les procédures de validation
- Les logs dans l'application vous guideront

---

**Bon empilement ! 🌌✨**

*"The universe is not only queerer than we suppose, but quequer than we can suppose." - J.B.S. Haldane*
