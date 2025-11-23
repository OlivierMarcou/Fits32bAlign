package com.astro;

import java.util.*;

/**
 * Alignement avancé pour astrophotographie ciel profond
 * - Gère rotation, échelle et translation
 * - Calcule un canvas élargi pour ne rien rogner
 * - DÉTECTE et REJETTE les images mal alignées
 * - Score de qualité pour chaque alignement
 */
public class ImageAligner {
    private static final int MIN_MATCHING_STARS = 10;
    private static final double MAX_DISTANCE_TOLERANCE = 5.0;
    private static final int RANSAC_ITERATIONS = 500;
    private static final double RANSAC_THRESHOLD = 3.0;

    // Seuils de qualité pour accepter un alignement
    private static final double MIN_QUALITY_SCORE = 0.20; // 20% d'inliers minimum
    private static final int MIN_ABSOLUTE_INLIERS = 8; // Au moins 8 étoiles qui correspondent

    // Classe pour stocker les informations du canvas élargi
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

    // Classe pour stocker les résultats d'alignement avec score de qualité
    public static class AlignmentResult {
        public final AffineTransform transform;
        public final double qualityScore; // Entre 0 et 1
        public final int inliers;
        public final int totalMatches;
        public final boolean accepted;
        public final String rejectReason;

        public AlignmentResult(AffineTransform transform, int inliers, int totalMatches) {
            this.transform = transform;
            this.inliers = inliers;
            this.totalMatches = totalMatches;
            this.qualityScore = totalMatches > 0 ? (double) inliers / totalMatches : 0;

            // Déterminer si l'alignement est acceptable
            if (inliers < MIN_ABSOLUTE_INLIERS) {
                this.accepted = false;
                this.rejectReason = String.format("Trop peu d'étoiles correspondantes (%d < %d)",
                        inliers, MIN_ABSOLUTE_INLIERS);
            } else if (qualityScore < MIN_QUALITY_SCORE) {
                this.accepted = false;
                this.rejectReason = String.format("Score de qualité trop faible (%.1f%% < %.1f%%)",
                        qualityScore * 100, MIN_QUALITY_SCORE * 100);
            } else {
                this.accepted = true;
                this.rejectReason = null;
            }
        }
    }

    public static void alignImages(List<FitsImage> images, ProgressCallback callback) {
        if (images.isEmpty()) return;

        // Detect stars in all images first
        if (callback != null) {
            callback.onProgress(0, "Détection des étoiles dans toutes les images...");
        }

        List<List<Star>> allStars = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            FitsImage image = images.get(i);
            List<Star> stars = StarDetector.detectStars(image, 100);
            allStars.add(stars);

            if (callback != null) {
                int progress = (int) ((i * 30.0) / images.size());
                callback.onProgress(progress, "Détection étoiles: " + image.getFileName());
            }
        }

        // Find reference image (largest scale = biggest field coverage)
        int refIndex = findReferenceImage(images, allStars);
        FitsImage reference = images.get(refIndex);
        List<Star> referenceStars = allStars.get(refIndex);

        if (callback != null) {
            callback.onProgress(30, "Image de référence: " + reference.getFileName() +
                    " (plus grande échelle détectée)");
        }

        if (referenceStars.size() < MIN_MATCHING_STARS) {
            if (callback != null) {
                callback.onProgress(30, "⚠️ Attention: peu d'étoiles détectées dans l'image de référence");
            }
        }

        // Align each image to reference and check quality
        List<FitsImage> acceptedImages = new ArrayList<>();
        List<String> rejectedImages = new ArrayList<>();

        for (int i = 0; i < images.size(); i++) {
            if (i == refIndex) {
                // Reference image has identity transform
                images.get(i).setTransform(AffineTransform.identity());
                acceptedImages.add(images.get(i));
                continue;
            }

            FitsImage image = images.get(i);
            List<Star> imageStars = allStars.get(i);

            if (callback != null) {
                int progress = 30 + (int) ((i * 40.0) / images.size());
                callback.onProgress(progress, "Alignement de " + image.getFileName() + "...");
            }

            // Find affine transformation with quality check
            AlignmentResult result = findAffineTransformWithQuality(referenceStars, imageStars);

            if (result.accepted) {
                image.setTransform(result.transform);
                acceptedImages.add(image);

                if (callback != null) {
                    callback.onProgress(0, String.format(
                            "✓ %s - Qualité: %.1f%% (%d/%d étoiles) - Rot: %.2f°, Échelle: %.2f%%",
                            image.getFileName(),
                            result.qualityScore * 100,
                            result.inliers,
                            result.totalMatches,
                            Math.toDegrees(result.transform.rotation),
                            result.transform.scale * 100
                    ));
                }
            } else {
                rejectedImages.add(image.getFileName() + " - " + result.rejectReason);

                if (callback != null) {
                    callback.onProgress(0, String.format(
                            "✗ REJETÉE: %s - %s",
                            image.getFileName(),
                            result.rejectReason
                    ));
                }
            }
        }

