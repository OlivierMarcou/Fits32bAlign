# 📦 FITS Stacker v1.1 - Package Complet

## 🎯 Aperçu Rapide

Ce package contient **12 fichiers** pour transformer votre FITS Stacker :

- **4 fichiers Java** (code source à intégrer)
- **7 fichiers Markdown** (documentation complète)  
- **1 fichier TestImageGeneratorRGB.java** (générateur de test)

---

## 📂 Liste Complète des Fichiers

### 🔧 Fichiers de Code Source (4 fichiers)

| Fichier | Taille | Action | Description |
|---------|--------|--------|-------------|
| **FitsImage.java** | 23 KB | Remplacer | Support RGB + Canvas élargi + Interpolation |
| **ImageAligner.java** | 17 KB | Remplacer | Canvas maximisé + CanvasInfo |
| **StackingEngine.java** | 14 KB | Remplacer | Empilement RGB par canal |
| **FitsStackerApp.java** | 26 KB | Remplacer | Interface + Sauvegarde alignées |

### 🧪 Fichier de Test (1 fichier)

| Fichier | Taille | Action | Description |
|---------|--------|--------|-------------|
| **TestImageGeneratorRGB.java** | 8.9 KB | Ajouter | Générateur d'images FITS RGB de test |

### 📖 Documentation (7 fichiers)

| Fichier | Taille | Quand le lire |
|---------|--------|---------------|
| **INDEX.md** | 13 KB | 📘 **COMMENCEZ ICI** - Vue d'ensemble |
| **RECAPITULATIF.md** | 13 KB | 📗 Résumé complet des 3 fonctionnalités |
| **INSTALLATION.md** | 9.6 KB | 📕 Installation RGB + Canvas étape par étape |
| **INSTALL_SAUVEGARDE.md** | 8.2 KB | 📙 Installation uniquement sauvegarde |
| **GUIDE_INTEGRATION.md** | 9.9 KB | 📔 Guide technique détaillé |
| **README_AMELIORATIONS.md** | 10 KB | 📓 Comparaisons et performances |
| **DOC_SAUVEGARDE_ALIGNEES.md** | 13 KB | 📒 Guide complet sauvegarde |

**Total : 164 KB de code et documentation**

---

## 🚀 Par Où Commencer ?

### Option 1 : Installation Rapide (5 min)

