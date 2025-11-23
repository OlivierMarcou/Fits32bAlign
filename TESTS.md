# Tests et Exemples - FITS Stacker

## Génération d'Images de Test

Si vous n'avez pas d'images FITS astronomiques, voici comment créer des images de test.

### Script Python pour générer des FITS de test

```python
#!/usr/bin/env python3
"""
Génère des images FITS de test avec des étoiles artificielles
Nécessite: astropy, numpy
"""
import numpy as np
from astropy.io import fits
import os

def create_star_field(width=2048, height=2048, num_stars=100, noise_level=10):
    """Crée un champ d'étoiles artificiel"""
    # Image de fond avec bruit
    image = np.random.normal(100, noise_level, (height, width)).astype(np.float32)
    
    # Ajouter des étoiles
    for _ in range(num_stars):
        x = np.random.randint(50, width-50)
        y = np.random.randint(50, height-50)
        brightness = np.random.uniform(500, 5000)
        size = np.random.uniform(2, 5)
        
        # Créer une PSF gaussienne
        y_grid, x_grid = np.ogrid[-10:11, -10:11]
        star = brightness * np.exp(-(x_grid**2 + y_grid**2) / (2 * size**2))
        
        # Ajouter à l'image
        y1, y2 = max(0, y-10), min(height, y+11)
        x1, x2 = max(0, x-10), min(width, x+11)
        sy1, sy2 = 10-(y-y1), 10+(y2-y)
        sx1, sx2 = 10-(x-x1), 10+(x2-x)
        
        image[y1:y2, x1:x2] += star[sy1:sy2, sx1:sx2]
    
    return image

def generate_test_set(output_dir="test_fits", num_images=10):
    """Génère un ensemble d'images de test avec de légers décalages"""
    os.makedirs(output_dir, exist_ok=True)
    
    # Créer le champ de base
    print(f"Génération de {num_images} images de test...")
    base_field = create_star_field()
    
    for i in range(num_images):
        # Ajouter un léger décalage et rotation
        offset_x = np.random.randint(-20, 21)
        offset_y = np.random.randint(-20, 21)
        
        # Créer une image décalée
        image = np.roll(base_field, (offset_y, offset_x), axis=(0, 1))
        
        # Ajouter du bruit unique
        image += np.random.normal(0, 5, image.shape).astype(np.float32)
        
        # Sauvegarder
        hdu = fits.PrimaryHDU(image)
        filename = os.path.join(output_dir, f"test_image_{i+1:03d}.fits")
        hdu.writeto(filename, overwrite=True)
        print(f"  Créé: {filename} (offset: {offset_x}, {offset_y})")
    
    print(f"\n✓ {num_images} images générées dans {output_dir}/")
    print(f"  Vous pouvez maintenant les utiliser dans FITS Stacker!")

if __name__ == "__main__":
    generate_test_set()
```

### Utilisation

```bash
# Installer les dépendances
pip install astropy numpy --break-system-packages

# Générer les images
python3 generate_test_fits.py

# Résultat : dossier test_fits/ avec 10 images
```

## Tests de Validation

### Test 1 : Alignement basique

**Objectif** : Vérifier que l'alignement détecte et corrige les décalages

**Procédure** :
1. Générer 5 images avec des décalages de 10-20 pixels
2. Charger dans FITS Stacker
3. Aligner
4. Empiler avec "Moyenne"
5. **Résultat attendu** : Étoiles ponctuelles et nettes

### Test 2 : Réjection du bruit

**Objectif** : Vérifier que Sigma Clipping élimine les pixels aberrants

**Procédure** :
1. Prendre les images de test
2. Ajouter manuellement des "hot pixels" à quelques images
3. Empiler avec "Moyenne" → sauvegarder résultat A
4. Empiler avec "Sigma Clipping" → sauvegarder résultat B
5. **Résultat attendu** : B a moins de pixels chauds que A

### Test 3 : Comparaison des méthodes

**Objectif** : Comparer visuellement les différentes méthodes

**Procédure** :
1. Utiliser un jeu de 20 images identiques
2. Empiler avec chaque méthode
3. Comparer dans un visualiseur FITS
4. **Résultats attendus** :
   - Moyenne : image la plus lumineuse
   - Médiane : similaire mais légèrement moins lumineuse
   - Sigma Clip : entre les deux, plus propre
   - Maximum : plus lumineuse, peut avoir des artefacts
   - Minimum : plus sombre, fond du ciel bien défini

### Test 4 : Performance

**Objectif** : Mesurer les temps de traitement

**Procédure** :
```
Images : 10x 2048x2048 pixels
Machine : [votre config]

Résultats attendus :
- Chargement : < 5 secondes
- Alignement : 10-30 secondes
- Empilement :
  * Moyenne : 20-40 secondes
  * Médiane : 40-90 secondes
  * Sigma Clip : 60-120 secondes
```

## Tests avec Vraies Images

### Sources d'Images FITS

**NASA Archives**
- https://archive.stsci.edu/
- Images du télescope Hubble en FITS

**Amateur Astrophotography**
- https://www.astrobin.com/
- Rechercher des images avec téléchargement FITS

