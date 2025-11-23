package com.astro;

import nom.tam.fits.*;
import nom.tam.util.BufferedDataOutputStream;

import java.io.*;
import java.nio.file.*;
import java.util.Random;

/**
 * Générateur d'images FITS RGB de test avec étoiles artificielles
 * Utile pour tester le support des images couleur
 */
public class TestImageGeneratorRGB {
    private static final int DEFAULT_WIDTH = 1024;
    private static final int DEFAULT_HEIGHT = 1024;
    private static final int DEFAULT_NUM_STARS = 80;
    private static final double NOISE_LEVEL = 8.0;
    private static final double BACKGROUND_LEVEL = 100.0;

    public static void main(String[] args) {
        try {
            String outputDir = args.length > 0 ? args[0] : "test_images_rgb";
            int numImages = args.length > 1 ? Integer.parseInt(args[1]) : 5;
            
            System.out.println("=== Générateur d'Images FITS RGB de Test ===");
            System.out.println("Dossier de sortie : " + outputDir);
            System.out.println("Nombre d'images : " + numImages);
            System.out.println();
            
            generateTestSetRGB(outputDir, numImages);
            
            System.out.println("\n✓ Génération terminée!");
            System.out.println("Vous pouvez maintenant utiliser ces images RGB dans FITS Stacker.");
            
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void generateTestSetRGB(String outputDir, int numImages) throws Exception {
        Path dirPath = Paths.get(outputDir);
        Files.createDirectories(dirPath);
        
        Random random = new Random(42); // Seed fixe pour reproductibilité
        
        // Générer un champ d'étoiles de base
        System.out.println("Création du champ d'étoiles de référence...");
        StarField baseField = createStarField(random);
        
        // Générer plusieurs images RGB avec décalages et rotations
        for (int i = 0; i < numImages; i++) {
            int offsetX = random.nextInt(41) - 20; // -20 à +20 pixels
            int offsetY = random.nextInt(41) - 20;
            double rotation = (random.nextDouble() - 0.5) * 10.0; // -5° à +5°
            
            System.out.printf("Génération image RGB %d/%d (offset: %+d, %+d, rot: %.1f°)...%n", 
                i + 1, numImages, offsetX, offsetY, rotation);
            
            float[][][] imageData = createShiftedRGBImage(baseField, offsetX, offsetY, rotation, random);
            
            String filename = String.format("test_rgb_%03d.fits", i + 1);
            Path outputPath = dirPath.resolve(filename);
            
            saveAsRGBFits(imageData, outputPath);
        }
    }

    private static StarField createStarField(Random random) {
        StarField field = new StarField();
        
        for (int i = 0; i < DEFAULT_NUM_STARS; i++) {
            int x = random.nextInt(DEFAULT_WIDTH - 100) + 50;
            int y = random.nextInt(DEFAULT_HEIGHT - 100) + 50;
            double brightness = 300 + random.nextDouble() * 4000;
            double size = 1.5 + random.nextDouble() * 2.5;
            
            // Couleur de l'étoile (température)
            // Étoiles bleues : température haute
            // Étoiles jaunes/oranges : température moyenne
            // Étoiles rouges : température basse
            double temperature = random.nextDouble();
            double r, g, b;
            
            if (temperature < 0.3) {
                // Étoiles bleues/blanches chaudes
                r = 0.7 + random.nextDouble() * 0.3;
                g = 0.8 + random.nextDouble() * 0.2;
                b = 1.0;
            } else if (temperature < 0.7) {
                // Étoiles jaunes/blanches moyennes
                r = 0.9 + random.nextDouble() * 0.1;
                g = 0.9 + random.nextDouble() * 0.1;
                b = 0.8 + random.nextDouble() * 0.2;
            } else {
                // Étoiles rouges/oranges froides
                r = 1.0;
                g = 0.6 + random.nextDouble() * 0.3;
                b = 0.3 + random.nextDouble() * 0.3;
            }
            
            field.addStar(x, y, brightness, size, r, g, b);
        }
        
        return field;
    }

    private static float[][][] createShiftedRGBImage(StarField field, int offsetX, int offsetY, 
                                                    double rotationDeg, Random random) {
        float[][][] image = new float[3][DEFAULT_HEIGHT][DEFAULT_WIDTH]; // [canal][y][x]
        
        double rotationRad = Math.toRadians(rotationDeg);
        double cos = Math.cos(rotationRad);
        double sin = Math.sin(rotationRad);
        int centerX = DEFAULT_WIDTH / 2;
        int centerY = DEFAULT_HEIGHT / 2;
        
        // Ajouter le fond avec bruit pour chaque canal
        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < DEFAULT_HEIGHT; y++) {
                for (int x = 0; x < DEFAULT_WIDTH; x++) {
                    image[c][y][x] = (float) (BACKGROUND_LEVEL + random.nextGaussian() * NOISE_LEVEL);
                }
            }
        }
        
        // Ajouter les étoiles avec transformation (rotation + translation)
        for (StarField.Star star : field.stars) {
            // Appliquer rotation autour du centre
            double dx = star.x - centerX;
            double dy = star.y - centerY;
            double rotatedX = dx * cos - dy * sin;
            double rotatedY = dx * sin + dy * cos;
            
            // Ajouter translation
            int finalX = (int) (centerX + rotatedX + offsetX);
            int finalY = (int) (centerY + rotatedY + offsetY);
            
            // Créer une PSF gaussienne pour chaque étoile dans chaque canal
            addGaussianStarRGB(image, finalX, finalY, star.brightness, star.size, 
                             star.colorR, star.colorG, star.colorB);
        }
        
        // Ajouter du bruit supplémentaire unique à cette image
        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < DEFAULT_HEIGHT; y++) {
                for (int x = 0; x < DEFAULT_WIDTH; x++) {
                    image[c][y][x] += (float) (random.nextGaussian() * 3.0);
                    
                    // S'assurer que les valeurs sont positives
                    if (image[c][y][x] < 0) image[c][y][x] = 0;
                }
            }
        }
        
        return image;
    }

    private static void addGaussianStarRGB(float[][][] image, int centerX, int centerY, 
                                          double brightness, double size,
                                          double colorR, double colorG, double colorB) {
        int radius = 12;
        int height = image[0].length;
        int width = image[0][0].length;
        
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;
                
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    double distance = dx * dx + dy * dy;
                    double baseValue = brightness * Math.exp(-distance / (2 * size * size));
                    
                    // Appliquer la couleur spécifique à chaque canal
                    image[0][y][x] += (float) (baseValue * colorR); // R
                    image[1][y][x] += (float) (baseValue * colorG); // G
                    image[2][y][x] += (float) (baseValue * colorB); // B
                }
            }
        }
    }

    private static void saveAsRGBFits(float[][][] data, Path outputPath) throws Exception {
        Fits fits = new Fits();
        ImageHDU hdu = (ImageHDU) Fits.makeHDU(data);
        fits.addHDU(hdu);
        
        try (BufferedDataOutputStream os = new BufferedDataOutputStream(
                new FileOutputStream(outputPath.toFile()))) {
            fits.write(os);
        }
    }

    private static class StarField {
        private final java.util.List<Star> stars = new java.util.ArrayList<>();
        
        void addStar(int x, int y, double brightness, double size, 
                    double colorR, double colorG, double colorB) {
            stars.add(new Star(x, y, brightness, size, colorR, colorG, colorB));
        }
        
        private static class Star {
            final int x, y;
            final double brightness, size;
            final double colorR, colorG, colorB;
            
            Star(int x, int y, double brightness, double size,
                 double colorR, double colorG, double colorB) {
                this.x = x;
                this.y = y;
                this.brightness = brightness;
                this.size = size;
                this.colorR = colorR;
                this.colorG = colorG;
                this.colorB = colorB;
            }
        }
    }
}