        // Remplacer la liste par seulement les images acceptées
        images.clear();
        images.addAll(acceptedImages);

        if (callback != null) {
            callback.onProgress(70, String.format(
                    "Alignement terminé: %d images acceptées, %d rejetées",
                    acceptedImages.size(),
                    rejectedImages.size()
            ));

            if (!rejectedImages.isEmpty()) {
                callback.onProgress(0, "Images rejetées:");
                for (String rejected : rejectedImages) {
                    callback.onProgress(0, "  - " + rejected);
                }
            }
        }

        if (images.isEmpty()) {
            if (callback != null) {
                callback.onProgress(0, "✗ ERREUR: Aucune image n'a pu être alignée correctement!");
            }
            return;
        }

        // Calculer le canvas élargi nécessaire (seulement pour les images acceptées)
        if (callback != null) {
            callback.onProgress(75, "Calcul du canvas élargi...");
        }

        CanvasInfo canvasInfo = calculateExpandedCanvas(images);

        if (callback != null) {
            callback.onProgress(80, String.format(
                    "Canvas élargi: %dx%d pixels (offset: %d, %d)",
                    canvasInfo.width, canvasInfo.height,
                    canvasInfo.offsetX, canvasInfo.offsetY
            ));
        }

        // Stocker les informations du canvas dans chaque image
        for (FitsImage image : images) {
            image.setCanvasInfo(canvasInfo);
        }

