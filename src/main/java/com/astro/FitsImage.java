package com.astro;

import nom.tam.fits.*;
import nom.tam.util.BufferedDataOutputStream;

import java.io.*;
import java.nio.file.Path;

/**
 * VERSION CORRIGÉE avec débogage pour problème d'images noires
 */
public class FitsImage {
    protected final Path path;
    protected float[][] data;
    protected float[][][] colorData;
    protected int width;
    protected int height;
    protected boolean isColor = false;
    protected ImageAligner.AffineTransform transform = ImageAligner.AffineTransform.identity();
    protected ImageAligner.CanvasInfo canvasInfo = null;
    protected double alignmentQuality = 1.0;

    public FitsImage(Path path) throws Exception {
        this.path = path;
        loadFits();
    }

    protected FitsImage(Path path, int width, int height, boolean isColor) {
        this.path = path;
        this.width = width;
        this.height = height;
        this.isColor = isColor;
        if (isColor) {
            this.colorData = new float[3][height][width];
            this.data = new float[height][width];
        } else {
            this.data = new float[height][width];
        }
    }

    public static FitsImage createEmpty(Path path, int width, int height, boolean isColor) {
        return new FitsImage(path, width, height, isColor);
    }

    protected void loadFits() throws Exception {
        try (Fits fits = new Fits(path.toFile())) {
            BasicHDU<?> hdu = fits.readHDU();

            if (hdu == null) {
                throw new IllegalArgumentException("Fichier FITS vide ou corrompu");
            }

            Object rawData = hdu.getData().getData();

            if (rawData == null) {
                throw new IllegalArgumentException("Aucune donnée dans le fichier FITS");
            }

            System.out.println("Type FITS détecté: " + rawData.getClass().getName());

            // [... reste du code de chargement identique ...]
            // Je garde seulement les parties essentielles pour la correction

            if (rawData instanceof float[][][]) {
                float[][][] data3D = (float[][][]) rawData;
                if (data3D.length == 3) {
                    loadColorFloat3D(data3D);
                } else {
                    this.data = extract2DFromFloat3D(data3D);
                }
            } else if (rawData instanceof float[][] floatData) {
                this.data = floatData;
            } else if (rawData instanceof short[][] shortData) {
                this.data = convertToFloat(shortData);
            } else if (rawData instanceof int[][] intData) {
                this.data = convertToFloat(intData);
            } else if (rawData instanceof double[][] doubleData) {
                this.data = convertToFloat(doubleData);
            } else {
                throw new IllegalArgumentException("Format FITS non supporté: " + rawData.getClass().getName());
            }

            if (this.data == null || this.data.length == 0) {
                throw new IllegalArgumentException("Données FITS invalides");
            }

            this.height = data.length;
            this.width = data[0].length;

            System.out.println("Image chargée: " + width + "x" + height + " pixels" +
                    (isColor ? " (RGB)" : " (Mono)"));
        }
    }

    // [Méthodes de conversion - code identique omis pour la lisibilité]

