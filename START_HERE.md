# ⚡ DÉMARRAGE RAPIDE - FITS Stacker v1.1

## 🎯 3 Améliorations en 1 Package

✨ **RGB** - Images couleur 3 canaux  
🖼️ **Canvas** - Aucune perte de données  
💾 **Sauvegarde** - Paramètres d'alignement

---

## 🚀 Installation en 3 Commandes

```bash
# 1. Sauvegarder
cp -r fits-stacker fits-stacker.backup

# 2. Copier les fichiers
cd fits-stacker/src/main/java/com/astro/
cp /mnt/user-data/outputs/{FitsImage,ImageAligner,StackingEngine,FitsStackerApp}.java .

# 3. Recompiler
cd ../../../../.. && mvn clean package
```

✅ **C'est tout !** Votre application est prête.

---

## 🧪 Test Rapide

```bash
# Lancer
java -Xmx4G -jar target/fits-stacker-1.0-SNAPSHOT.jar

# Vérifier
Dans l'interface :
✓ Nouveau bouton "Sauvegarder Alignées"
✓ Charger vos images FITS
✓ Aligner → Observer "Canvas élargi"
✓ Empiler ou Sauvegarder
```

---

## 📚 Documentation

**Débutant** → [README.md](README.md) - Liste de tout  
**Rapide** → [INDEX.md](INDEX.md) - Vue d'ensemble  
**Complet** → [RECAPITULATIF.md](RECAPITULATIF.md) - Tout savoir  
**Installation** → [INSTALLATION.md](INSTALLATION.md) - Pas à pas  
**Sauvegarde** → [DOC_SAUVEGARDE_ALIGNEES.md](DOC_SAUVEGARDE_ALIGNEES.md) - Guide complet

---

## ❓ Aide Rapide

**Erreur compilation** → Vérifier que les 4 .java sont copiés  
**OutOfMemory** → `java -Xmx8G -jar ...`  
**RGB non détecté** → Vérifier avec DS9 que c'est vraiment RGB  
**Canvas trop grand** → Normal avec rotations >30°

---

## 📦 Fichiers Copiés

- ✅ FitsImage.java (23 KB)
- ✅ ImageAligner.java (17 KB)
- ✅ StackingEngine.java (14 KB)
- ✅ FitsStackerApp.java (26 KB)

**Total : 80 KB de code**

---

## 🎉 Prêt !

Votre FITS Stacker supporte maintenant :
- Images RGB couleur
- Canvas sans perte
- Sauvegarde des alignées

**Bon empilement ! 🌟**
