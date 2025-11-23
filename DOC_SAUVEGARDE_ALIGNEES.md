# Sauvegarde des Images Alignées - Documentation

## 🎯 Nouvelle Fonctionnalité

Cette fonctionnalité permet de sauvegarder **individuellement** chaque image après alignement, avec leurs **paramètres de transformation** inclus dans le nom du fichier et dans un fichier de métadonnées détaillé.

---

## 🚀 Utilisation

### Étape 1 : Aligner les Images

1. Charger vos images FITS
2. Cliquer sur **"Aligner Images"**
3. Attendre la fin de l'alignement

### Étape 2 : Sauvegarder les Images Alignées

1. Le bouton **"Sauvegarder Alignées"** devient actif
2. Cliquer sur ce bouton
3. Sélectionner un dossier de destination
4. Les images alignées sont sauvegardées avec leurs paramètres

---

## 📁 Format des Fichiers de Sortie

### Nom des Fichiers

Les fichiers sont nommés selon ce format :

```
aligned_XXX_[nom_original]_dxDX_dyDY_rotROT_scaleSCALE.fits
```

**Exemple** :
```
aligned_001_M33_001_dx-15.2_dy23.8_rot-2.35_scale1.0234.fits
aligned_002_M33_002_dx12.5_dy-8.3_rot1.89_scale0.9876.fits
aligned_003_M33_003_dx0.0_dy0.0_rot0.00_scale1.0000.fits  (référence)
```

**Légende** :
- `XXX` : Numéro de l'image (001, 002, ...)
- `[nom_original]` : Nom du fichier d'origine sans extension
- `dxDX` : Décalage en X en pixels (peut être négatif)
- `dyDY` : Décalage en Y en pixels (peut être négatif)
- `rotROT` : Rotation en degrés (peut être négatif)
- `scaleSCALE` : Facteur d'échelle (1.0 = pas de changement)

---

## 📄 Fichier de Métadonnées

Un fichier **`alignment_params.txt`** est créé avec tous les détails :

```
=== Paramètres d'Alignement FITS Stacker ===

Canvas élargi: 2150x2150 pixels
Offset global: dx=50, dy=50

Image de référence: M33_001.fits

Détails par image:
================

Image 1: M33_001.fits
  Décalage: dx=-15.23, dy=23.84 pixels
  Rotation: -2.345 degrés
  Échelle: 1.0234 (102.34%)
  Translation: tx=-15.23, ty=23.84
  Fichier sauvegardé: aligned_001_M33_001_dx-15.2_dy23.8_rot-2.35_scale1.0234.fits

Image 2: M33_002.fits
  Décalage: dx=12.56, dy=-8.32 pixels
  Rotation: 1.892 degrés
  Échelle: 0.9876 (98.76%)
  Translation: tx=12.56, ty=-8.32
  Fichier sauvegardé: aligned_002_M33_002_dx12.5_dy-8.3_rot1.89_scale0.9876.fits

...
```

---

## 🔍 Interprétation des Paramètres

### Décalage (dx, dy)

**Signification** : Translation nécessaire pour aligner l'image sur la référence

**Exemples** :
```
dx=15.0, dy=-10.0  →  L'image doit être déplacée de 15 pixels vers la droite
                       et 10 pixels vers le haut
                       
dx=0.0, dy=0.0     →  Image de référence (pas de décalage)
```

**Utilisation** :
- Vérifier la qualité de l'alignement
- Identifier les images avec trop de mouvement
- Détecter les problèmes de suivi

### Rotation (rot)

**Signification** : Rotation nécessaire pour aligner l'image

**Exemples** :
```
rot=5.23°   →  Rotation de 5.23° dans le sens anti-horaire
rot=-3.45°  →  Rotation de 3.45° dans le sens horaire
rot=0.0°    →  Pas de rotation (généralement l'image de référence)
```