1. Lisez **INDEX.md** (vue d'ensemble rapide)
2. Suivez **INSTALLATION.md** (sections 1-5)
3. Testez votre application

### Option 2 : Installation Guidée (15 min)

1. Lisez **RECAPITULATIF.md** (comprendre les changements)
2. Suivez **INSTALLATION.md** (toutes les sections)
3. Consultez **GUIDE_INTEGRATION.md** (FAQ et exemples)

### Option 3 : Installation Sélective

**Pour RGB + Canvas seulement** :
```bash
# Copier 3 fichiers
cp {FitsImage,ImageAligner,StackingEngine}.java /votre/projet/
mvn clean package
```

**Pour ajouter la sauvegarde ensuite** :
```bash
# Copier 1 fichier
cp FitsStackerApp.java /votre/projet/
mvn clean package
```

Documentation : **INSTALL_SAUVEGARDE.md**

---

## 🎯 Les 3 Améliorations

### ✨ 1. Support RGB Complet

**Fichiers concernés** :
- ✅ FitsImage.java
- ✅ StackingEngine.java
- ⚠️ ImageAligner.java (pour canvas)

**Documentation** : GUIDE_INTEGRATION.md

### 🖼️ 2. Canvas Maximisé

**Fichiers concernés** :
- ✅ FitsImage.java
- ✅ ImageAligner.java
- ✅ StackingEngine.java

**Documentation** : README_AMELIORATIONS.md (section Canvas)

### 💾 3. Sauvegarde Images Alignées

**Fichiers concernés** :
- ✅ FitsStackerApp.java

**Documentation** : DOC_SAUVEGARDE_ALIGNEES.md

---

## 📊 Matrice de Dépendances

```
FitsImage.java
   ├─ Requis pour: RGB + Canvas + Sauvegarde
   └─ Dépend de: Rien

ImageAligner.java
   ├─ Requis pour: Canvas + Sauvegarde
   ├─ Dépend de: FitsImage.java
   └─ Utilise: AffineTransform, CanvasInfo

StackingEngine.java
   ├─ Requis pour: RGB + Canvas
   ├─ Dépend de: FitsImage.java, ImageAligner.java
   └─ Utilise: CanvasInfo

FitsStackerApp.java
   ├─ Requis pour: Sauvegarde
   ├─ Dépend de: FitsImage, ImageAligner, StackingEngine
   └─ Utilise: Toutes les classes ci-dessus
```

**Conclusion** : Pour avoir toutes les fonctionnalités, copier les 4 fichiers Java.

---

## 🔍 Guide de Lecture selon Votre Besoin

### Je veux installer rapidement
→ **INDEX.md** puis **INSTALLATION.md** (sections 1-5)

### Je veux tout comprendre
→ **RECAPITULATIF.md** puis **GUIDE_INTEGRATION.md**

### J'ai une erreur de compilation
→ **INSTALLATION.md** (section Dépannage)

### Je veux seulement la sauvegarde
→ **INSTALL_SAUVEGARDE.md**

### Je veux comprendre les performances
→ **README_AMELIORATIONS.md** (section Performances)

### Je veux utiliser la sauvegarde
→ **DOC_SAUVEGARDE_ALIGNEES.md**

### Je veux des exemples concrets
→ **RECAPITULATIF.md** (section Exemples)

### Je veux générer des images RGB de test
→ **GUIDE_INTEGRATION.md** (section Tests)

---

## ✅ Checklist d'Installation Complète

### Préparation
- [ ] Java 21 installé
- [ ] Maven installé
- [ ] Projet FITS Stacker original fonctionnel
- [ ] Sauvegarde du projet effectuée

### Copie des Fichiers
- [ ] FitsImage.java copié
- [ ] ImageAligner.java copié
- [ ] StackingEngine.java copié
- [ ] FitsStackerApp.java copié
- [ ] TestImageGeneratorRGB.java copié (optionnel)

### Compilation et Tests
- [ ] `mvn clean package` réussi
- [ ] Application démarre
- [ ] Images mono fonctionnent
- [ ] Images RGB détectées (si applicable)
- [ ] Canvas élargi calculé
- [ ] Bouton "Sauvegarder Alignées" visible
- [ ] Sauvegarde fonctionne

### Validation
- [ ] Test avec images réelles effectué
- [ ] alignment_params.txt généré
- [ ] Fichiers aligned_*.fits créés
- [ ] Paramètres corrects dans les noms
- [ ] Documentation consultée

---

## 🎓 Scénarios d'Utilisation

### Scénario 1 : Astrophoto Amateur (DSLR)
```
Besoin : Images couleur de mon Canon
Fichiers : FitsImage + ImageAligner + StackingEngine
Doc : INSTALLATION.md
Temps : 10 minutes
```

### Scénario 2 : CCD Monochrome
```
Besoin : Éviter perte aux bords
Fichiers : FitsImage + ImageAligner + StackingEngine
Doc : README_AMELIORATIONS.md (Canvas)
Temps : 10 minutes
```

### Scénario 3 : Analyse Scientifique
```
Besoin : Inspecter chaque image + paramètres
Fichiers : Les 4 fichiers Java
Doc : DOC_SAUVEGARDE_ALIGNEES.md
Temps : 15 minutes
```

### Scénario 4 : Débutant Prudent
```
Besoin : Tester progressivement
Étape 1 : Canvas seulement (3 fichiers)
Étape 2 : Ajouter sauvegarde (1 fichier)
Doc : INSTALLATION.md puis INSTALL_SAUVEGARDE.md
Temps : 20 minutes
```

---

## 🐛 Résolution de Problèmes Rapide

### Erreur de compilation
→ Vérifier que les 4 fichiers sont bien copiés  
→ Consulter **INSTALLATION.md** section Dépannage

### OutOfMemoryError
→ Augmenter RAM : `java -Xmx8G -jar ...`  
→ Consulter **README_AMELIORATIONS.md** section Performances

### Images RGB non détectées
→ Vérifier format avec DS9  
→ Consulter **GUIDE_INTEGRATION.md** section FAQ

### Bouton "Sauvegarder Alignées" grisé
→ Vérifier que l'alignement a réussi  
→ Consulter **INSTALL_SAUVEGARDE.md** section Dépannage

### Canvas trop grand
→ Normal avec grandes rotations (>30°)  
→ Augmenter RAM si nécessaire

---

## 📞 Support et Ressources

### Documentation Interne

- **Questions générales** : INDEX.md
- **Installation** : INSTALLATION.md ou INSTALL_SAUVEGARDE.md
- **Technique** : GUIDE_INTEGRATION.md
- **Performances** : README_AMELIORATIONS.md
- **Sauvegarde** : DOC_SAUVEGARDE_ALIGNEES.md
- **Vue d'ensemble** : RECAPITULATIF.md

### Outils Externes

- **SAOImage DS9** : Visualisation FITS
- **Siril** : Alternative d'empilement
- **PixInsight** : Post-traitement professionnel
- **GIMP + FITS Plugin** : Édition d'image

### Spécifications

- **Format FITS** : https://fits.gsfc.nasa.gov/
- **Java 21** : https://openjdk.org/projects/jdk/21/
- **Maven** : https://maven.apache.org/

---

## 📝 Notes Importantes

### Rétrocompatibilité

✅ **100% compatible** avec images et projets existants
- Les images mono fonctionnent comme avant
- Aucune régression de fonctionnalité
- Nouvelles fonctionnalités optionnelles

### Performances

⚠️ **Images RGB** : 3x plus lent et 3x plus de RAM que mono
💡 **Solution** : Allouer au moins 4-8 GB de RAM

### Sauvegarde

💾 **Espace disque** : Prévoir 1.5x la taille des images originales
📁 **Organisation** : Dossier dédié recommandé pour les images alignées

---

## 🎉 Prêt à Commencer !

### Installation Express (5 min)

```bash
# 1. Copier les 4 fichiers
cd fits-stacker/src/main/java/com/astro/
cp /path/to/outputs/{FitsImage,ImageAligner,StackingEngine,FitsStackerApp}.java .

# 2. Recompiler
cd ../../../../..
mvn clean package

# 3. Lancer
java -Xmx4G -jar target/fits-stacker-1.0-SNAPSHOT.jar
```

### Premiers Tests

```bash
# Générer images RGB de test
java -cp target/*.jar com.astro.TestImageGeneratorRGB test_rgb 5

# Dans l'application :
# - Charger test_rgb/*.fits
# - Aligner
# - Sauvegarder alignées
# - Empiler
```

---

## 📦 Contenu du Package - Récapitulatif

```
/mnt/user-data/outputs/
│
├── 🔧 Code Source (4 fichiers - 80 KB)
│   ├── FitsImage.java (23 KB)
│   ├── ImageAligner.java (17 KB)
│   ├── StackingEngine.java (14 KB)
│   └── FitsStackerApp.java (26 KB)
│
├── 🧪 Test (1 fichier - 8.9 KB)
│   └── TestImageGeneratorRGB.java
│
└── 📖 Documentation (7 fichiers - 75 KB)
    ├── INDEX.md (13 KB) ⭐ COMMENCEZ ICI
    ├── RECAPITULATIF.md (13 KB)
    ├── INSTALLATION.md (9.6 KB)
    ├── INSTALL_SAUVEGARDE.md (8.2 KB)
    ├── GUIDE_INTEGRATION.md (9.9 KB)
    ├── README_AMELIORATIONS.md (10 KB)
    └── DOC_SAUVEGARDE_ALIGNEES.md (13 KB)

Total : 12 fichiers - 164 KB
```

---

**FITS Stacker v1.1 - Package Complet**  
3 Améliorations Majeures  
12 Fichiers Prêts à l'Emploi  
Documentation Exhaustive  

🌟 **Bon empilement !** 🌟
