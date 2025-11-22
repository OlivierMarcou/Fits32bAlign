# FITS Stacker - Alignement et Empilement d'Images Astronomiques

Application Java 21 avec interface Swing moderne pour aligner et empiler des images astronomiques au format FITS.

## Fonctionnalités

### 🎯 Principales caractéristiques

- **Interface moderne** avec FlatLaf Dark theme
- **Sélection flexible** : ajout manuel de fichiers ou dossiers complets
- **Détection automatique d'étoiles** pour l'alignement
- **Alignement précis** basé sur la correspondance de triangles d'étoiles
- **Méthodes d'empilement multiples** :
  - Moyenne (Average)
  - Médiane (Median)
  - Sigma Clipping (élimine les valeurs aberrantes)
  - Maximum
  - Minimum
  - Moyenne Pondérée (Weighted Average)
- **Barre de progression** et journal en temps réel
- **Support multi-formats FITS** (float, short, int, double)

## Prérequis

- Java 21 ou supérieur
- Maven 3.6+ (pour la compilation)
- Bibliothèques :
  - FlatLaf 3.2.5 (interface moderne)
  - nom-tam-fits 1.20.1 (lecture/écriture FITS)

## Installation

### 1. Compilation avec Maven

```bash
mvn clean package
```

Cela créera un JAR exécutable dans `target/fits-stacker-1.0-SNAPSHOT.jar`

### 2. Exécution

```bash
java -jar target/fits-stacker-1.0-SNAPSHOT.jar
```

Ou simplement double-cliquez sur le fichier JAR.

## Utilisation

### Étape 1 : Charger les images

**Option A : Ajouter des fichiers individuels**
1. Cliquez sur "Ajouter Fichiers"
2. Sélectionnez vos images FITS (`.fits`, `.fit`, `.fts`)
3. Vous pouvez en sélectionner plusieurs à la fois

**Option B : Ajouter un dossier complet**
1. Cliquez sur "Ajouter Dossier"
2. Sélectionnez le dossier contenant vos images FITS
3. Tous les fichiers FITS du dossier seront ajoutés

### Étape 2 : Aligner les images

1. Une fois vos images chargées, cliquez sur "Aligner Images"
2. Le programme va :
   - Charger chaque image
   - Détecter automatiquement les étoiles
   - Calculer les offsets nécessaires pour aligner toutes les images
3. Suivez la progression dans la barre et le journal

### Étape 3 : Empiler les images

1. Choisissez votre méthode d'empilement dans le menu déroulant :
   
   **Moyenne** : Moyenne simple, rapide mais sensible aux valeurs aberrantes
   
   **Médiane** : Plus robuste, élimine mieux le bruit mais plus lent
   
   **Sigma Clipping** : Élimine les valeurs à plus de 2σ de la moyenne, excellent pour rejeter les pixels chauds/froids
   
   **Maximum** : Conserve la valeur maximale, utile pour les météores
   
   **Minimum** : Conserve la valeur minimale, utile pour éliminer les traces de satellites
   
   **Moyenne Pondérée** : Donne plus de poids aux premières images

2. Cliquez sur "Empiler Images"
3. Choisissez l'emplacement de sortie pour votre image finale
4. Attendez la fin du traitement

### Conseils d'utilisation

- **Images de référence** : La première image de la liste est utilisée comme référence pour l'alignement
- **Ordre des images** : Mettez votre meilleure image en premier pour de meilleurs résultats
- **Nombre d'images** : Plus vous empilez d'images, meilleur sera le rapport signal/bruit
- **Méthode recommandée** : Sigma Clipping pour la plupart des cas d'astrophotographie

## Architecture du code

### Classes principales

**FitsImage.java**
- Gestion des images FITS
- Conversion automatique des différents formats (float, short, int, double)
- Calcul des offsets d'alignement
- Création de copies alignées

**Star.java**
- Représentation d'une étoile détectée
- Calcul de distance entre étoiles

**StarDetector.java**
- Détection automatique des étoiles
- Calcul du seuil basé sur la moyenne et l'écart-type
- Détection de maxima locaux
- Calcul de centroïdes pour positionnement sub-pixel

**ImageAligner.java**
- Alignement basé sur la correspondance de triangles d'étoiles
- Robuste aux rotations et translations
- Calcul des offsets optimaux

**StackingEngine.java**
- Implémentation de toutes les méthodes d'empilement
- Traitement pixel par pixel
- Support de la progression

**FitsStackerApp.java**
- Interface utilisateur Swing avec FlatLaf
- Gestion des fichiers et dossiers
- Coordination des opérations
- Retour visuel avec barre de progression et journal

## Algorithmes

### Détection d'étoiles

1. Calcul de la moyenne et écart-type de l'image
2. Seuillage à moyenne + 3σ
3. Détection de maxima locaux
4. Calcul du centroïde pondéré pour chaque étoile
5. Tri par flux (intensité totale)

### Alignement

1. Création de triangles à partir des étoiles les plus brillantes
2. Calcul des rapports de côtés pour chaque triangle
3. Correspondance des triangles similaires entre images
4. Calcul du décalage (offset) optimal
5. Scoring basé sur le nombre d'étoiles qui correspondent

### Empilement

Chaque méthode traite l'image pixel par pixel :
- **Moyenne** : Σ(pixels) / n
- **Médiane** : Valeur médiane des pixels
- **Sigma Clipping** : Moyenne après élimination des valeurs > 2σ
- **Maximum/Minimum** : Valeur max/min de chaque pile de pixels
- **Moyenne Pondérée** : Σ(pixels × poids) / Σ(poids)

## Améliorations possibles

- [ ] Prévisualisation des images avec zoom
- [ ] Histogramme et statistiques d'image
- [ ] Support du debayering pour les images couleur
- [ ] Calibration avec darks, flats et bias
- [ ] Alignement par corrélation croisée en plus des étoiles
- [ ] Support multi-threading pour traitement parallèle
- [ ] Export en TIFF 32-bit en plus de FITS
- [ ] Sauvegarde/chargement de sessions

## Comparaison avec Siril

Cette application implémente les méthodes d'empilement principales de Siril :
- ✅ Average (Moyenne)
- ✅ Median (Médiane) 
- ✅ Sigma Clipping
- ✅ Maximum/Minimum
- ⚠️ Siril propose des variantes supplémentaires (Winsorized Sigma Clipping, etc.)

## Licence

Ce projet est fourni tel quel pour un usage personnel et éducatif.

## Auteur

Créé pour le traitement d'images astronomiques FITS avec Java 21 moderne.

---

**Note** : Pour de meilleures performances, allouez suffisamment de mémoire à la JVM :
```bash
java -Xmx4G -jar fits-stacker-1.0-SNAPSHOT.jar
```