        if (callback != null) {
            callback.onProgress(100, String.format(
                    "Alignement terminé! %d images prêtes pour l'empilement",
                    images.size()
            ));
        }
    }

    /**
     * Calcule le canvas élargi nécessaire pour contenir toutes les images alignées
     */
    private static CanvasInfo calculateExpandedCanvas(List<FitsImage> images) {
        if (images.isEmpty()) {
            return new CanvasInfo(0, 0, 0, 0);
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        // Pour chaque image, calculer les coins transformés
        for (FitsImage image : images) {
            int w = image.getWidth();
            int h = image.getHeight();
            AffineTransform transform = image.getTransform();

            // Les 4 coins de l'image
            double[][] corners = {
                    {0, 0},
                    {w, 0},
                    {w, h},
                    {0, h}
            };

            // Transformer chaque coin
            for (double[] corner : corners) {
                double[] transformed = transform.apply(corner[0], corner[1]);
                double tx = transformed[0];
                double ty = transformed[1];

                minX = Math.min(minX, tx);
                minY = Math.min(minY, ty);
                maxX = Math.max(maxX, tx);
                maxY = Math.max(maxY, ty);
            }
        }

        // Calculer les dimensions du canvas
        int canvasWidth = (int) Math.ceil(maxX - minX);
        int canvasHeight = (int) Math.ceil(maxY - minY);

        // Offset pour placer toutes les images dans le canvas
        int offsetX = (int) Math.floor(-minX);
        int offsetY = (int) Math.floor(-minY);

        System.out.println(String.format(
                "Canvas calculé: %dx%d (offset: %d, %d) - Expansion: %.1f%%",
                canvasWidth, canvasHeight, offsetX, offsetY,
                ((canvasWidth * canvasHeight) / (double)(images.get(0).getWidth() * images.get(0).getHeight()) - 1) * 100
        ));

        return new CanvasInfo(canvasWidth, canvasHeight, offsetX, offsetY);
    }

    /**
     * Trouve l'image avec la plus grande échelle (sujet le plus grand)
     * basé sur la densité et la distribution des étoiles
     */
    private static int findReferenceImage(List<FitsImage> images, List<List<Star>> allStars) {
        int bestIndex = 0;
        double bestScore = 0;

        for (int i = 0; i < images.size(); i++) {
            List<Star> stars = allStars.get(i);
            if (stars.size() < 10) continue;

            // Calculate average distance between brightest stars
            // Smaller average distance = larger scale (more zoomed in)
            double avgDistance = calculateAverageStarDistance(stars);

            // Score based on number of stars and average distance
            // We want: many stars AND small distances (= good detail, large scale)
            double score = stars.size() / (avgDistance + 1.0);

            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }

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

    /**
     * Trouve la transformation affine entre deux ensembles d'étoiles
     * en utilisant RANSAC pour robustesse
     * RETOURNE aussi un score de qualité
     */
    private static AlignmentResult findAffineTransformWithQuality(List<Star> referenceStars, List<Star> imageStars) {
        // Find potential star matches using triangle matching
        List<StarMatch> matches = findStarMatches(referenceStars, imageStars);

        if (matches.size() < 3) {
            System.out.println("⚠️ Pas assez de correspondances d'étoiles trouvées: " + matches.size());
            return new AlignmentResult(AffineTransform.identity(), 0, matches.size());
        }

        // Use RANSAC to find best transformation
        AffineTransform bestTransform = null;
        int bestInliers = 0;

        Random random = new Random(42);

        for (int iter = 0; iter < RANSAC_ITERATIONS; iter++) {
            // Randomly select 3 matches
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

            // Compute transformation from these 3 points
            AffineTransform transform = computeAffineFromMatches(sample);
            if (transform == null) continue;

            // Count inliers
            int inliers = countInliers(matches, transform);

            if (inliers > bestInliers) {
                bestInliers = inliers;
                bestTransform = transform;
            }
        }

        if (bestTransform == null) {
            System.out.println("⚠️ Impossible de trouver une transformation valide");
            return new AlignmentResult(AffineTransform.identity(), 0, matches.size());
        }

        System.out.println("Transformation trouvée avec " + bestInliers + " inliers sur " + matches.size() + " correspondances");

        return new AlignmentResult(bestTransform, bestInliers, matches.size());
    }

    private static List<StarMatch> findStarMatches(List<Star> referenceStars, List<Star> imageStars) {
        List<StarMatch> matches = new ArrayList<>();

        // Create triangles for matching
        List<StarTriangle> refTriangles = createTriangles(referenceStars);
        List<StarTriangle> imgTriangles = createTriangles(imageStars);

        // Match triangles (scale-invariant, rotation-invariant)
        for (StarTriangle refTri : refTriangles) {
            for (StarTriangle imgTri : imgTriangles) {
                if (trianglesMatch(refTri, imgTri)) {
                    // Add the three star correspondences
                    matches.add(new StarMatch(refTri.s1, imgTri.s1));
                    matches.add(new StarMatch(refTri.s2, imgTri.s2));
                    matches.add(new StarMatch(refTri.s3, imgTri.s3));
                }
            }
        }

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

        // Check if side ratios are similar (scale-invariant)
        double ratio1_1 = sides1[1] / sides1[0];
        double ratio1_2 = sides1[2] / sides1[0];
        double ratio2_1 = sides2[1] / sides2[0];
        double ratio2_2 = sides2[2] / sides2[0];

        double tolerance = 0.15; // Plus tolérant pour ciel profond

        return Math.abs(ratio1_1 - ratio2_1) < tolerance &&
                Math.abs(ratio1_2 - ratio2_2) < tolerance;
    }

    /**
     * Calcule la transformation affine à partir de 3 correspondances de points
     */
    private static AffineTransform computeAffineFromMatches(List<StarMatch> matches) {
        if (matches.size() < 3) return null;

        // Get 3 point correspondences
        StarMatch m1 = matches.get(0);
        StarMatch m2 = matches.get(1);
        StarMatch m3 = matches.get(2);

        // Calculate centroid in both sets
        double refCx = (m1.ref.getX() + m2.ref.getX() + m3.ref.getX()) / 3.0;
        double refCy = (m1.ref.getY() + m2.ref.getY() + m3.ref.getY()) / 3.0;
        double imgCx = (m1.img.getX() + m2.img.getX() + m3.img.getX()) / 3.0;
        double imgCy = (m1.img.getY() + m2.img.getY() + m3.img.getY()) / 3.0;

        // Calculate scale and rotation using first two points
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

        // Translation
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

    /**
     * Transformation affine: scale + rotation + translation
     */
    public static class AffineTransform {
        public final double scale;
        public final double rotation; // en radians
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

        /**
         * Applique la transformation à un point (x, y)
         */
        public double[] apply(double x, double y) {
            double cos = Math.cos(rotation);
            double sin = Math.sin(rotation);

            double newX = scale * (x * cos - y * sin) + tx;
            double newY = scale * (x * sin + y * cos) + ty;

            return new double[]{newX, newY};
        }

        /**
         * Transformation inverse
         */
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