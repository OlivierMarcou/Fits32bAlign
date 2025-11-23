# 🔧 CORRECTIF COMPLET - Images Noires

## 🎯 Problème Identifié

Le problème vient probablement du fait que la **transformation est calculée par rapport au centre de l'image**, mais **appliquée par rapport à l'origine (0,0)**.

## 📋 Corrections à Appliquer

### Correction 1 : Dans ImageAligner.java - computeAffineFromMatches()

La méthode calcule la transformation en utilisant les centroïdes :

```java
// Calcule centroïdes
double refCx = (m1.ref.getX() + m2.ref.getX() + m3.ref.getX()) / 3.0;
double refCy = (m1.ref.getY() + m2.ref.getY() + m3.ref.getY()) / 3.0;

// Translation calculée par rapport aux centroïdes
double tx = refCx - imgCx * scale * Math.cos(rotation) + imgCy * scale * Math.sin(rotation);
double ty = refCy - imgCx * scale * Math.sin(rotation) - imgCy * scale * Math.cos(rotation);
```

**PROBLÈME** : Cette translation `tx, ty` inclut déjà la position des centroïdes.

### Correction 2 : Solution Alternative - Transformation Par Rapport au Centre de l'Image

Au lieu d'utiliser les centroïdes des correspondances, calculez TOUJOURS par rapport au **centre de l'image** :

```java
private static AffineTransform computeAffineFromMatches(List<StarMatch> matches,
                                                         int refWidth, int refHeight,
                                                         int imgWidth, int imgHeight) {
    if (matches.size() < 3) return null;

    StarMatch m1 = matches.get(0);
    StarMatch m2 = matches.get(1);
    
    // Centres des images
    double refCenterX = refWidth / 2.0;
    double refCenterY = refHeight / 2.0;
    double imgCenterX = imgWidth / 2.0;
    double imgCenterY = imgHeight / 2.0;

    // Vecteurs par rapport aux centres
    double refDx = m2.ref.getX() - m1.ref.getX();
    double refDy = m2.ref.getY() - m1.ref.getY();
    double imgDx = m2.img.getX() - m1.img.getX();
    double imgDy = m2.img.getY() - m1.img.getY();

    double refDist = Math.sqrt(refDx * refDx + refDy * refDy);
    double imgDist = Math.sqrt(imgDx * imgDx + imgDy * imgDy);

    if (refDist < 1.0 || imgDist < 1.0) return null;

    double scale = refDist / imgDist;
    double refAngle = Math.atan2(refDy, refDx);
    double imgAngle = Math.atan2(imgDy, imgDx);
    double rotation = refAngle - imgAngle;

    // Calculer la translation qui aligne les CENTRES après scale+rotation
    // Point image: (imgCenterX, imgCenterY)
    // Après scale+rotation: ?
    // Doit arriver à: (refCenterX, refCenterY)
    
    double cos = Math.cos(rotation);
    double sin = Math.sin(rotation);
    
    // Centre de l'image après scale+rotation
    double transformedCenterX = imgCenterX * scale * cos - imgCenterY * scale * sin;
    double transformedCenterY = imgCenterX * scale * sin + imgCenterY * scale * cos;
    
    // Translation pour aligner au centre de référence
    double tx = refCenterX - transformedCenterX;
    double ty = refCenterY - transformedCenterY;

    return new AffineTransform(scale, rotation, tx, ty);
}
```

### Correction 3 : Dans FitsImage.java - createAlignedCopy()

Si la transformation est par rapport aux centres, il faut l'appliquer correctement :

```java
public FitsImage createAlignedCopy(int canvasWidth, int canvasHeight, int offsetX, int offsetY) {
    FitsImage copy = FitsImage.createEmpty(this.path, canvasWidth, canvasHeight, this.isColor);

    // Centre de l'image source
    double srcCenterX = width / 2.0;
    double srcCenterY = height / 2.0;

    for (int y = 0; y < canvasHeight; y++) {
        for (int x = 0; x < canvasWidth; x++) {
            // Point dans le canvas
            double canvasX = x - offsetX;
            double canvasY = y - offsetY;

            // Appliquer transformation inverse
            double[] srcPoint = transform.applyInverse(canvasX, canvasY);
            double srcX = srcPoint[0];
            double srcY = srcPoint[1];

            // Interpoler
            float value = interpolate(srcX, srcY);
            copy.data[y][x] = value;
        }
    }

    return copy;
}
```

