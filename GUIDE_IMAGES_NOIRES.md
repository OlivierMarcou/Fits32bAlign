# 🔧 Diagnostic : Images Alignées Noires

## 🎯 Symptôme

Après l'alignement, toutes les images **sauf la première** sont complètement **noires** (pixels = 0).

## 🔍 Cause du Problème

Le problème vient de la **transformation inverse** dans `createAlignedCopy()`. Voici ce qui se passe :

### Image de Référence (la première)
```java
transform = identité (rotation=0, scale=1, tx=0, ty=0)
→ transformInverse(x, y) retourne (x, y)
→ Les coordonnées restent valides
→ L'image s'affiche correctement ✓
```

### Autres Images
```java
transform = affine (rotation≠0, scale≠1, tx≠0, ty≠0)
→ transformInverse(x, y) retourne des coordonnées invalides
→ Toutes les coordonnées tombent HORS de l'image source
→ interpolate() retourne 0 pour tous les pixels
→ Image complètement NOIRE ✗
```

---

## 🧪 Test Rapide pour Confirmer

Lancez votre application et regardez les logs. Si vous voyez :

```
=== Création copie alignée ===
Image source: M31_002.fits
  Dimensions source: 2048x2048
  Canvas destination: 2150x2150
  Transformation:
    rotation = -2.35°
    scale = 1.0234
    tx = -15.23
    ty = 23.84
  Résultat:
    Pixels valides: 0/4622500 (0.0%)  ← PROBLÈME ICI !
    ⚠️⚠️⚠️ ATTENTION: AUCUN PIXEL VALIDE!
```

C'est exactement votre problème.

---

## 🔧 Solutions

### Solution 1 : Utiliser la Version de Débogage ⭐ RECOMMANDÉ

J'ai créé une version corrigée avec beaucoup de logs de diagnostic.

**Installation** :

```bash
# 1. Sauvegarder l'ancien
cp src/main/java/com/astro/FitsImage.java \
   src/main/java/com/astro/FitsImage.java.backup

# 2. Copier la version de débogage
cp /mnt/user-data/outputs/FitsImage_debug.java \
   src/main/java/com/astro/FitsImage.java

# 3. Recompiler
mvn clean package

# 4. Lancer et REGARDER LES LOGS
java -jar target/fits-stacker-1.0-SNAPSHOT.jar
```

**Ce qui va se passer** :

Les logs vont maintenant afficher :
```
=== Création copie alignée ===
Image source: M31_002.fits
  Dimensions source: 2048x2048
  Canvas destination: 2150x2150
  Offset: (50, 50)
  Transformation:
    rotation = -2.35°
    scale = 1.0234
    tx = -15.23
    ty = 23.84
  
  TEST DIAGNOSTIC:
    Centre canvas (1075, 1075)
    → Source: (2500.5, -300.2)    ← HORS LIMITES !
    Image source: 0-2047, 0-2047
    ✗ Centre hors limites!

  Résultat:
    Pixels valides: 0/4622500 (0.0%)
    ⚠️⚠️⚠️ ATTENTION: AUCUN PIXEL VALIDE!
```

Cela nous dira **exactement** quel est le problème.

---

### Solution 2 : Vérifier l'Implémentation de applyInverse()

Le problème peut aussi venir de la méthode `applyInverse()` dans `ImageAligner.AffineTransform`.

**Code actuel** (potentiellement problématique) :

```java
public double[] applyInverse(double x, double y) {
    double cos = Math.cos(-rotation);
    double sin = Math.sin(-rotation);

    double dx = x - tx;
    double dy = y - ty;

    double newX = (dx * cos - dy * sin) / scale;
    double newY = (dx * sin + dy * cos) / scale;

    return new double[]{newX, newY};
}
```

**Version corrigée** :

```java
public double[] applyInverse(double x, double y) {
    // Étape 1: Soustraire la translation
    double dx = x - tx;
    double dy = y - ty;

    // Étape 2: Appliquer rotation inverse
    double cos = Math.cos(-rotation);
    double sin = Math.sin(-rotation);
    
    double rx = dx * cos - dy * sin;
    double ry = dx * sin + dy * cos;

    // Étape 3: Appliquer scale inverse
    double newX = rx / scale;
    double newY = ry / scale;

    return new double[]{newX, newY};
}
```

**C'est exactement la même chose** ! Donc le problème vient d'**ailleurs**.

---

### Solution 3 : Le Vrai Problème - Ordre des Opérations

Le problème est probablement dans la méthode `apply()` (transformation directe) !

**Dans ImageAligner.AffineTransform**, la méthode `apply()` devrait être :

```java
public double[] apply(double x, double y) {
    // Ordre correct : Scale → Rotation → Translation
    
    // 1. Échelle
    double sx = x * scale;
    double sy = y * scale;
    
    // 2. Rotation
    double cos = Math.cos(rotation);
    double sin = Math.sin(rotation);
    
    double rx = sx * cos - sy * sin;
    double ry = sx * sin + sy * cos;
    
    // 3. Translation
    double newX = rx + tx;
    double newY = ry + ty;

    return new double[]{newX, newY};
}
```

Et `applyInverse()` doit faire l'inverse dans l'ordre inverse :

```java
public double[] applyInverse(double x, double y) {
    // Ordre inverse : Translation → Rotation → Scale
    
    // 1. Enlever translation
    double dx = x - tx;
    double dy = y - ty;
    
    // 2. Rotation inverse
    double cos = Math.cos(-rotation);
    double sin = Math.sin(-rotation);
    
    double rx = dx * cos - dy * sin;
    double ry = dx * sin + dy * cos;
    
    // 3. Scale inverse
    double newX = rx / scale;
    double newY = ry / scale;

    return new double[]{newX, newY};
}
```