**Sample Data**
- http://fits.gsfc.nasa.gov/fits_samples.html
- Exemples officiels de fichiers FITS

### Objets Recommandés pour Tests

| Objet      | Difficulté | Pourquoi                          |
|------------|------------|-----------------------------------|
| M31        | Facile     | Grande, nombreuses étoiles        |
| M42        | Moyen      | Nébulosité + étoiles              |
| M13        | Facile     | Amas globulaire, étoiles denses   |
| Lune       | Facile     | Détails de surface                |
| IC 1805    | Difficile  | Nébulosité faible, peu d'étoiles  |

## Validation des Résultats

### Critères de Qualité

**Bon alignement** :
- ✓ Étoiles ponctuelles partout dans l'image
- ✓ Pas de dédoublement
- ✓ Pas de traînées

**Mauvais alignement** :
- ✗ Étoiles allongées ou doublées
- ✗ Halos autour des objets brillants
- ✗ Bords flous

### Inspection Visuelle

Utilisez un visualiseur FITS comme :
- **DS9** (SAOImage DS9)
- **FITS Liberator**
- **Siril** (pour comparaison)
- **PixInsight** (professionnel)

### Métriques Quantitatives

```python
# Script pour calculer le FWHM (Full Width Half Maximum)
from astropy.io import fits
from photutils.detection import DAOStarFinder

def measure_quality(fits_file):
    """Mesure la qualité d'une image empilée"""
    data = fits.getdata(fits_file)
    
    # Détection d'étoiles
    finder = DAOStarFinder(threshold=3.0 * std(data), fwhm=3.0)
    sources = finder(data - median(data))
    
    # FWHM moyen
    mean_fwhm = np.mean(sources['fwhm'])
    
    # Signal/Bruit estimé
    background = np.median(data)
    noise = np.std(data)
    snr = background / noise
    
    print(f"FWHM moyen: {mean_fwhm:.2f} pixels")
    print(f"SNR estimé: {snr:.2f}")
    print(f"Étoiles détectées: {len(sources)}")
    
    return mean_fwhm, snr
```

## Cas d'Usage Réels

### Cas 1 : Deep Sky Simple

```
Sujet : M31 (Galaxie d'Andromède)
Images : 30 x 180s @ ISO 800
Format : Canon CR2 → converti en FITS
Setup : Réfracteur 80mm f/6

Workflow :
1. Conversion CR2 → FITS (SIRIL ou autre)
2. FITS Stacker → Alignement
3. FITS Stacker → Sigma Clipping
4. Post-traitement dans Siril/PixInsight
```

### Cas 2 : Planétaire

```
Sujet : Jupiter
Images : 500 frames × 1/60s
Format : FITS natif (ZWO ASI)
Setup : Newton 200mm f/5

Workflow :
1. Sélection des 100 meilleures frames (autre soft)
2. FITS Stacker → Alignement
3. FITS Stacker → Maximum (pour détails)
4. Ondelettes pour affûter
```

### Cas 3 : Grand Champ

```
Sujet : Voie Lactée
Images : 20 × 30s @ ISO 3200
Format : RAW → FITS
Setup : 50mm f/1.8 sur DSLR

Workflow :
1. Conversion RAW → FITS
2. FITS Stacker → Alignement (attention au champ large!)
3. FITS Stacker → Moyenne Pondérée
4. Gradient removal + color calibration
```

## Troubleshooting Tests

### "Aucune étoile détectée"

**Cause** : Image trop sombre ou trop bruitée

**Solution** :
```java
// Modifier dans StarDetector.java
private static final double THRESHOLD_MULTIPLIER = 2.0; // au lieu de 3.0
```

### "Alignement incorrect"

**Cause** : Pas assez d'étoiles communes entre images

**Solution** :
- Vérifier que toutes les images montrent le même champ
- Augmenter le nombre d'étoiles détectées
- Utiliser des images avec plus d'étoiles

### "Out of Memory"

**Solution** :
```bash
# Augmenter la mémoire JVM
java -Xmx8G -jar fits-stacker.jar

# Ou traiter par lots plus petits
```

## Rapport de Test Type

```
=== RAPPORT DE TEST FITS STACKER ===

Date : 2024-XX-XX
Version : 1.0

Configuration :
- Java : 21.0.X
- OS : Ubuntu 24.04 / Windows 11 / macOS
- RAM : 16GB
- CPU : Intel i7 / AMD Ryzen

Images test :
- Nombre : 10
- Dimensions : 2048x2048
- Format : FITS float32

Résultats :
✓ Chargement : OK (4.2s)
✓ Détection étoiles : 87 étoiles détectées
✓ Alignement : OK (18.3s)
✓ Empilement Moyenne : OK (23.1s)
✓ Empilement Médiane : OK (41.7s)
✓ Empilement Sigma Clip : OK (52.3s)

Qualité visuelle :
✓ Étoiles ponctuelles
✓ Pas de dédoublement
✓ Réduction visible du bruit

Conclusion : TOUS LES TESTS PASSÉS ✓
```

---

Pour contribuer avec vos propres tests, créez une issue sur le dépôt du projet !
