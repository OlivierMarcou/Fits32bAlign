package com.astro;

import java.util.*;

public class ImageAligner {
    private static final int MIN_MATCHING_STARS = 10;
    private static final double MAX_DISTANCE_TOLERANCE = 5.0;

    public static void alignImages(List<FitsImage> images, ProgressCallback callback) {
        if (images.isEmpty()) return;

        // Use first image as reference
        FitsImage reference = images.get(0);
        
        if (callback != null) {
            callback.onProgress(0, "Détection des étoiles dans l'image de référence...");
        }
        
        List<Star> referenceStars = StarDetector.detectStars(reference, 100);
        
        if (referenceStars.size() < MIN_MATCHING_STARS) {
            if (callback != null) {
                callback.onProgress(0, "Attention: peu d'étoiles détectées dans l'image de référence");
            }
        }

        // Align each subsequent image
        for (int i = 1; i < images.size(); i++) {
            FitsImage image = images.get(i);
            
            if (callback != null) {
                int progress = (int) ((i * 100.0) / images.size());
                callback.onProgress(progress, "Alignement de " + image.getFileName() + "...");
            }
            
            List<Star> imageStars = StarDetector.detectStars(image, 100);
            
            // Find translation offset
            Offset offset = findBestOffset(referenceStars, imageStars);
            image.setOffsetX(-offset.dx);
            image.setOffsetY(-offset.dy);
        }
        
        if (callback != null) {
            callback.onProgress(100, "Alignement terminé!");
        }
    }

    private static Offset findBestOffset(List<Star> referenceStars, List<Star> imageStars) {
        double bestScore = 0;
        Offset bestOffset = new Offset(0, 0);

        // Try to match stars using triangle similarity
        List<StarTriangle> refTriangles = createTriangles(referenceStars);
        List<StarTriangle> imgTriangles = createTriangles(imageStars);

        for (StarTriangle refTri : refTriangles) {
            for (StarTriangle imgTri : imgTriangles) {
                if (trianglesMatch(refTri, imgTri)) {
                    // Calculate offset from this match
                    double dx = refTri.s1.getX() - imgTri.s1.getX();
                    double dy = refTri.s1.getY() - imgTri.s1.getY();
                    
                    // Score this offset by counting matching stars
                    double score = scoreOffset(referenceStars, imageStars, dx, dy);
                    
                    if (score > bestScore) {
                        bestScore = score;
                        bestOffset = new Offset(dx, dy);
                    }
                }
            }
        }

        return bestOffset;
    }

    private static List<StarTriangle> createTriangles(List<Star> stars) {
        List<StarTriangle> triangles = new ArrayList<>();
        
        // Create triangles from brightest stars
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
        
        // Check if side ratios are similar
        double ratio1 = sides1[1] / sides1[0];
        double ratio2 = sides2[1] / sides2[0];
        double ratio3 = sides1[2] / sides1[0];
        double ratio4 = sides2[2] / sides2[0];
        
        double tolerance = 0.1;
        
        return Math.abs(ratio1 - ratio2) < tolerance && 
               Math.abs(ratio3 - ratio4) < tolerance;
    }

    private static double scoreOffset(List<Star> refStars, List<Star> imgStars, 
                                     double dx, double dy) {
        int matches = 0;
        
        for (Star refStar : refStars) {
            double targetX = refStar.getX() - dx;
            double targetY = refStar.getY() - dy;
            
            // Find closest star in image
            for (Star imgStar : imgStars) {
                double dist = Math.sqrt(
                    Math.pow(imgStar.getX() - targetX, 2) + 
                    Math.pow(imgStar.getY() - targetY, 2)
                );
                
                if (dist < MAX_DISTANCE_TOLERANCE) {
                    matches++;
                    break;
                }
            }
        }
        
        return matches;
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

    private static class Offset {
        final double dx;
        final double dy;

        Offset(double dx, double dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    public interface ProgressCallback {
        void onProgress(int percent, String message);
    }
}