**Utilisation** :
- Corriger la rotation de champ
- Identifier les problèmes de montage
- Vérifier l'alignement polaire

### Échelle (scale)

**Signification** : Facteur de zoom nécessaire pour aligner l'image

**Exemples** :
```
scale=1.0000  →  Pas de changement d'échelle (100%)
scale=1.0234  →  Image agrandie de 2.34% (102.34%)
scale=0.9876  →  Image réduite de 1.24% (98.76%)
```

**Utilisation** :
- Détecter les variations de focale (température, seeing)
- Corriger les différences d'échelle entre sessions
- Identifier les problèmes optiques

**Cas Typiques** :

| Valeur | Cause Probable |
|--------|----------------|
| 1.00 ± 0.01 | Normal - variations thermiques |
| 1.00 ± 0.05 | Acceptable - changements optiques mineurs |
| 1.00 ± 0.10 | Attention - vérifier la mise au point |
| > 1.10 | Problème - focales différentes |

---

## 💡 Cas d'Usage

### 1. Inspection Visuelle des Images Alignées

**Objectif** : Vérifier visuellement chaque image alignée

**Méthode** :
```bash
# Ouvrir toutes les images alignées dans DS9
ds9 -tile aligned_*.fits

# Ou une par une
ds9 aligned_001_M33_001_*.fits
ds9 aligned_002_M33_002_*.fits
...
```