    private void loadColorFloat3D(float[][][] input) {
        isColor = true;
        height = input[0].length;
        width = input[0][0].length;
        colorData = input;
        data = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                data[y][x] = colorData[1][y][x];
            }
        }
    }

    private float[][] extract2DFromFloat3D(float[][][] input) {
        int height = input[0].length;
        int width = input[0][0].length;
        float[][] result = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = input[0][y][x];
            }
        }
        return result;
    }

    private float[][] convertToFloat(short[][] input) {
        float[][] result = new float[input.length][input[0].length];
        for (int y = 0; y < input.length; y++) {
            for (int x = 0; x < input[0].length; x++) {
                result[y][x] = input[y][x] & 0xFFFF;
            }
        }
        return result;
    }

    private float[][] convertToFloat(int[][] input) {
        float[][] result = new float[input.length][input[0].length];
        for (int y = 0; y < input.length; y++) {
            for (int x = 0; x < input[0].length; x++) {
                result[y][x] = input[y][x];
            }
        }
        return result;
    }

    private float[][] convertToFloat(double[][] input) {
        float[][] result = new float[input.length][input[0].length];
        for (int y = 0; y < input.length; y++) {
            for (int x = 0; x < input[0].length; x++) {
                result[y][x] = (float) input[y][x];
            }
        }
        return result;
    }

    // [Méthodes publiques standard]

    public void saveFits(Path outputPath) throws Exception {
        Fits fits = new Fits();
        if (isColor && colorData != null) {
            ImageHDU hdu = (ImageHDU) Fits.makeHDU(colorData);
            fits.addHDU(hdu);
        } else {
            ImageHDU hdu = (ImageHDU) Fits.makeHDU(data);
            fits.addHDU(hdu);
        }
        try (BufferedDataOutputStream os = new BufferedDataOutputStream(
                new FileOutputStream(outputPath.toFile()))) {
            fits.write(os);
        }
    }

    public float getPixel(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return 0;
        }
        return data[y][x];
    }

    public float getPixel(int channel, int x, int y) {
        if (!isColor || colorData == null) {
            return getPixel(x, y);
        }
        if (channel < 0 || channel >= 3 || x < 0 || x >= width || y < 0 || y >= height) {
            return 0;
        }
        return colorData[channel][y][x];
    }

    public void setPixel(int x, int y, float value) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            data[y][x] = value;
        }
    }

    public void setPixel(int channel, int x, int y, float value) {
        if (!isColor || colorData == null) {
            setPixel(x, y, value);
            return;
        }
        if (channel >= 0 && channel < 3 && x >= 0 && x < width && y >= 0 && y < height) {
            colorData[channel][y][x] = value;
        }
    }

    public float[][] getData() { return data; }
    public float[][][] getColorData() { return colorData; }
    public boolean isColor() { return isColor; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Path getPath() { return path; }
    public String getFileName() { return path.getFileName().toString(); }

    public void setTransform(ImageAligner.AffineTransform transform) {
        this.transform = transform;
    }
    public ImageAligner.AffineTransform getTransform() { return transform; }

    public void setCanvasInfo(ImageAligner.CanvasInfo canvasInfo) {
        this.canvasInfo = canvasInfo;
    }
    public ImageAligner.CanvasInfo getCanvasInfo() { return canvasInfo; }

    public void setAlignmentQuality(double quality) {
        this.alignmentQuality = quality;
    }
    public double getAlignmentQuality() { return alignmentQuality; }

    /**
     * 🔧 VERSION CORRIGÉE - Crée une copie alignée avec canvas élargi
     * AVEC DÉBOGAGE DÉTAILLÉ
     */
    public FitsImage createAlignedCopy(int canvasWidth, int canvasHeight, int offsetX, int offsetY) {
        try {
            System.out.println("\n=== Création copie alignée ===");
            System.out.println("Image source: " + getFileName());
            System.out.println("  Dimensions source: " + width + "x" + height);
            System.out.println("  Canvas destination: " + canvasWidth + "x" + canvasHeight);
            System.out.println("  Offset: (" + offsetX + ", " + offsetY + ")");
            System.out.println("  Transformation:");
            System.out.println("    rotation = " + Math.toDegrees(transform.rotation) + "°");
            System.out.println("    scale = " + transform.scale);
            System.out.println("    tx = " + transform.tx);
            System.out.println("    ty = " + transform.ty);

            FitsImage copy = FitsImage.createEmpty(this.path, canvasWidth, canvasHeight, this.isColor);

            // Compteurs pour diagnostic
            int validPixels = 0;
            int outOfBounds = 0;
            double sumValues = 0;

            if (isColor && colorData != null) {
                // Transformer chaque canal RGB
                for (int c = 0; c < 3; c++) {
                    for (int y = 0; y < canvasHeight; y++) {
                        for (int x = 0; x < canvasWidth; x++) {
                            // ⚠️ CORRECTION CRITIQUE ICI
                            // On transforme du canvas vers l'image source
                            double[] srcPoint = transformInverse(x - offsetX, y - offsetY);
                            double srcX = srcPoint[0];
                            double srcY = srcPoint[1];

                            float value = interpolateChannel(c, srcX, srcY);
                            copy.colorData[c][y][x] = value;

                            if (c == 0) { // Compter seulement une fois
                                if (value > 0) {
                                    validPixels++;
                                    sumValues += value;
                                } else {
                                    outOfBounds++;
                                }
                            }
                        }
                    }
                }

                // Mettre à jour la version mono
                for (int y = 0; y < canvasHeight; y++) {
                    for (int x = 0; x < canvasWidth; x++) {
                        copy.data[y][x] = copy.colorData[1][y][x];
                    }
                }
            } else {
                // Image mono
                for (int y = 0; y < canvasHeight; y++) {
                    for (int x = 0; x < canvasWidth; x++) {
                        // ⚠️ CORRECTION CRITIQUE ICI
                        double[] srcPoint = transformInverse(x - offsetX, y - offsetY);
                        double srcX = srcPoint[0];
                        double srcY = srcPoint[1];

                        float value = interpolate(srcX, srcY);
                        copy.data[y][x] = value;

                        if (value > 0) {
                            validPixels++;
                            sumValues += value;
                        } else {
                            outOfBounds++;
                        }
                    }
                }
            }

            // Diagnostic final
            int totalPixels = canvasWidth * canvasHeight;
            double coverage = (validPixels * 100.0) / totalPixels;
            double avgValue = validPixels > 0 ? sumValues / validPixels : 0;

            System.out.println("  Résultat:");
            System.out.println("    Pixels valides: " + validPixels + "/" + totalPixels +
                    " (" + String.format("%.1f", coverage) + "%)");
            System.out.println("    Valeur moyenne: " + String.format("%.1f", avgValue));

            if (validPixels == 0) {
                System.out.println("  ⚠️⚠️⚠️ ATTENTION: AUCUN PIXEL VALIDE!");
                System.out.println("  → La transformation inverse ne fonctionne pas correctement");
                System.out.println("  → Toutes les coordonnées tombent en dehors de l'image source");

                // Test de diagnostic
                System.out.println("\n  TEST DIAGNOSTIC:");
                double[] testPoint = transformInverse(canvasWidth/2 - offsetX, canvasHeight/2 - offsetY);
                System.out.println("    Centre canvas (" + canvasWidth/2 + ", " + canvasHeight/2 + ")");
                System.out.println("    → Source: (" + testPoint[0] + ", " + testPoint[1] + ")");
                System.out.println("    Image source: 0-" + (width-1) + ", 0-" + (height-1));

                if (testPoint[0] < 0 || testPoint[0] >= width ||
                        testPoint[1] < 0 || testPoint[1] >= height) {
                    System.out.println("    ✗ Centre hors limites!");
                } else {
                    System.out.println("    ✓ Centre dans l'image");
                }
            } else if (coverage < 10) {
                System.out.println("  ⚠️ ATTENTION: Couverture très faible (<10%)");
            } else if (coverage < 50) {
                System.out.println("  ⚠️ Couverture moyenne: vérifier les paramètres");
            }

            return copy;
        } catch (Exception e) {
            System.err.println("ERREUR dans createAlignedCopy: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 🔧 TRANSFORMATION INVERSE CORRIGÉE
     * Applique la transformation inverse complète
     */
    private double[] transformInverse(double x, double y) {
        // Appliquer la transformation affine inverse
        double[] pt = transform.applyInverse(x, y);

        // 🔍 DÉBOGAGE: Vérifier quelques points
        if (Math.random() < 0.0001) { // Log 0.01% des points pour ne pas spammer
            System.out.println(String.format(
                    "    Point canvas (%.1f, %.1f) → source (%.1f, %.1f) [image: %dx%d]",
                    x, y, pt[0], pt[1], width, height
            ));
        }

        return new double[]{pt[0], pt[1]};
    }

    /**
     * Interpolation bilinéaire pour un canal (RGB)
     */
    private float interpolateChannel(int channel, double x, double y) {
        // Vérifier les limites
        if (x < 0 || x >= width - 1 || y < 0 || y >= height - 1) {
            return 0;
        }

        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int x1 = Math.min(x0 + 1, width - 1);
        int y1 = Math.min(y0 + 1, height - 1);

        double dx = x - x0;
        double dy = y - y0;

        float v00 = colorData[channel][y0][x0];
        float v10 = colorData[channel][y0][x1];
        float v01 = colorData[channel][y1][x0];
        float v11 = colorData[channel][y1][x1];

        float v0 = (float) (v00 * (1 - dx) + v10 * dx);
        float v1 = (float) (v01 * (1 - dx) + v11 * dx);

        return (float) (v0 * (1 - dy) + v1 * dy);
    }

    /**
     * Interpolation bilinéaire pour image mono
     */
    private float interpolate(double x, double y) {
        // Vérifier les limites
        if (x < 0 || x >= width - 1 || y < 0 || y >= height - 1) {
            return 0;
        }

        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int x1 = Math.min(x0 + 1, width - 1);
        int y1 = Math.min(y0 + 1, height - 1);

        double dx = x - x0;
        double dy = y - y0;

        float v00 = data[y0][x0];
        float v10 = data[y0][x1];
        float v01 = data[y1][x0];
        float v11 = data[y1][x1];

        float v0 = (float) (v00 * (1 - dx) + v10 * dx);
        float v1 = (float) (v01 * (1 - dx) + v11 * dx);

        return (float) (v0 * (1 - dy) + v1 * dy);
    }
}