---

## 🚀 Solution Rapide : Version Simplifiée

Si les corrections ci-dessus sont trop complexes, voici une solution **simplifiée** qui devrait fonctionner :

### ImageAligner.java - MODIFICATION MINIMALE

Dans `computeAffineFromMatches()`, ajoutez un log pour voir les valeurs :

```java
// Juste avant le return
System.out.println("    Transformation calculée:");
System.out.println("      scale=" + scale + ", rotation=" + Math.toDegrees(rotation) + "°");
System.out.println("      tx=" + tx + ", ty=" + ty);

return new AffineTransform(scale, rotation, tx, ty);
```

### FitsImage.java - VERSION SIMPLIFIÉE

Remplacez `createAlignedCopy()` par cette version qui gère mieux les coordonnées :

```java
public FitsImage createAlignedCopy(int canvasWidth, int canvasHeight, int offsetX, int offsetY) {
    try {
        System.out.println("\n=== Alignement: " + getFileName() + " ===");
        System.out.println("  Source: " + width + "x" + height);
        System.out.println("  Canvas: " + canvasWidth + "x" + canvasHeight);
        System.out.println("  Offset: (" + offsetX + ", " + offsetY + ")");

        FitsImage copy = FitsImage.createEmpty(this.path, canvasWidth, canvasHeight, this.isColor);

        int validCount = 0;
        int outCount = 0;

        // Pour chaque pixel du canvas de destination
        for (int dstY = 0; dstY < canvasHeight; dstY++) {
            for (int dstX = 0; dstX < canvasWidth; dstX++) {
                
                // Convertir coordonnées canvas → coordonnées de travail
                // (enlever l'offset pour revenir dans le système de référence)
                double workX = dstX - offsetX;
                double workY = dstY - offsetY;

                // Appliquer transformation inverse pour trouver le point source
                double[] srcPoint = transform.applyInverse(workX, workY);
                double srcX = srcPoint[0];
                double srcY = srcPoint[1];

                // Vérifier si le point source est dans l'image
                if (srcX >= 0 && srcX < width - 1 && srcY >= 0 && srcY < height - 1) {
                    // Interpoler
                    float value = interpolate(srcX, srcY);
                    copy.data[dstY][dstX] = value;
                    validCount++;
                } else {
                    // Pixel noir (hors de l'image source)
                    copy.data[dstY][dstX] = 0;
                    outCount++;
                }
            }
        }

        double coverage = (validCount * 100.0) / (canvasWidth * canvasHeight);
        System.out.println("  Couverture: " + validCount + "/" + (canvasWidth*canvasHeight) + 
                          " (" + String.format("%.1f", coverage) + "%)");

        if (validCount == 0) {
            System.out.println("  ⚠️⚠️⚠️ AUCUN PIXEL VALIDE - PROBLÈME DE TRANSFORMATION!");
            
            // Test diagnostic
            double testX = canvasWidth / 2.0 - offsetX;
            double testY = canvasHeight / 2.0 - offsetY;
            double[] testSrc = transform.applyInverse(testX, testY);
            
            System.out.println("  Test centre canvas:");
            System.out.println("    Canvas: (" + (canvasWidth/2) + ", " + (canvasHeight/2) + ")");
            System.out.println("    Travail: (" + testX + ", " + testY + ")");
            System.out.println("    Source: (" + testSrc[0] + ", " + testSrc[1] + ")");
            System.out.println("    Limites source: [0-" + (width-1) + ", 0-" + (height-1) + "]");
        }

        return copy;
        
    } catch (Exception e) {
        System.err.println("ERREUR: " + e.getMessage());
        throw new RuntimeException(e);
    }
}
```

---

## 🎯 Plan d'Action

### Étape 1 : Ajouter les Logs

