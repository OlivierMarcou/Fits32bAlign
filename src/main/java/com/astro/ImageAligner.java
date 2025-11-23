package com.astro;

import java.util.*;

/**
 * VERSION ULTRA-DEBUG - Accepte TOUT et logue TOUT
 * Pour diagnostiquer pourquoi l'alignement échoue complètement
 */
public class ImageAligner {
    private static final int MIN_MATCHING_STARS = 5; // Réduit de 10 à 5
    private static final double MAX_DISTANCE_TOLERANCE = 10.0; // Augmenté de 5 à 10
    private static final int RANSAC_ITERATIONS = 1000; // Augmenté de 500 à 1000
    private static final double RANSAC_THRESHOLD = 5.0; // Augmenté de 3 à 5

    // ⚠️ DÉSACTIVÉ COMPLÈTEMENT - On accepte TOUT
    private static final boolean ENABLE_QUALITY_FILTERING = false;
    private static final double MIN_QUALITY_SCORE = 0.01; // 1% seulement
    private static final int MIN_ABSOLUTE_INLIERS = 3; // 3 au lieu de 8

    public static class CanvasInfo {
        public final int width;
        public final int height;
        public final int offsetX;
        public final int offsetY;

        public CanvasInfo(int width, int height, int offsetX, int offsetY) {
            this.width = width;
            this.height = height;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
    }

    public static class AlignmentResult {
        public final AffineTransform transform;
        public final double qualityScore;
        public final int inliers;
        public final int totalMatches;
        public final boolean accepted;
        public final String rejectReason;

        public AlignmentResult(AffineTransform transform, int inliers, int totalMatches) {
            this.transform = transform;
            this.inliers = inliers;
            this.totalMatches = totalMatches;
            this.qualityScore = totalMatches > 0 ? (double) inliers / totalMatches : 0;

            // ACCEPTER TOUT si filtrage désactivé
            if (!ENABLE_QUALITY_FILTERING) {
                this.accepted = true;
                this.rejectReason = null;
            } else if (inliers < MIN_ABSOLUTE_INLIERS) {
                this.accepted = false;
                this.rejectReason = String.format("Trop peu d'inliers (%d < %d)", inliers, MIN_ABSOLUTE_INLIERS);
            } else if (qualityScore < MIN_QUALITY_SCORE) {
                this.accepted = false;
                this.rejectReason = String.format("Score trop faible (%.1f%%)", qualityScore * 100);
            } else {
                this.accepted = true;
                this.rejectReason = null;
            }
        }
    }

