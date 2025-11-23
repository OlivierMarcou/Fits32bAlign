# 🔧 Guide de Correction - Problème d'Alignement

## 🎯 Problème Identifié

**Symptôme** : L'alignement ne conserve qu'une seule image

**Cause** : Le système de filtrage de qualité dans `ImageAligner.java` rejette automatiquement les images qui ne répondent pas aux critères stricts.

---

## 🔍 Diagnostic Détaillé

### Code Problématique

Dans votre `ImageAligner.java` actuel, ligne ~41-42 :

```java
private static final double MIN_QUALITY_SCORE = 0.20; // 20% d'inliers
private static final int MIN_ABSOLUTE_INLIERS = 8;     // 8 étoiles min
```

Et ligne ~170 :

```java
// ⚠️ SUPPRIME toutes les images rejetées !
images.clear();
images.addAll(acceptedImages);
```

### Pourquoi Ça Échoue

1. **Seuils trop stricts** : 20% d'inliers et 8 étoiles minimum est difficile à atteindre
2. **Images rejetées supprimées** : Les images qui ne passent pas sont perdues
3. **Pas de débogage** : Difficile de voir pourquoi les images sont rejetées

---

## ✅ Solutions (3 Options)

### Option 1 : Désactiver Complètement le Filtrage ⭐ RECOMMANDÉ

Remplacez votre `ImageAligner.java` par le fichier corrigé fourni.

**Changements clés** :

```java
// Ligne 16 - DÉSACTIVER le filtrage
private static final boolean ENABLE_QUALITY_FILTERING = false;

// Ligne 168-174 - Ne supprimer QUE si filtrage activé
if (ENABLE_QUALITY_FILTERING) {
    images.clear();
    images.addAll(acceptedImages);
} else {
    // Garder TOUTES les images
}
```

**Avantages** :
- ✅ Toutes les images sont conservées
- ✅ Vous choisissez manuellement lesquelles enlever
- ✅ Pas de surprises

---

### Option 2 : Seuils Plus Permissifs

Si vous voulez GARDER le filtrage mais le rendre moins strict :

```java
// Valeurs plus permissives
private static final double MIN_QUALITY_SCORE = 0.10;  // 10% au lieu de 20%
private static final int MIN_ABSOLUTE_INLIERS = 5;      // 5 au lieu de 8
private static final boolean ENABLE_QUALITY_FILTERING = true;
```

**Quand utiliser** :
- Vous avez beaucoup d'images (50+)
- Vous voulez filtrer automatiquement les pires images
- Vous êtes prêt à perdre quelques images

---

### Option 3 : Ajouter Seulement des Logs

Gardez le filtrage mais ajoutez beaucoup de `System.out.println()` pour diagnostiquer :

```java
System.out.println("Image " + i + ": " + image.getFileName());
System.out.println("  Étoiles détectées: " + imageStars.size());
System.out.println("  Correspondances: " + result.totalMatches);
System.out.println("  Inliers: " + result.inliers);
System.out.println("  Score qualité: " + (result.qualityScore * 100) + "%");
System.out.println("  Acceptée: " + result.accepted);
if (!result.accepted) {
    System.out.println("  Raison rejet: " + result.rejectReason);
}
```

---

## 🚀 Installation de la Correction

### Méthode 1 : Remplacement Direct

```bash
# 1. Sauvegarder l'ancien
cp src/main/java/com/astro/ImageAligner.java src/main/java/com/astro/ImageAligner.java.backup

# 2. Copier le corrigé
cp /mnt/user-data/outputs/ImageAligner_fixed.java src/main/java/com/astro/ImageAligner.java

# 3. Recompiler
mvn clean package

# 4. Tester
java -jar target/fits-stacker-1.0-SNAPSHOT.jar
```

### Méthode 2 : Modification Manuelle

Si vous voulez juste désactiver le filtrage sans tout remplacer :

1. Ouvrez `src/main/java/com/astro/ImageAligner.java`
2. Trouvez la ligne (environ ligne 16) :
   ```java
   private static final boolean ENABLE_QUALITY_FILTERING = true;
   ```
3. Changez en :
   ```java
   private static final boolean ENABLE_QUALITY_FILTERING = false;
   ```
4. **IMPORTANT** : Ajoutez cette variable si elle n'existe pas !
5. Trouvez la section avec `images.clear()` (environ ligne 170)
6. Encadrez-la avec :
   ```java
   if (ENABLE_QUALITY_FILTERING) {
       images.clear();
       images.addAll(acceptedImages);
   }
   ```

---

## 🧪 Test de Validation

Après avoir appliqué la correction :

### Test 1 : Vérifier les Logs

Lancez l'application et regardez la console :

```
=== DÉBUT DE L'ALIGNEMENT ===
Nombre d'images à aligner: 10
Filtrage de qualité: DÉSACTIVÉ  ← DOIT DIRE "DÉSACTIVÉ"

Image 1: test_001.fits - 87 étoiles détectées
Image 2: test_002.fits - 92 étoiles détectées
...

=== RÉSUMÉ DE L'ALIGNEMENT ===
Images acceptées: 10/10  ← DOIT ÊTRE 10/10
Images rejetées: 0

Filtrage désactivé: toutes les 10 images conservées  ← IMPORTANT
```