---

## 🎯 Diagnostic avec les Logs

Après avoir installé `FitsImage_debug.java`, lancez votre application et cherchez dans les logs :

### Cas 1 : Image de Référence (OK)

```
=== Création copie alignée ===
Image source: M31_001.fits
  Transformation:
    rotation = 0.0°
    scale = 1.0
    tx = 0.0
    ty = 0.0
  Résultat:
    Pixels valides: 4194304/4194304 (100.0%)  ← PARFAIT !
```

### Cas 2 : Autres Images (PROBLÈME)

```
=== Création copie alignée ===
Image source: M31_002.fits
  Transformation:
    rotation = -2.35°
    scale = 1.0234
    tx = -15.23
    ty = 23.84
  
  Échantillon de points transformés:
    Point canvas (500.0, 500.0) → source (-1200.5, -800.2)  ← HORS LIMITES !
    Point canvas (1000.0, 1000.0) → source (-700.8, -300.5)  ← HORS LIMITES !
  
  Résultat:
    Pixels valides: 0/4622500 (0.0%)
    ⚠️⚠️⚠️ ATTENTION: AUCUN PIXEL VALIDE!
```

**Interprétation** : Les coordonnées transformées sont négatives → problème dans la transformation !

---

## 🔍 Causes Possibles

### 1. Transformation Directe Incorrecte

Le calcul dans `ImageAligner` lors de l'alignement produit des paramètres incorrects.

**Symptômes** :
- `tx` et `ty` ont des valeurs énormes (>1000)
- `scale` très différent de 1.0 (>2.0 ou <0.5)

**Solution** : Vérifier la méthode `computeAffineFromMatches()` dans `ImageAligner.java`

### 2. Offset de Canvas Mal Appliqué

Le canvas est élargi mais l'offset n'est pas appliqué correctement.

**Symptômes** :
```
Canvas destination: 2150x2150
Offset: (50, 50)
```

Mais dans `createAlignedCopy()`, on fait :
```java
double[] srcPoint = transformInverse(x - offsetX, y - offsetY);
```

**Vérification** : L'offset devrait être **ajouté**, pas soustrait ?

### 3. Centre de Rotation Incorrect

La transformation est calculée par rapport au centre de l'image, mais appliquée par rapport à l'origine.

**Solution** : Ajouter une translation avant/après la rotation.

---

## ✅ Checklist de Diagnostic

Après avoir installé la version de débogage :

1. [ ] Les logs montrent `=== Création copie alignée ===`
2. [ ] Pour l'image 1 : `Pixels valides: XXX/XXX (100.0%)`
3. [ ] Pour les autres : `Pixels valides: 0/XXX (0.0%)` ← PROBLÈME !
4. [ ] Les logs montrent des coordonnées négatives ou hors limites
5. [ ] Identifiez quelle transformation est incorrecte

---

## 🚀 Action Immédiate

**Faites ceci maintenant** :

```bash
# 1. Installer la version de débogage
cp /mnt/user-data/outputs/FitsImage_debug.java \
   src/main/java/com/astro/FitsImage.java

# 2. Recompiler
mvn clean package

# 3. Lancer et COPIER les logs
java -jar target/fits-stacker-1.0-SNAPSHOT.jar 2>&1 | tee alignment_debug.log

# 4. Chercher les lignes problématiques
grep "AUCUN PIXEL VALIDE" alignment_debug.log
grep "Centre hors limites" alignment_debug.log
```

**Ensuite, ENVOYEZ-MOI** :
- Les logs de la section `=== Création copie alignée ===` 
- Pour AU MOINS une image qui devient noire
- Je pourrai alors identifier le problème exact !

---

## 🎓 Explications Théoriques

### Transformation Affine Correcte

Une transformation affine complète se compose de :

```
1. Scale (échelle)          : x' = x * s,  y' = y * s
2. Rotation                 : x'' = x' * cos(θ) - y' * sin(θ)
                              y'' = x' * sin(θ) + y' * cos(θ)
3. Translation              : x_final = x'' + tx
                              y_final = y'' + ty
```

L'inverse doit faire l'opposé dans l'ordre inverse :

```
1. Translation inverse      : x' = x - tx,  y' = y - ty
2. Rotation inverse         : x'' = x' * cos(-θ) - y' * sin(-θ)
                              y'' = x' * sin(-θ) + y' * cos(-θ)
3. Scale inverse            : x_src = x'' / s
                              y_src = y'' / s
```

### Coordonnées et Canvas

```
Image source : 2048 x 2048
Canvas élargi: 2150 x 2150
Offset       : (50, 50)

Point canvas (1000, 1000) doit mapper à:
→ Point relatif: (1000 - 50, 1000 - 50) = (950, 950)
→ Appliquer transform.applyInverse(950, 950)
→ Devrait donner un point dans [0, 2047]
```

Si le point calculé est négatif ou >2047 → ERREUR !

---

## 📞 Prochaines Étapes

1. **Installez la version de débogage**
2. **Lancez l'application**
3. **Copiez les logs** de la première image noire
4. **Envoyez-moi** ces logs

Je pourrai alors vous dire **exactement** où est le bug et comment le corriger !

---

**Dernière mise à jour** : 2024  
**Fichier de débogage** : `FitsImage_debug.java`  
**Statut** : 🔍 Diagnostic en cours