    public static void alignImages(List<FitsImage> images, ProgressCallback callback) {
        if (images.isEmpty()) return;

        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔍 ULTRA-DEBUG MODE - ALIGNEMENT");
        System.out.println("=".repeat(80));
        System.out.println("Nombre d'images: " + images.size());
        System.out.println("Filtrage de qualité: " + (ENABLE_QUALITY_FILTERING ? "ACTIVÉ" : "DÉSACTIVÉ"));
        System.out.println("Seuils:");
        System.out.println("  - Étoiles min pour match: " + MIN_MATCHING_STARS);
        System.out.println("  - Tolérance distance: " + MAX_DISTANCE_TOLERANCE);
        System.out.println("  - Itérations RANSAC: " + RANSAC_ITERATIONS);
        System.out.println("  - Seuil RANSAC: " + RANSAC_THRESHOLD);
        System.out.println();

        if (callback != null) {
            callback.onProgress(0, "Détection des étoiles...");
        }

        // Detect stars
        List<List<Star>> allStars = new ArrayList<>();
        System.out.println("📍 DÉTECTION DES ÉTOILES");
        System.out.println("-".repeat(80));

        for (int i = 0; i < images.size(); i++) {
            FitsImage image = images.get(i);
            System.out.println("\nImage " + (i+1) + ": " + image.getFileName());

            List<Star> stars = StarDetector.detectStars(image, 100);
            allStars.add(stars);

            System.out.println("  ✓ Étoiles détectées: " + stars.size());

            if (stars.size() < 10) {
                System.out.println("  ⚠️ ATTENTION: Très peu d'étoiles! (<10)");
            } else if (stars.size() < MIN_MATCHING_STARS) {
                System.out.println("  ⚠️ ATTENTION: Moins que le minimum requis! (<" + MIN_MATCHING_STARS + ")");
            }

            // Afficher les 5 étoiles les plus brillantes
            if (!stars.isEmpty()) {
                System.out.println("  Top 5 étoiles:");
                for (int j = 0; j < Math.min(5, stars.size()); j++) {
                    Star s = stars.get(j);
                    System.out.println(String.format("    %d. Position (%.1f, %.1f), Flux: %.1f",
                            j+1, s.getX(), s.getY(), s.getFlux()));
                }
            }

            if (callback != null) {
                int progress = (int) ((i * 30.0) / images.size());
                callback.onProgress(progress, "Détection: " + image.getFileName() + " (" + stars.size() + " étoiles)");
            }
        }

        // Find reference image
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎯 SÉLECTION IMAGE DE RÉFÉRENCE");
        System.out.println("-".repeat(80));

        int refIndex = findReferenceImage(images, allStars);
        FitsImage reference = images.get(refIndex);
        List<Star> referenceStars = allStars.get(refIndex);

        System.out.println("Image de référence: #" + (refIndex+1) + " - " + reference.getFileName());
        System.out.println("  Étoiles: " + referenceStars.size());
        System.out.println("  Dimensions: " + reference.getWidth() + "x" + reference.getHeight());

        if (callback != null) {
            callback.onProgress(30, "Référence: " + reference.getFileName());
        }

        if (referenceStars.size() < MIN_MATCHING_STARS) {
            System.out.println("  ⚠️⚠️⚠️ ATTENTION CRITIQUE: Pas assez d'étoiles dans la référence!");
        }

        // Align each image
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔄 ALIGNEMENT DES IMAGES");
        System.out.println("=".repeat(80));

        List<FitsImage> acceptedImages = new ArrayList<>();
        List<String> rejectedImages = new ArrayList<>();

        for (int i = 0; i < images.size(); i++) {
            System.out.println("\n" + "-".repeat(80));
            System.out.println("Image " + (i+1) + "/" + images.size() + ": " + images.get(i).getFileName());
            System.out.println("-".repeat(80));

            if (i == refIndex) {
                images.get(i).setTransform(AffineTransform.identity());
                acceptedImages.add(images.get(i));
                System.out.println("→ IMAGE DE RÉFÉRENCE (transformation identité)");
                continue;
            }

            FitsImage image = images.get(i);
            List<Star> imageStars = allStars.get(i);

            System.out.println("Étoiles dans cette image: " + imageStars.size());
            System.out.println("Étoiles dans référence: " + referenceStars.size());

            if (callback != null) {
                int progress = 30 + (int) ((i * 40.0) / images.size());
                callback.onProgress(progress, "Alignement: " + image.getFileName());
            }

            // Find transformation
            System.out.println("\n🔍 Recherche de la transformation...");
            AlignmentResult result = findAffineTransformWithQuality(referenceStars, imageStars);

            System.out.println("\n📊 RÉSULTAT:");
            System.out.println("  Correspondances trouvées: " + result.totalMatches);
            System.out.println("  Inliers RANSAC: " + result.inliers);
            System.out.println("  Score de qualité: " + String.format("%.1f%%", result.qualityScore * 100));
            System.out.println("\n  Transformation calculée:");
            System.out.println("    Rotation: " + String.format("%.3f°", Math.toDegrees(result.transform.rotation)));
            System.out.println("    Échelle: " + String.format("%.4f (%.2f%%)", result.transform.scale, result.transform.scale * 100));
            System.out.println("    Translation X: " + String.format("%.2f pixels", result.transform.tx));
            System.out.println("    Translation Y: " + String.format("%.2f pixels", result.transform.ty));

            if (result.accepted) {
                image.setTransform(result.transform);
                acceptedImages.add(image);
                System.out.println("\n  ✅ ACCEPTÉE");
            } else {
                rejectedImages.add(image.getFileName() + " - " + result.rejectReason);
                System.out.println("\n  ❌ REJETÉE: " + result.rejectReason);
            }

            if (callback != null) {
                String status = result.accepted ? "✓" : "✗";
                callback.onProgress(0, String.format("%s %s - Qualité: %.1f%%",
                        status, image.getFileName(), result.qualityScore * 100));
            }
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("📈 RÉSUMÉ DE L'ALIGNEMENT");
        System.out.println("=".repeat(80));
        System.out.println("Total images: " + images.size());
        System.out.println("Images acceptées: " + acceptedImages.size());
        System.out.println("Images rejetées: " + rejectedImages.size());

        if (!rejectedImages.isEmpty()) {
            System.out.println("\n❌ Images rejetées:");
            for (String rejected : rejectedImages) {
                System.out.println("  - " + rejected);
            }
        }

        // NE PAS MODIFIER LA LISTE si filtrage désactivé
        if (ENABLE_QUALITY_FILTERING && !rejectedImages.isEmpty()) {
            images.clear();
            images.addAll(acceptedImages);
            System.out.println("\n⚠️ Liste modifiée: " + images.size() + " images conservées");
        } else {
            System.out.println("\n✓ Toutes les " + images.size() + " images conservées (filtrage désactivé)");
        }

        if (images.isEmpty()) {
            System.out.println("\n❌❌❌ ERREUR CRITIQUE: Aucune image n'a pu être alignée!");
            if (callback != null) {
                callback.onProgress(0, "ERREUR: Aucune image alignée!");
            }
            return;
        }

        // Calculate canvas
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📐 CALCUL DU CANVAS");
        System.out.println("=".repeat(80));

        if (callback != null) {
            callback.onProgress(75, "Calcul du canvas...");
        }

        CanvasInfo canvasInfo = calculateExpandedCanvas(images);

        System.out.println("Canvas élargi: " + canvasInfo.width + "x" + canvasInfo.height);
        System.out.println("Offset global: (" + canvasInfo.offsetX + ", " + canvasInfo.offsetY + ")");

        int originalSize = images.get(0).getWidth() * images.get(0).getHeight();
        int canvasSize = canvasInfo.width * canvasInfo.height;
        double expansion = ((canvasSize - originalSize) * 100.0) / originalSize;
        System.out.println("Expansion: " + String.format("%.1f%%", expansion));

        // Store canvas info
        for (FitsImage image : images) {
            image.setCanvasInfo(canvasInfo);
        }

        if (callback != null) {
            callback.onProgress(100, "Alignement terminé: " + images.size() + " images prêtes");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("✅ ALIGNEMENT TERMINÉ");
        System.out.println("=".repeat(80));
        System.out.println();
    }

    private static CanvasInfo calculateExpandedCanvas(List<FitsImage> images) {
        if (images.isEmpty()) {
            return new CanvasInfo(0, 0, 0, 0);
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        System.out.println("Calcul des limites pour " + images.size() + " images:");

        for (int i = 0; i < images.size(); i++) {
            FitsImage image = images.get(i);
            int w = image.getWidth();
            int h = image.getHeight();
            AffineTransform transform = image.getTransform();

            double[][] corners = {
                    {0, 0}, {w, 0}, {w, h}, {0, h}
            };

            for (double[] corner : corners) {
                double[] transformed = transform.apply(corner[0], corner[1]);
                minX = Math.min(minX, transformed[0]);
                minY = Math.min(minY, transformed[1]);
                maxX = Math.max(maxX, transformed[0]);
                maxY = Math.max(maxY, transformed[1]);
            }

            System.out.println(String.format("  Image %d: X [%.1f, %.1f], Y [%.1f, %.1f]",
                    i+1, minX, maxX, minY, maxY));
        }

        int canvasWidth = (int) Math.ceil(maxX - minX);
        int canvasHeight = (int) Math.ceil(maxY - minY);
        int offsetX = (int) Math.floor(-minX);
        int offsetY = (int) Math.floor(-minY);

        return new CanvasInfo(canvasWidth, canvasHeight, offsetX, offsetY);
    }

    private static int findReferenceImage(List<FitsImage> images, List<List<Star>> allStars) {
        int bestIndex = 0;
        double bestScore = 0;

        System.out.println("Évaluation des images pour la référence:");

        for (int i = 0; i < images.size(); i++) {
            List<Star> stars = allStars.get(i);
            if (stars.size() < 10) {
                System.out.println("  Image " + (i+1) + ": " + stars.size() + " étoiles (trop peu)");
                continue;
            }

            double avgDistance = calculateAverageStarDistance(stars);
            double score = stars.size() / (avgDistance + 1.0);

            System.out.println(String.format("  Image %d: %d étoiles, dist moy=%.1f, score=%.3f",
                    i+1, stars.size(), avgDistance, score));

            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }

        System.out.println("→ Meilleure image: #" + (bestIndex+1) + " (score: " + String.format("%.3f", bestScore) + ")");
        return bestIndex;
    }

    private static double calculateAverageStarDistance(List<Star> stars) {
        if (stars.size() < 10) return Double.MAX_VALUE;

        int numPairs = Math.min(30, stars.size() / 2);
        double sumDistances = 0;
        int count = 0;

        for (int i = 0; i < numPairs && i < stars.size(); i++) {
            for (int j = i + 1; j < numPairs && j < stars.size(); j++) {
                sumDistances += stars.get(i).distanceTo(stars.get(j));
                count++;
            }
        }

        return count > 0 ? sumDistances / count : Double.MAX_VALUE;
    }

    private static AlignmentResult findAffineTransformWithQuality(List<Star> referenceStars, List<Star> imageStars) {
        System.out.println("  Recherche de correspondances de triangles...");

        List<StarMatch> matches = findStarMatches(referenceStars, imageStars);

        System.out.println("  → Correspondances brutes: " + matches.size());

        if (matches.size() < 3) {
            System.out.println("  ❌ ÉCHEC: Pas assez de correspondances (<3)");
            return new AlignmentResult(AffineTransform.identity(), 0, matches.size());
        }

        System.out.println("  Lancement RANSAC (" + RANSAC_ITERATIONS + " itérations)...");

        AffineTransform bestTransform = null;
        int bestInliers = 0;
        Random random = new Random(42);

        for (int iter = 0; iter < RANSAC_ITERATIONS; iter++) {
            if (matches.size() < 3) break;

            List<StarMatch> sample = new ArrayList<>();
            Set<Integer> used = new HashSet<>();

            while (sample.size() < 3 && used.size() < matches.size()) {
                int idx = random.nextInt(matches.size());
                if (!used.contains(idx)) {
                    sample.add(matches.get(idx));
                    used.add(idx);
                }
            }

            if (sample.size() < 3) continue;

            AffineTransform transform = computeAffineFromMatches(sample);
            if (transform == null) continue;

            int inliers = countInliers(matches, transform);

            if (inliers > bestInliers) {
                bestInliers = inliers;
                bestTransform = transform;
            }
        }

        if (bestTransform == null) {
            System.out.println("  ❌ ÉCHEC: RANSAC n'a pas convergé");
            return new AlignmentResult(AffineTransform.identity(), 0, matches.size());
        }

        System.out.println("  ✓ RANSAC terminé: " + bestInliers + " inliers sur " + matches.size() + " correspondances");

        return new AlignmentResult(bestTransform, bestInliers, matches.size());
    }

    private static List<StarMatch> findStarMatches(List<Star> referenceStars, List<Star> imageStars) {
        List<StarMatch> matches = new ArrayList<>();

        List<StarTriangle> refTriangles = createTriangles(referenceStars);
        List<StarTriangle> imgTriangles = createTriangles(imageStars);

        System.out.println("    Triangles référence: " + refTriangles.size());
        System.out.println("    Triangles image: " + imgTriangles.size());

        int matchCount = 0;
        for (StarTriangle refTri : refTriangles) {
            for (StarTriangle imgTri : imgTriangles) {
                if (trianglesMatch(refTri, imgTri)) {
                    matches.add(new StarMatch(refTri.s1, imgTri.s1));
                    matches.add(new StarMatch(refTri.s2, imgTri.s2));
                    matches.add(new StarMatch(refTri.s3, imgTri.s3));
                    matchCount++;
                }
            }
        }

        System.out.println("    Triangles correspondants: " + matchCount);

        // Remove duplicates
        Set<String> seen = new HashSet<>();
        List<StarMatch> uniqueMatches = new ArrayList<>();

        for (StarMatch match : matches) {
            String key = String.format("%.1f,%.1f-%.1f,%.1f",
                    match.ref.getX(), match.ref.getY(),
                    match.img.getX(), match.img.getY());
            if (!seen.contains(key)) {
                seen.add(key);
                uniqueMatches.add(match);
            }
        }

        System.out.println("    Correspondances uniques: " + uniqueMatches.size());

        return uniqueMatches;
    }

    private static List<StarTriangle> createTriangles(List<Star> stars) {
        List<StarTriangle> triangles = new ArrayList<>();
        int maxStars = Math.min(20, stars.size());

        for (int i = 0; i < maxStars - 2; i++) {
            for (int j = i + 1; j < maxStars - 1; j++) {
                for (int k = j + 1; k < maxStars; k++) {
                    triangles.add(new StarTriangle(stars.get(i), stars.get(j), stars.get(k)));
                }
            }
        }

        return triangles;
    }

    private static boolean trianglesMatch(StarTriangle t1, StarTriangle t2) {
        double[] sides1 = t1.getSortedSides();
        double[] sides2 = t2.getSortedSides();

        double ratio1_1 = sides1[1] / sides1[0];
        double ratio1_2 = sides1[2] / sides1[0];
        double ratio2_1 = sides2[1] / sides2[0];
        double ratio2_2 = sides2[2] / sides2[0];

        double tolerance = 0.20; // Augmenté de 0.15 à 0.20

        return Math.abs(ratio1_1 - ratio2_1) < tolerance &&
                Math.abs(ratio1_2 - ratio2_2) < tolerance;
    }

    private static AffineTransform computeAffineFromMatches(List<StarMatch> matches) {
        if (matches.size() < 3) return null;

        StarMatch m1 = matches.get(0);
        StarMatch m2 = matches.get(1);
        StarMatch m3 = matches.get(2);

        double refCx = (m1.ref.getX() + m2.ref.getX() + m3.ref.getX()) / 3.0;
        double refCy = (m1.ref.getY() + m2.ref.getY() + m3.ref.getY()) / 3.0;
        double imgCx = (m1.img.getX() + m2.img.getX() + m3.img.getX()) / 3.0;
        double imgCy = (m1.img.getY() + m2.img.getY() + m3.img.getY()) / 3.0;

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

        double tx = refCx - imgCx * scale * Math.cos(rotation) + imgCy * scale * Math.sin(rotation);
        double ty = refCy - imgCx * scale * Math.sin(rotation) - imgCy * scale * Math.cos(rotation);

        return new AffineTransform(scale, rotation, tx, ty);
    }

    private static int countInliers(List<StarMatch> matches, AffineTransform transform) {
        int inliers = 0;

        for (StarMatch match : matches) {
            double[] transformed = transform.apply(match.img.getX(), match.img.getY());
            double dx = transformed[0] - match.ref.getX();
            double dy = transformed[1] - match.ref.getY();
            double error = Math.sqrt(dx * dx + dy * dy);

            if (error < RANSAC_THRESHOLD) {
                inliers++;
            }
        }

        return inliers;
    }

    private static class StarTriangle {
        final Star s1, s2, s3;
        final double side1, side2, side3;

        StarTriangle(Star s1, Star s2, Star s3) {
            this.s1 = s1;
            this.s2 = s2;
            this.s3 = s3;
            this.side1 = s1.distanceTo(s2);
            this.side2 = s2.distanceTo(s3);
            this.side3 = s3.distanceTo(s1);
        }

        double[] getSortedSides() {
            double[] sides = {side1, side2, side3};
            Arrays.sort(sides);
            return sides;
        }
    }

    private static class StarMatch {
        final Star ref;
        final Star img;

        StarMatch(Star ref, Star img) {
            this.ref = ref;
            this.img = img;
        }
    }

    public static class AffineTransform {
        public final double scale;
        public final double rotation;
        public final double tx, ty;

        public AffineTransform(double scale, double rotation, double tx, double ty) {
            this.scale = scale;
            this.rotation = rotation;
            this.tx = tx;
            this.ty = ty;
        }

        public static AffineTransform identity() {
            return new AffineTransform(1.0, 0.0, 0.0, 0.0);
        }

        public double[] apply(double x, double y) {
            double cos = Math.cos(rotation);
            double sin = Math.sin(rotation);

            double newX = scale * (x * cos - y * sin) + tx;
            double newY = scale * (x * sin + y * cos) + ty;

            return new double[]{newX, newY};
        }

        public double[] applyInverse(double x, double y) {
            double cos = Math.cos(-rotation);
            double sin = Math.sin(-rotation);

            double dx = x - tx;
            double dy = y - ty;

            double newX = (dx * cos - dy * sin) / scale;
            double newY = (dx * sin + dy * cos) / scale;

            return new double[]{newX, newY};
        }
    }

    public interface ProgressCallback {
        void onProgress(int percent, String message);
    }
}