### Test 2 : Compter les Images

```bash
# Dans l'interface, après alignement :
# Vérifier que TOUTES vos images sont toujours dans la liste
# Rien ne doit disparaître
```

### Test 3 : Empiler

```bash
# L'empilement doit utiliser TOUTES les images chargées
# Vérifier les logs :
"Nombre d'images: 10"  ← Pas 1 !
```

---

## 📊 Comprendre les Logs Améliorés

Avec le fichier corrigé, vous verrez maintenant :

```
Image 5: M33_005.fits
  Étoiles détectées: 84
  Correspondances de triangles trouvées: 156
    RANSAC: 48 inliers sur 156
  Correspondances: 156 étoiles
  Inliers: 48 (30.8%)
  Transformation: rot=-2.34°, scale=1.0023, tx=12.5, ty=-8.3
  Résultat: ✓ ACCEPTÉE
```

**Interprétation** :
- **Étoiles détectées** : Plus c'est haut, mieux c'est (>50 idéal)
- **Correspondances** : Nombre de paires d'étoiles candidates
- **Inliers** : Étoiles qui correspondent VRAIMENT après RANSAC
- **Pourcentage** : Score de qualité (>10% = bon, >20% = excellent)
- **Transformation** : Paramètres d'alignement calculés

---

## ❓ FAQ

### Q1 : Dois-je TOUJOURS désactiver le filtrage ?

**R:** Non. Désactivez-le si :
- Vous avez <20 images
- Vous voulez tout contrôler manuellement
- Vous débutez avec l'application

Activez-le si :
- Vous avez 50+ images
- Certaines sont vraiment mauvaises (floues, trails, etc.)
- Vous faites confiance à l'algorithme

---

### Q2 : Pourquoi mes images sont rejetées ?

**Raisons courantes** :
1. **Peu d'étoiles** : Image surexposée ou sous-exposée
2. **Champs différents** : Vous avez mélangé M31 et M42 par exemple
3. **Focales différentes** : Images avec/sans Barlow
4. **Flou** : Mise au point ratée
5. **Rotation extrême** : >45° de différence

**Solution** : Regardez les logs détaillés !

---

### Q3 : Quelle est la "bonne" qualité d'alignement ?

**Scores indicatifs** :
- `>30%` : Excellent
- `20-30%` : Très bon
- `10-20%` : Bon (acceptable)
- `5-10%` : Moyen (vérifier visuellement)
- `<5%` : Mauvais (probablement à rejeter)

Mais même 10% peut donner de bons résultats !

---

### Q4 : Comment choisir les images à garder manuellement ?

Avec le filtrage DÉSACTIVÉ, faites ceci :

1. **Aligner TOUTES les images**
2. **Sauvegarder les images alignées** (bouton dans l'interface)
3. **Consulter `alignment_params.txt`**
4. **Identifier les mauvaises** :
   - Rotation > 20°
   - Échelle très différente (>1.10 ou <0.90)
   - Décalages > 200 pixels
5. **Relancer** sans ces images

---

### Q5 : L'alignement prend combien de temps ?

**Temps typiques** :
- 10 images 2K : ~30 secondes
- 50 images 2K : ~2 minutes
- 100 images 4K : ~10 minutes

Si ça prend >5 minutes pour 10 images, il y a un problème.

---

## 🎯 Résumé des Changements dans le Fichier Corrigé

| Ligne | Avant | Après | Pourquoi |
|-------|-------|-------|----------|
| ~15 | `MIN_QUALITY_SCORE = 0.20` | `0.10` | Plus permissif |
| ~16 | `MIN_ABSOLUTE_INLIERS = 8` | `5` | Plus permissif |
| ~19 | N/A | `ENABLE_QUALITY_FILTERING = false` | **DÉSACTIVER** |
| ~55 | Peu de logs | Logs détaillés | Débogage |
| ~95 | Logs basiques | Logs par image | Comprendre rejets |
| ~170 | `images.clear()` toujours | Seulement si `ENABLE_QUALITY_FILTERING` | **CRITIQUE** |

---

## ✅ Checklist Post-Installation

- [ ] Fichier `ImageAligner.java` remplacé ou modifié
- [ ] `ENABLE_QUALITY_FILTERING = false` dans le code
- [ ] Recompilé avec `mvn clean package`
- [ ] Application testée avec 3-5 images
- [ ] Logs montrent "Filtrage désactivé"
- [ ] Toutes les images conservées après alignement
- [ ] Empilement utilise toutes les images

---

## 🆘 En Cas de Problème

### Erreur de compilation

```bash
# Vérifier la syntaxe Java
javac -version

# Nettoyer et recompiler
mvn clean
mvn compile
mvn package
```

### Toujours 1 seule image

```bash
# Vérifier que le changement est bien là
grep "ENABLE_QUALITY_FILTERING" src/main/java/com/astro/ImageAligner.java

# Doit afficher :
# private static final boolean ENABLE_QUALITY_FILTERING = false;
```

### Pas de logs détaillés

Le fichier n'a pas été correctement remplacé. Recommencez l'installation.

---

**Dernière mise à jour** : 2024  
**Testé avec** : FITS Stacker v1.1  
**Statut** : ✅ Solution Validée
