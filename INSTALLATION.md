# Instructions de Déploiement - FITS Stacker RGB & Canvas

## 📦 Fichiers Fournis

Vous avez reçu 5 fichiers dans `/mnt/user-data/outputs/` :

### Fichiers de Code Source (à copier)

1. **FitsImage.java** (✨ MODIFIÉ)
   - Support RGB complet
   - Canvas élargi
   - Méthodes pour les 3 canaux

2. **ImageAligner.java** (✨ MODIFIÉ)
   - Classe CanvasInfo
   - Calcul du canvas élargi
   - Stockage des infos dans les images

3. **StackingEngine.java** (✨ MODIFIÉ)
   - Détection RGB/Mono
   - Empilement par canal
   - Support canvas élargi

### Fichiers de Test (optionnel)

4. **TestImageGeneratorRGB.java** (🆕 NOUVEAU)
   - Génère des images FITS RGB de test
   - Avec étoiles colorées (températures variées)
   - Utile pour valider le support RGB

### Documentation

5. **GUIDE_INTEGRATION.md** (📖 DOCUMENTATION)
   - Instructions détaillées d'installation
   - Exemples d'utilisation
   - FAQ et dépannage

6. **README_AMELIORATIONS.md** (📖 RÉSUMÉ)
   - Vue d'ensemble des changements
   - Comparaison avant/après
   - Tests recommandés

---

## 🚀 Installation en 5 Étapes

### Étape 1 : Préparation

```bash
# Naviguer vers votre projet
cd /chemin/vers/fits-stacker

# Créer une sauvegarde
cp -r . ../fits-stacker-backup

# Vérifier la structure
ls -la src/main/java/com/astro/
```

### Étape 2 : Copier les Fichiers de Code

```bash
# Copier les 3 fichiers modifiés
cp /mnt/user-data/outputs/FitsImage.java src/main/java/com/astro/
cp /mnt/user-data/outputs/ImageAligner.java src/main/java/com/astro/
cp /mnt/user-data/outputs/StackingEngine.java src/main/java/com/astro/

# Vérifier
ls -lh src/main/java/com/astro/{FitsImage,ImageAligner,StackingEngine}.java
```

### Étape 3 : Copier le Générateur de Test (Optionnel)

```bash
# Copier le générateur RGB
cp /mnt/user-data/outputs/TestImageGeneratorRGB.java src/main/java/com/astro/

# Vérifier
ls -lh src/main/java/com/astro/TestImageGeneratorRGB.java
```

### Étape 4 : Copier la Documentation

```bash
# Créer un dossier docs si nécessaire
mkdir -p docs

# Copier les guides
cp /mnt/user-data/outputs/GUIDE_INTEGRATION.md docs/
cp /mnt/user-data/outputs/README_AMELIORATIONS.md docs/

# Vérifier
ls -lh docs/*.md
```

### Étape 5 : Compiler et Tester

```bash
# Nettoyer et recompiler
mvn clean package

# Vérifier la compilation
ls -lh target/fits-stacker-1.0-SNAPSHOT.jar

# Lancer l'application
java -Xmx4G -jar target/fits-stacker-1.0-SNAPSHOT.jar
```

---

## 📁 Structure Finale du Projet

```
fits-stacker/
│
├── src/main/java/com/astro/
│   ├── FitsImage.java              ✨ MODIFIÉ
│   ├── ImageAligner.java           ✨ MODIFIÉ
│   ├── StackingEngine.java         ✨ MODIFIÉ
│   ├── TestImageGeneratorRGB.java  🆕 NOUVEAU (optionnel)
│   │
│   ├── Config.java                 ✅ Inchangé
│   ├── Star.java                   ✅ Inchangé
│   ├── StarDetector.java           ✅ Inchangé
│   ├── FitsStackerApp.java         ✅ Inchangé
│   └── TestImageGenerator.java     ✅ Inchangé
│
├── docs/
│   ├── GUIDE_INTEGRATION.md        📖 Guide complet
│   └── README_AMELIORATIONS.md     📖 Résumé des changements
│
├── pom.xml                         ✅ Inchangé
├── README.md                       ✅ Votre README original
│
└── target/
    └── fits-stacker-1.0-SNAPSHOT.jar
```