**Que chercher** :
- Étoiles bien rondes (pas d'allongement)
- Alignement cohérent entre images
- Absence de décalages résiduels

### 2. Filtrage d'Images de Mauvaise Qualité

**Objectif** : Identifier et exclure les images mal alignées

**Critères** :
```
Rotation > 10°     → Problème majeur de suivi
Scale > 1.10       → Focale différente ou problème optique
dx ou dy > 100px   → Décrochage du suivi
```

**Action** :
1. Consulter `alignment_params.txt`
2. Identifier les images avec paramètres aberrants
3. Les exclure de l'empilement manuel

### 3. Empilement Sélectif

**Objectif** : N'empiler que les meilleures images

**Méthode** :
```bash
# 1. Sauvegarder les images alignées
# 2. Consulter alignment_params.txt
# 3. Sélectionner les meilleures (ex: rotation < 2°, scale ≈ 1.0)
# 4. Empiler manuellement avec Siril ou autre outil
```

### 4. Analyse Statistique de la Session

**Objectif** : Évaluer la qualité du suivi

**Méthode** :
```python
# Script Python pour analyser alignment_params.txt
import re

def analyze_alignment(filepath):
    with open(filepath) as f:
        content = f.read()
    
    # Extraire tous les décalages
    dx_values = re.findall(r'Décalage: dx=([-\d.]+)', content)
    dy_values = re.findall(r'dy=([-\d.]+)', content)
    rot_values = re.findall(r'Rotation: ([-\d.]+)', content)
    scale_values = re.findall(r'Échelle: ([\d.]+)', content)
    
    # Calculer statistiques
    print(f"Décalage moyen: {sum(map(float, dx_values))/len(dx_values):.2f} px")
    print(f"Rotation max: {max(map(abs, map(float, rot_values))):.2f}°")
    print(f"Variation échelle: {max(map(float, scale_values)) - min(map(float, scale_values)):.4f}")

analyze_alignment('alignment_params.txt')
```

### 5. Débogage de l'Alignement

**Objectif** : Comprendre pourquoi l'alignement échoue

**Méthode** :
1. Sauvegarder les images alignées
2. Ouvrir dans DS9 avec mode "blink"
3. Identifier visuellement les problèmes
4. Consulter les paramètres de l'image problématique

**Exemples de diagnostic** :
```
Rotation très élevée (>20°)
→ Problème : Image prise avec rotation de caméra différente
→ Solution : Exclure l'image ou corriger manuellement

Échelle très différente (>1.20)
→ Problème : Focale différente (ex: barlow ajoutée)
→ Solution : Séparer en deux groupes d'empilement

Décalages > 500 pixels
→ Problème : Champ différent (plusieurs objets ou mosaïque)
→ Solution : Empiler séparément puis assembler
```

---

## 🎓 Workflow Recommandé

### Workflow Basique

```
1. Charger images brutes
2. Aligner
3. Sauvegarder alignées
4. Vérifier alignment_params.txt
5. Empiler (dans FITS Stacker ou ailleurs)
```

### Workflow Avancé (Quality Control)

```
1. Charger images brutes
2. Aligner
3. Sauvegarder alignées
4. Analyser alignment_params.txt
   ├─ Identifier images aberrantes
   └─ Noter les statistiques
5. Inspecter visuellement (DS9)
   ├─ Vérifier rondes d'étoiles
   └─ Chercher trainées/satellites
6. Filtrer (garder seulement les meilleures)
7. Empiler sélectivement
```

### Workflow Professionnel (Photométrie)

```
1. Charger images calibrées (avec darks/flats)
2. Aligner avec grande précision
3. Sauvegarder alignées individuellement
4. Mesures photométriques sur chaque image
5. Analyse des variations temporelles
6. Empilement final si nécessaire
```

---

## 📊 Exemple Complet

### Scénario : Session M33

**Setup** :
- 20 images de 300s
- Monture EQ6-R Pro
- Autoguidage PHD2
- Seeing variable

**Résultats de l'Alignement** :

| Image | dx (px) | dy (px) | Rotation (°) | Scale | Qualité |
|-------|---------|---------|--------------|-------|---------|
| 001 | -15.2 | 23.8 | -2.35 | 1.0234 | ✓ Bonne |
| 002 | 12.5 | -8.3 | 1.89 | 0.9876 | ✓ Bonne |
| 003 | 0.0 | 0.0 | 0.00 | 1.0000 | ✓ Référence |
| ... | ... | ... | ... | ... | ... |
| 015 | 234.5 | -156.2 | -8.45 | 1.0123 | ✗ Décrochage |
| 016 | -8.9 | 14.2 | 0.89 | 1.0187 | ✓ Bonne |
| ... | ... | ... | ... | ... | ... |

**Analyse** :
- Image 015 : Décrochage du suivi (déplacements > 150px)
- Échelle stable (0.98 - 1.02) : Bon seeing
- Rotations faibles (< 3°) : Bon alignement polaire

**Action** :
```bash
# Exclure image 015
rm aligned_015_*.fits

# Empiler les 19 autres images
# (manuellement avec Siril ou dans FITS Stacker)
```

---

## 🔧 Intégration dans le Workflow

### Avec Siril

```bash
# 1. Sauvegarder les images alignées depuis FITS Stacker
# 2. Dans Siril:
cd /path/to/aligned/
seqfindstar aligned
register aligned
stack aligned

# Les images sont déjà alignées, le register sera très rapide
```

### Avec PixInsight

```javascript
// 1. Sauvegarder les images alignées
// 2. Dans PixInsight:
//    - ImageIntegration
//    - Input Files: aligned_*.fits
//    - Registration: None (déjà alignées)
//    - Integration: Average/Median
```

### Empilement Manuel Sélectif

```bash
# 1. Consulter alignment_params.txt
# 2. Sélectionner les meilleures images
# 3. Dans FITS Stacker:
#    - Ajouter uniquement les fichiers aligned_*.fits sélectionnés
#    - NE PAS réaligner (option à ajouter)
#    - Empiler directement
```

---

## ❓ FAQ

**Q: Pourquoi sauvegarder les images alignées ?**  
R: Pour inspection visuelle, filtrage de qualité, débogage, et réutilisation sans réalignement.

**Q: Les images sont-elles rognées ?**  
R: Non ! Le canvas est élargi pour conserver 100% des données.

**Q: Quelle est la taille des fichiers ?**  
R: Similaire aux originaux, mais le canvas peut être plus grand (donc fichiers légèrement plus gros).

**Q: Puis-je réempiler ces images ?**  
R: Oui ! Elles sont déjà alignées, vous pouvez les empiler directement.

**Q: L'image de référence a quels paramètres ?**  
R: dx=0, dy=0, rot=0°, scale=1.0 (généralement la première image).

**Q: Les paramètres sont-ils exacts ?**  
R: Oui, à la précision sub-pixel grâce à l'interpolation bilinéaire.

**Q: Peut-on modifier les paramètres manuellement ?**  
R: Non directement, mais vous pouvez éditer `alignment_params.txt` pour documentation.

---

## 🐛 Dépannage

### Problème 1 : Bouton "Sauvegarder Alignées" Grisé

**Cause** : Images pas encore alignées  
**Solution** : Cliquer d'abord sur "Aligner Images"

### Problème 2 : Fichiers Très Volumineux

**Cause** : Canvas très élargi (rotations importantes)  
**Symptôme** : Fichiers de plusieurs Go  
**Solution** : Normal si rotations > 30°

### Problème 3 : OutOfMemoryError

**Cause** : Pas assez de RAM pour canvas élargi  
**Solution** : 
```bash
java -Xmx8G -jar fits-stacker.jar
```

### Problème 4 : Paramètres Tous à Zéro

**Cause** : Images pas alignées (option désactivée)  
**Solution** : Vérifier que l'alignement a réussi

---

## 📝 Format du Fichier de Métadonnées

### Structure Complète

```
=== Paramètres d'Alignement FITS Stacker ===
[En-tête avec infos globales]

Canvas élargi: WIDTHxHEIGHT pixels
Offset global: dx=X, dy=Y
Image de référence: FILENAME

Détails par image:
================

Image N: FILENAME
  Décalage: dx=X.XX, dy=Y.YY pixels
  Rotation: R.RRR degrés
  Échelle: S.SSSS (SS.SS%)
  Translation: tx=X.XX, ty=Y.YY
  Fichier sauvegardé: ALIGNED_FILENAME

[Répété pour chaque image]

=== Fin du fichier ===
```

### Parsing Automatique

Le fichier est conçu pour être facilement parsé par scripts :

```python
# Exemple de parsing
import re

def parse_alignment_params(filepath):
    images = []
    with open(filepath) as f:
        content = f.read()
        
    # Extraire chaque bloc d'image
    blocks = re.findall(
        r'Image (\d+): (.+?)\n'
        r'  Décalage: dx=([-\d.]+), dy=([-\d.]+) pixels\n'
        r'  Rotation: ([-\d.]+) degrés\n'
        r'  Échelle: ([\d.]+)',
        content
    )
    
    for num, filename, dx, dy, rot, scale in blocks:
        images.append({
            'number': int(num),
            'filename': filename,
            'dx': float(dx),
            'dy': float(dy),
            'rotation': float(rot),
            'scale': float(scale)
        })
    
    return images

# Utilisation
images = parse_alignment_params('alignment_params.txt')
for img in images:
    print(f"{img['filename']}: rotation={img['rotation']}°")
```

---

## ✅ Checklist d'Utilisation

Après sauvegarde des images alignées :

- [ ] Fichiers `aligned_*.fits` créés
- [ ] Fichier `alignment_params.txt` présent
- [ ] Nombre de fichiers = nombre d'images chargées
- [ ] Noms de fichiers contiennent les paramètres
- [ ] Canvas élargi si rotations présentes
- [ ] Paramètres cohérents (pas de valeurs aberrantes)
- [ ] Images visualisables dans DS9/Siril
- [ ] Fichier de métadonnées lisible

---

**Fonctionnalité ajoutée à FITS Stacker v1.1**  
Date : 2024  
Documentation complète pour sauvegarde des images alignées avec paramètres