Dans `ImageAligner.java`, ligne ~460, ajoutez :

```java
System.out.println("    Transformation calculée:");
System.out.println("      scale=" + scale);
System.out.println("      rotation=" + Math.toDegrees(rotation) + "°");  
System.out.println("      tx=" + tx + ", ty=" + ty);
```

### Étape 2 : Remplacer createAlignedCopy()

Dans `FitsImage.java`, remplacez toute la méthode `createAlignedCopy()` par la version simplifiée ci-dessus.

### Étape 3 : Recompiler et Tester

```bash
mvn clean package
java -jar target/fits-stacker-1.0-SNAPSHOT.jar 2>&1 | tee test.log
```

### Étape 4 : Analyser les Logs

Cherchez dans `test.log` :

```
=== Alignement: M31_002.fits ===
  Source: 2048x2048
  Canvas: 2150x2150
  Offset: (50, 50)
  Couverture: 0/4622500 (0.0%)     ← Si 0%, il y a un problème
  
  Test centre canvas:
    Canvas: (1075, 1075)
    Travail: (1025, 1025)
    Source: (2500.5, -300.2)        ← Si HORS de [0-2047], PROBLÈME !
    Limites source: [0-2047, 0-2047]
```

---

## 🔍 Interprétation des Résultats

### Résultat A : Coordonnées Source Négatives

```
Source: (-500.2, -300.5)
```

**Diagnostic** : La transformation inverse produit des coordonnées négatives.  
**Cause** : Paramètres `tx, ty` incorrects OU centre de rotation mal géré.  
**Solution** : Modifier `computeAffineFromMatches()` pour calculer par rapport au centre de l'image.

### Résultat B : Coordonnées Source Trop Grandes

```
Source: (3500.2, 4200.5)
Limites: [0-2047, 0-2047]
```

**Diagnostic** : La transformation inverse "sort" de l'image source.  
**Cause** : Même problème que A.  
**Solution** : Même chose.

### Résultat C : Coordonnées Valides Mais Image Noire Quand Même

```
Source: (1200.5, 800.3)
Limites: [0-2047, 0-2047]
Couverture: 0/4622500 (0.0%)
```

**Diagnostic** : Bug dans `interpolate()` OU dans la condition `if`.  
**Cause** : Vérifier `interpolate()`.  
**Solution** : Ajouter des logs dans `interpolate()`.

---

## 💡 Si Rien Ne Marche : Solution de Contournement

Si vous n'arrivez pas à corriger le problème, voici un **hack temporaire** :

### Dans FitsImage.java - Version "Hack"

```java
public FitsImage createAlignedCopy(int canvasWidth, int canvasHeight, int offsetX, int offsetY) {
    // ⚠️ HACK TEMPORAIRE - Ne pas utiliser la transformation !
    
    FitsImage copy = FitsImage.createEmpty(this.path, canvasWidth, canvasHeight, this.isColor);
    
    // Copier directement l'image source au centre du canvas
    int startX = offsetX;
    int startY = offsetY;
    
    for (int y = 0; y < height && (startY + y) < canvasHeight; y++) {
        for (int x = 0; x < width && (startX + x) < canvasWidth; x++) {
            if (startX + x >= 0 && startY + y >= 0) {
                copy.data[startY + y][startX + x] = data[y][x];
            }
        }
    }
    
    System.out.println("  ⚠️ HACK: Image copiée sans transformation!");
    
    return copy;
}
```

**Résultat** : Les images ne seront **PAS** alignées, mais au moins elles seront **visibles** !

Cela vous permettra de :
1. Vérifier que le reste de l'application fonctionne
2. Voir à quoi ressemblent les images empilées (même mal alignées)
3. Déboguer le vrai problème séparément

---

**À FAIRE MAINTENANT** :

1. Installez la version avec logs
2. Testez avec 2-3 images
3. **ENVOYEZ-MOI les logs** de la section `=== Alignement: ===`
4. Je pourrai alors vous donner la correction exacte !

---

**Dernière mise à jour** : 2024  
**Statut** : Solution complète avec diagnostics
