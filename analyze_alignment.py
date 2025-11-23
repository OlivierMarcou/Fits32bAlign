#!/usr/bin/env python3
"""
Script de diagnostic pour FITS Stacker
Analyse le fichier alignment_params.txt pour identifier les problèmes
"""

import re
import sys
from pathlib import Path

def parse_alignment_params(filepath):
    """Parse le fichier alignment_params.txt"""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Extraire les infos globales
    canvas_match = re.search(r'Canvas élargi: (\d+)x(\d+)', content)
    canvas_w = int(canvas_match.group(1)) if canvas_match else 0
    canvas_h = int(canvas_match.group(2)) if canvas_match else 0
    
    # Extraire les infos par image
    images = []
    pattern = r'Image (\d+): (.+?)\n\s+Décalage: dx=([-\d.]+), dy=([-\d.]+) pixels\n\s+Rotation: ([-\d.]+) degrés\n\s+Échelle: ([\d.]+)'
    
    for match in re.finditer(pattern, content):
        images.append({
            'number': int(match.group(1)),
            'filename': match.group(2),
            'dx': float(match.group(3)),
            'dy': float(match.group(4)),
            'rotation': float(match.group(5)),
            'scale': float(match.group(6))
        })
    
    return {
        'canvas_width': canvas_w,
        'canvas_height': canvas_h,
        'images': images
    }

def analyze_alignment(data):
    """Analyse les données et identifie les problèmes"""
    images = data['images']
    
    if not images:
        print("❌ Aucune image trouvée dans le fichier!")
        return
    
    print("=" * 70)
    print("📊 ANALYSE DES PARAMÈTRES D'ALIGNEMENT")
    print("=" * 70)
    print()
    
    # Statistiques globales
    print(f"Canvas: {data['canvas_width']}x{data['canvas_height']} pixels")
    print(f"Nombre d'images: {len(images)}")
    print()
    
    # Analyser chaque métrique
    rotations = [img['rotation'] for img in images]
    scales = [img['scale'] for img in images]
    dx_values = [img['dx'] for img in images]
    dy_values = [img['dy'] for img in images]
    
    print("📐 ROTATION")
    print(f"  Min: {min(rotations):.2f}°")
    print(f"  Max: {max(rotations):.2f}°")
    print(f"  Moyenne: {sum(rotations)/len(rotations):.2f}°")
    print(f"  Écart max: {max(rotations) - min(rotations):.2f}°")
    
    # Alertes rotation
    if max(abs(r) for r in rotations) > 20:
        print("  ⚠️  ATTENTION: Rotation > 20° détectée!")
    if max(rotations) - min(rotations) > 30:
        print("  ⚠️  ATTENTION: Écart de rotation > 30°!")
    print()
    
    print("🔍 ÉCHELLE")
    print(f"  Min: {min(scales):.4f} ({min(scales)*100:.2f}%)")
    print(f"  Max: {max(scales):.4f} ({max(scales)*100:.2f}%)")
    print(f"  Moyenne: {sum(scales)/len(scales):.4f}")
    print(f"  Écart: {(max(scales) - min(scales))*100:.2f}%")
    
    # Alertes échelle
    if max(scales) > 1.10 or min(scales) < 0.90:
        print("  ⚠️  ATTENTION: Variation d'échelle > 10%!")
    if max(scales) - min(scales) > 0.10:
        print("  ⚠️  ATTENTION: Écart d'échelle important!")
    print()
    
    print("📍 DÉCALAGES")
    print(f"  X: min={min(dx_values):.1f}, max={max(dx_values):.1f}")
    print(f"  Y: min={min(dy_values):.1f}, max={max(dy_values):.1f}")
    print(f"  Distance max: {max((dx**2 + dy**2)**0.5 for dx, dy in zip(dx_values, dy_values)):.1f} pixels")
    
    # Alertes décalage
    max_offset = max((dx**2 + dy**2)**0.5 for dx, dy in zip(dx_values, dy_values))
    if max_offset > 200:
        print(f"  ⚠️  ATTENTION: Décalage > 200 pixels détecté!")
    print()
    
    # Images problématiques
    print("=" * 70)
    print("🔍 IMAGES À VÉRIFIER")
    print("=" * 70)
    print()
    
    problems = []
    
    for img in images:
        issues = []
        
        # Critères de rejet
        if abs(img['rotation']) > 20:
            issues.append(f"rotation extrême ({img['rotation']:.1f}°)")
        
        if img['scale'] > 1.10 or img['scale'] < 0.90:
            issues.append(f"échelle anormale ({img['scale']:.4f})")
        
        offset = (img['dx']**2 + img['dy']**2)**0.5
        if offset > 200:
            issues.append(f"décalage important ({offset:.1f} px)")
        
        if issues:
            problems.append({
                'image': img,
                'issues': issues
            })
    
    if problems:
        print(f"⚠️  {len(problems)} image(s) potentiellement problématique(s):")
        print()
        for prob in problems:
            img = prob['image']
            print(f"  Image {img['number']}: {img['filename']}")
            for issue in prob['issues']:
                print(f"    - {issue}")
            print()
    else:
        print("✅ Toutes les images semblent avoir des paramètres normaux")
        print()
    
    # Recommandations
    print("=" * 70)
    print("💡 RECOMMANDATIONS")
    print("=" * 70)
    print()
    
    if problems:
        print("1. Inspectez visuellement les images problématiques dans DS9")
        print("2. Considérez de les exclure et réempiler sans elles")
        print("3. Si trop d'images sont problématiques, vérifiez:")
        print("   - Même objet dans toutes les images?")
        print("   - Même focale?")
        print("   - Mise au point correcte?")
    else:
        print("Vos paramètres d'alignement sont bons!")
        print("Vous pouvez procéder à l'empilement en toute confiance.")
    print()
    
    # Commandes suggérées
    if problems:
        print("=" * 70)
        print("🔧 COMMANDES SUGGÉRÉES")
        print("=" * 70)
        print()
        print("Pour visualiser les images problématiques dans DS9:")
        print()
        for prob in problems:
            img = prob['image']
            filename = img['filename'].replace('.fits', '')
            print(f"  ds9 aligned_*{filename}*.fits")
        print()

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 analyze_alignment.py alignment_params.txt")
        print()
        print("Ce script analyse le fichier de paramètres d'alignement")
        print("et identifie les images potentiellement problématiques.")
        sys.exit(1)
    
    filepath = Path(sys.argv[1])
    
    if not filepath.exists():
        print(f"❌ Fichier introuvable: {filepath}")
        sys.exit(1)
    
    try:
        data = parse_alignment_params(filepath)
        analyze_alignment(data)
    except Exception as e:
        print(f"❌ Erreur lors de l'analyse: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == '__main__':
    main()
