package com.astro;

import nom.tam.fits.*;
import nom.tam.util.BufferedDataOutputStream;

import java.io.*;
import java.nio.file.*;
import java.util.Random;

/**
 * Générateur d'images FITS de test avec étoiles artificielles
 * Utile pour tester l'application sans avoir d'images astronomiques réelles
 */
public class TestImageGenerator {
    private static final int DEFAULT_WIDTH = 2048;
    private static final int DEFAULT_HEIGHT = 2048;
    private static final int DEFAULT_NUM_STARS = 100;
    private static final double NOISE_LEVEL = 10.0;
    private static final double BACKGROUND_LEVEL = 100.0;

    public static void main(String[] args) {
        try {
            String outputDir = args.length > 0 ? args[0] : "test_images";
            int numImages = args.length > 1 ? Integer.parseInt(args[1]) : 10;
            
            System.out.println("=== Générateur d'Images FITS de Test ===");
            System.out.println("Dossier de sortie : " + outputDir);
            System.out.println("Nombre d'images : " + numImages);
            System.out.println();
            
            generateTestSet(outputDir, numImages);
            
            System.out.println("\n✓ Génération terminée!");
            System.out.println("Vous pouvez maintenant utiliser ces images dans FITS Stacker.");
            
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void generateTestSet(String outputDir, int numImages) throws Exception {
        Path dirPath = Paths.get(outputDir);
        Files.createDirectories(dirPath);
        
        Random random = new Random(42); // Seed fixe pour reproductibilité
        
        // Générer un champ d'étoiles de base
        System.out.println("Création du champ d'étoiles de référence...");
        StarField baseField = createStarField(random);
        
        // Générer plusieurs images avec décalages
        for (int i = 0; i < numImages; i++) {
            int offsetX = random.nextInt(41) - 20; // -20 à +20 pixels
            int offsetY = random.nextInt(41) - 20;
            
            System.out.printf("Génération image %d/%d (offset: %+d, %+d)...%n", 
                i + 1, numImages, offsetX, offsetY);
            
            float[][] imageData = createShiftedImage(baseField, offsetX, offsetY, random);
            
            String filename = String.format("test_image_%03d.fits", i + 1);
            Path outputPath = dirPath.resolve(filename);
            
            saveAsFits(imageData, outputPath);
        }
    }

    private static StarField createStarField(Random random) {
        StarField field = new StarField();
        
        for (int i = 0; i < DEFAULT_NUM_STARS; i++) {
            int x = random.nextInt(DEFAULT_WIDTH - 100) + 50;
            int y = random.nextInt(DEFAULT_HEIGHT - 100) + 50;
            double brightness = 500 + random.nextDouble() * 4500;
            double size = 2.0 + random.nextDouble() * 3.0;
            
            field.addStar(x, y, brightness, size);
        }
        
        return field;
    }

    private static float[][] createShiftedImage(StarField field, int offsetX, int offsetY, 
                                               Random random) {
        float[][] image = new float[DEFAULT_HEIGHT][DEFAULT_WIDTH];
        
        // Ajouter le fond avec bruit
        for (int y = 0; y < DEFAULT_HEIGHT; y++) {
            for (int x = 0; x < DEFAULT_WIDTH; x++) {
                image[y][x] = (float) (BACKGROUND_LEVEL + random.nextGaussian() * NOISE_LEVEL);
            }
        }
        
        // Ajouter les étoiles avec décalage
        for (StarField.Star star : field.stars) {
            int centerX = star.x + offsetX;
            int centerY = star.y + offsetY;
            
            // Créer une PSF gaussienne pour chaque étoile
            addGaussianStar(image, centerX, centerY, star.brightness, star.size);
        }
        
        // Ajouter du bruit supplémentaire unique à cette image
        for (int y = 0; y < DEFAULT_HEIGHT; y++) {
            for (int x = 0; x < DEFAULT_WIDTH; x++) {
                image[y][x] += (float) (random.nextGaussian() * 5.0);
                
                // S'assurer que les valeurs sont positives
                if (image[y][x] < 0) image[y][x] = 0;
            }
        }
        
        return image;
    }

    private static void addGaussianStar(float[][] image, int centerX, int centerY, 
                                       double brightness, double size) {
        int radius = 10;
        int height = image.length;
        int width = image[0].length;
        
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;
                
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    double distance = dx * dx + dy * dy;
                    double value = brightness * Math.exp(-distance / (2 * size * size));
                    image[y][x] += (float) value;
                }
            }
        }
    }

    private static void saveAsFits(float[][] data, Path outputPath) throws Exception {
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
        
        void addStar(int x, int y, double brightness, double size) {
            stars.add(new Star(x, y, brightness, size));
        }
        
        private static class Star {
            final int x, y;
            final double brightness, size;
            
            Star(int x, int y, double brightness, double size) {
                this.x = x;
                this.y = y;
                this.brightness = brightness;
                this.size = size;
            }
        }
    }
}