---

## ✅ Validation Post-Installation

### Test 1 : Compilation
```bash
mvn clean package
# ✓ Devrait se terminer avec "BUILD SUCCESS"
```

### Test 2 : Lancement
```bash
java -jar target/fits-stacker-1.0-SNAPSHOT.jar
# ✓ Interface devrait s'ouvrir normalement
```

### Test 3 : Images Mono (Compatibilité)
```bash
# Générer des images de test mono
java -cp target/fits-stacker-1.0-SNAPSHOT.jar \
  com.astro.TestImageGenerator test_mono 5

# Dans l'application:
# 1. Ajouter Dossier → test_mono/
# 2. Aligner Images → Devrait afficher "Canvas élargi: XXxXX"
# 3. Empiler Images → Devrait fonctionner
```

### Test 4 : Images RGB (Nouvelle Fonctionnalité)
```bash
# Générer des images RGB de test
java -cp target/fits-stacker-1.0-SNAPSHOT.jar \
  com.astro.TestImageGeneratorRGB test_rgb 5

# Dans l'application:
# 1. Ajouter Dossier → test_rgb/
# 2. Observer dans le log: "Image RGB détectée: 1024x1024 x 3 canaux"
# 3. Aligner Images → Devrait fonctionner
# 4. Empiler Images → Devrait montrer progression pour R, G, B
```

---

## 🔍 Vérifications Spécifiques

### Vérifier le Support RGB

Cherchez ces messages dans le log de l'application :

```
✓ "Type FITS détecté: [[[F"               → Cube 3D détecté
✓ "Format: float[][][] RGB"               → RGB reconnu
✓ "Image chargée: 1024x1024 pixels (RGB)" → Confirmation
✓ "Empilement canal Rouge..."            → Traitement R
✓ "Empilement canal Vert..."             → Traitement G
✓ "Empilement canal Bleu..."             → Traitement B
```

### Vérifier le Canvas Élargi

Cherchez ces messages :

```
✓ "Canvas calculé: 2150x2150 (offset: 50, 50)"
✓ "Canvas élargi: 2150x2150 pixels (offset: 50, 50)"
✓ "Expansion: 4.9%"
```

---

## 🐛 Résolution de Problèmes

### Problème 1 : Erreur de Compilation

**Symptôme** :
```
[ERROR] /src/.../FitsImage.java:[XX,XX] cannot find symbol
```

**Solution** :
```bash
# Vérifier que tous les fichiers sont bien copiés
ls src/main/java/com/astro/{FitsImage,ImageAligner,StackingEngine}.java

# Nettoyer et recompiler
mvn clean
mvn compile
mvn package
```

### Problème 2 : OutOfMemoryError

**Symptôme** :
```
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
```

**Solution** :
```bash
# Augmenter la mémoire disponible
java -Xmx8G -jar target/fits-stacker-1.0-SNAPSHOT.jar

# Ou pour très grandes images
java -Xmx16G -jar target/fits-stacker-1.0-SNAPSHOT.jar
```

### Problème 3 : Images RGB Non Reconnues

**Symptôme** :
```
Format FITS non supporté: [[[S
```

**Solution** :
- Vérifier que le fichier FITS a bien 3 canaux
- Essayer de régénérer avec TestImageGeneratorRGB
- Vérifier avec DS9 : `ds9 image.fits -rgb`

### Problème 4 : Canvas Trop Grand

**Symptôme** :
```
Canvas élargi: 15000x15000 pixels
OutOfMemoryError
```

**Cause** : Rotations très importantes (>45°)

