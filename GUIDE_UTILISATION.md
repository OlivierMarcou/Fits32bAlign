# Guide d'Utilisation Rapide - FITS Stacker

## Interface Utilisateur

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  FITS Stacker                                                               │
│  Alignement et empilement d'images astronomiques                           │
└─────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────┬───────────────────────────────────────┐
│ Images FITS                        │ Méthode d'empilement                  │
│ ┌────────────────────────────────┐ │ ┌───────────────────────────────────┐ │
│ │ 1. image_001.fits              │ │ │ Sigma Clipping               ▼   │ │
│ │ 2. image_002.fits              │ │ └───────────────────────────────────┘ │
│ │ 3. image_003.fits              │ │                                       │
│ │ 4. image_004.fits              │ │ ┌───────────────────────────────────┐ │
│ │ 5. image_005.fits              │ │ │     ALIGNER IMAGES               │ │
│ │ ...                            │ │ └───────────────────────────────────┘ │
│ │                                │ │                                       │
│ │                                │ │ ┌───────────────────────────────────┐ │
│ │                                │ │ │     EMPILER IMAGES               │ │
│ └────────────────────────────────┘ │ └───────────────────────────────────┘ │
│                                    │                                       │
│ [Ajouter Fichiers] [Ajouter      │ Journal                               │
│  Dossier] [Retirer] [Tout Effacer] │ ┌───────────────────────────────────┐ │
│                                    │ │ Ajouté: image_001.fits            │ │
└────────────────────────────────────┤ │ Ajouté: image_002.fits            │ │
                                     │ │ === Début de l'alignement ===     │ │
Status: Prêt                         │ │ Chargé: image_001.fits (4096x...)│ │
┌──────────────────────────────────┐ │ │ Détection des étoiles...          │ │
│ ▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░  45%    │ │ │ Alignement de image_002.fits...  │ │
└──────────────────────────────────┘ │ └───────────────────────────────────┘ │
                                     └───────────────────────────────────────┘
```

## Workflow Typique

### Scénario 1 : Session simple

```bash
1. Lancer l'application
   $ java -jar fits-stacker.jar

2. Ajouter vos images
   - Clic sur "Ajouter Dossier"
   - Sélectionner le dossier contenant vos lights
   - 10 images ajoutées

3. Aligner
   - Clic sur "Aligner Images"
   - Attendre la fin (barre de progression)
   - Journal montre : "Alignement terminé avec succès"

4. Empiler
   - Choisir "Sigma Clipping" dans le menu
   - Clic sur "Empiler Images"
   - Choisir "stacked_m31.fits" comme nom de sortie
   - Attendre la fin

5. Résultat
   - Votre image empilée est prête dans stacked_m31.fits
   - Ouvrir avec votre logiciel de traitement préféré
```

### Scénario 2 : Comparaison de méthodes

```bash
1. Charger vos images (ex: 20 images de M42)

2. Aligner une seule fois
   - Les offsets sont calculés et conservés

3. Empiler avec "Moyenne"
   - Sauvegarder : m42_average.fits
   
4. Empiler avec "Médiane"
   - Sauvegarder : m42_median.fits
   
5. Empiler avec "Sigma Clipping"
   - Sauvegarder : m42_sigma.fits

6. Comparer les trois résultats dans votre logiciel
```

## Conseils Pratiques

### Préparation des images

```
Avant d'utiliser FITS Stacker, assurez-vous que :
✓ Toutes les images sont au même format FITS
✓ Les images ont des dimensions similaires (tolérance possible)
✓ Les images sont des LIGHT frames (pas de dark/flat/bias)
✓ Les images sont du même sujet avec des étoiles visibles
```

### Choix de la méthode d'empilement

| Méthode           | Quand l'utiliser                              | Avantages                |
|-------------------|-----------------------------------------------|--------------------------|
| Moyenne           | Images propres, pas d'artefacts              | Rapide, simple           |
| Médiane           | Présence de satellites, avions              | Robuste aux outliers     |
| Sigma Clipping    | Astrophotographie générale (RECOMMANDÉ)     | Meilleur rapport S/N     |
| Maximum           | Capture de météores                          | Conserve les pics        |
| Minimum           | Élimination de traces                        | Conserve le fond         |
| Moyenne Pondérée  | Qualité variable des images                  | Privilégie les meilleurs |

### Optimisation des performances

```bash
# Pour de grandes images ou beaucoup de fichiers
java -Xmx8G -jar fits-stacker.jar

# Allocation mémoire recommandée :
# - 10 images 2000x2000 : 2GB
# - 50 images 4000x4000 : 8GB
# - 100 images 6000x6000 : 16GB
```

### Résolution de problèmes

**Problème : "Peu d'étoiles détectées"**
```
Solution : Vérifier que vos images :
- Ne sont pas surexposées
- Contiennent des étoiles visibles
- Ont un bon contraste
```

**Problème : Alignement imprécis**
```
Solution :
- Utiliser votre meilleure image en premier
- S'assurer que toutes les images ont le même champ
- Vérifier que les étoiles sont bien nettes
```

**Problème : Manque de mémoire**
```
Solution :
java -Xmx16G -jar fits-stacker.jar
```

## Exemples de Commandes

### Lancement standard
```bash
java -jar fits-stacker.jar
```

### Avec plus de mémoire
```bash
java -Xmx8G -jar fits-stacker.jar
```

### Mode console (futur)
```bash
# Cette fonctionnalité pourrait être ajoutée
java -jar fits-stacker.jar --batch \
  --input /path/to/lights/*.fits \
  --method sigma-clip \
  --output stacked.fits
```

## Support des Formats

### Formats FITS supportés
- ✅ FITS float (32-bit)
- ✅ FITS short/unsigned short (16-bit)
- ✅ FITS int (32-bit)
- ✅ FITS double (64-bit)
- ✅ Extensions : .fits, .fit, .fts

### Formats non supportés
- ❌ XISF (format PixInsight)
- ❌ TIFF 16/32-bit
- ❌ RAW (CR2, NEF, etc.)

Pour ces formats, utilisez d'abord un convertisseur vers FITS.

## Workflow Complet d'Astrophotographie

```
1. Acquisition
   └─> Images RAW (DSLR) ou FITS (caméra astro)

2. Conversion (si nécessaire)
   └─> Convertir en FITS

3. FITS Stacker
   ├─> Aligner les images
   └─> Empiler (Sigma Clipping)

4. Traitement
   ├─> Étirement d'histogramme
   ├─> Balance des couleurs
   ├─> Réduction du bruit
   └─> Amélioration des détails

5. Résultat final !
```

---

Pour plus d'informations, consultez le README.md principal.