**Solution** :
```bash
# Augmenter RAM massivement
java -Xmx24G -XX:+UseG1GC -jar ...

# Ou réduire les rotations d'entrée
```

---

## 📊 Métriques de Validation

Après installation complète, vos résultats devraient être :

| Métrique | Attendu | Comment Vérifier |
|----------|---------|-----------------|
| Compilation | SUCCESS | `mvn package` |
| Lancement | OK | Interface visible |
| Images Mono | Fonctionnent | Test avec images existantes |
| Images RGB | Détectées | Log affiche "RGB détectée" |
| Canvas élargi | Calculé | Log affiche "Canvas élargi" |
| Taille sortie | ≥ entrée | Comparer dimensions |
| Empilement RGB | 3 canaux | Progression R, G, B |
| Fichier FITS | Valide | Ouvrable dans DS9/Siril |

---

## 🎓 Pour Aller Plus Loin

### Créer vos Propres Images RGB

**Option 1 : Débayeriser avec Siril**
```bash
# Dans Siril
1. Ouvrir vos fichiers RAW
2. Débayeriser (Bayer Pattern RGGB)
3. Exporter en FITS RGB
4. Utiliser dans FITS Stacker
```

**Option 2 : Combiner des Canaux**
```python
# Script Python avec astropy
from astropy.io import fits
import numpy as np

# Charger 3 images mono (R, G, B)
r = fits.getdata('red.fits')
g = fits.getdata('green.fits')
b = fits.getdata('blue.fits')

# Créer cube RGB
rgb = np.array([r, g, b])

# Sauvegarder
fits.writeto('combined_rgb.fits', rgb)
```

### Optimiser les Performances

**Config.java** - Ajustez ces valeurs :
```java
public static final int DEFAULT_MAX_STARS = 150;  // Plus d'étoiles = meilleur alignement
public static final int RANSAC_ITERATIONS = 1000; // Plus d'itérations = meilleur alignement
public static final int PROGRESS_UPDATE_INTERVAL = 50000; // Updates moins fréquents
```

---

## 📞 Support

### En cas de Problème

1. **Consultez d'abord** :
   - [GUIDE_INTEGRATION.md](GUIDE_INTEGRATION.md) - Section FAQ
   - [README_AMELIORATIONS.md](README_AMELIORATIONS.md) - Section Dépannage

2. **Vérifiez** :
   - Version Java : `java -version` (doit être ≥ 21)
   - Mémoire disponible : `java -XshowSettings:vm -version`
   - Fichiers copiés : tous les 3 fichiers modifiés

3. **Collectez les Informations** :
   - Message d'erreur complet
   - Version du système d'exploitation
   - Taille et type des images FITS
   - Contenu du log de l'application

---

## 📝 Checklist Finale

Avant de considérer l'installation terminée :

- [ ] Tous les fichiers copiés au bon endroit
- [ ] Compilation Maven réussie
- [ ] Application démarre sans erreur
- [ ] Images mono fonctionnent (rétro-compatibilité)
- [ ] Images RGB sont détectées et traitées
- [ ] Canvas élargi est calculé et affiché
- [ ] Empilement produit un résultat valide
- [ ] Fichier de sortie peut être ouvert dans DS9/Siril
- [ ] Documentation accessible dans docs/
- [ ] Tests avec générateurs fonctionnent

---

## 🎉 Félicitations !

Si tous les tests passent, votre FITS Stacker est maintenant amélioré avec :
- ✅ Support complet RGB
- ✅ Canvas maximisé (pas de rognage)
- ✅ Rétro-compatibilité mono
- ✅ Performances optimisées

**Prochaines Étapes** :
1. Testez avec vos vraies images astronomiques
2. Ajustez la RAM si nécessaire
3. Explorez les différentes méthodes d'empilement
4. Comparez les résultats avec Siril/PixInsight

---

**Installation préparée pour FITS Stacker v1.1**  
Date : 2024  
Support : Consultez GUIDE_INTEGRATION.md pour aide détaillée
