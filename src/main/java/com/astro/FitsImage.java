package com.astro;

import nom.tam.fits.*;
import nom.tam.util.BufferedDataOutputStream;

import java.io.*;
import java.nio.file.Path;

public class FitsImage {
    private final Path path;
    private float[][] data;
    private int width;
    private int height;
    private ImageAligner.AffineTransform transform = ImageAligner.AffineTransform.identity();

    public FitsImage(Path path) throws Exception {
        this.path = path;
        loadFits();
    }

    private void loadFits() throws Exception {
        try (Fits fits = new Fits(path.toFile())) {
            BasicHDU<?> hdu = fits.readHDU();

            if (hdu == null) {
                throw new IllegalArgumentException("Fichier FITS vide ou corrompu");
            }

            Object rawData = hdu.getData().getData();

            if (rawData == null) {
                throw new IllegalArgumentException("Aucune donnée dans le fichier FITS");
            }

            // Log du type détecté pour debug
            System.out.println("Type FITS détecté: " + rawData.getClass().getName());

            // Convert to float[][] regardless of input type
            if (rawData instanceof float[][] floatData) {
                this.data = floatData;
                System.out.println("Format: float[][] (32-bit float)");
            } else if (rawData instanceof short[][] shortData) {
                this.data = convertToFloat(shortData);
                System.out.println("Format: short[][] (16-bit integer)");
            } else if (rawData instanceof int[][] intData) {
                this.data = convertToFloat(intData);
                System.out.println("Format: int[][] (32-bit integer)");
            } else if (rawData instanceof long[][] longData) {
                this.data = convertToFloat(longData);
                System.out.println("Format: long[][] (64-bit integer)");
            } else if (rawData instanceof double[][] doubleData) {
                this.data = convertToFloat(doubleData);
                System.out.println("Format: double[][] (64-bit float)");
            } else if (rawData instanceof byte[][] byteData) {
                this.data = convertToFloat(byteData);
                System.out.println("Format: byte[][] (8-bit integer)");
            } else if (rawData instanceof float[][][]) {
                // Images 3D (RGB, cubes de données, etc.) - extraction du premier plan
                this.data = extract2DFromFloat3D((float[][][]) rawData);
                System.out.println("Format: float[][][] (3D cube - extraction plan 1)");
            } else if (rawData instanceof short[][][]) {
                this.data = extract2DFromShort3D((short[][][]) rawData);
                System.out.println("Format: short[][][] (3D cube - extraction plan 1)");
            } else if (rawData instanceof int[][][]) {
                this.data = extract2DFromInt3D((int[][][]) rawData);
                System.out.println("Format: int[][][] (3D cube - extraction plan 1)");
            } else if (rawData instanceof double[][][]) {
                this.data = extract2DFromDouble3D((double[][][]) rawData);
                System.out.println("Format: double[][][] (3D cube - extraction plan 1)");
            } else if (rawData instanceof byte[][][]) {
                this.data = extract2DFromByte3D((byte[][][]) rawData);
                System.out.println("Format: byte[][][] (3D cube - extraction plan 1)");
            } else if (rawData instanceof float[]) {
                // Parfois les données 1D doivent être converties en 2D
                this.data = convert1DToFloat((float[]) rawData, hdu);
                System.out.println("Format: float[] (1D array converted to 2D)");
            } else if (rawData instanceof short[]) {
                this.data = convert1DToFloat((short[]) rawData, hdu);
                System.out.println("Format: short[] (1D array converted to 2D)");
            } else if (rawData instanceof int[]) {
                this.data = convert1DToFloat((int[]) rawData, hdu);
                System.out.println("Format: int[] (1D array converted to 2D)");
            } else {
                throw new IllegalArgumentException(
                        "Format FITS non supporté: " + rawData.getClass().getName() +
                                "\nLe fichier utilise un format de données non géré par l'application."
                );
            }

            if (this.data == null || this.data.length == 0) {
                throw new IllegalArgumentException("Données FITS invalides");
            }

            this.height = data.length;
            this.width = data[0].length;

            System.out.println("Image chargée: " + width + "x" + height + " pixels");
        }
    }

    // ========== Conversions 2D -> float[][] ==========

    private float[][] convertToFloat(short[][] input) {
        float[][] result = new float[input.length][input[0].length];
        for (int y = 0; y < input.length; y++) {
            for (int x = 0; x < input[0].length; x++) {
                result[y][x] = input[y][x] & 0xFFFF; // unsigned short
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

    private float[][] convertToFloat(long[][] input) {
        float[][] result = new float[input.length][input[0].length];
        for (int y = 0; y < input.length; y++) {
            for (int x = 0; x < input[0].length; x++) {
                result[y][x] = (float) input[y][x];
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

    private float[][] convertToFloat(byte[][] input) {
        float[][] result = new float[input.length][input[0].length];
        for (int y = 0; y < input.length; y++) {
            for (int x = 0; x < input[0].length; x++) {
                result[y][x] = input[y][x] & 0xFF; // unsigned byte
            }
        }
        return result;
    }

    // ========== Extraction de plans 2D à partir de cubes 3D ==========

    private float[][] extract2DFromFloat3D(float[][][] input) {
        if (input.length == 0 || input[0].length == 0) {
            throw new IllegalArgumentException("Cube 3D vide");
        }

        int numPlanes = input.length;
        int height = input[0].length;
        int width = input[0][0].length;

        System.out.println("Cube 3D détecté: " + numPlanes + " plans de " + width + "x" + height);

        // Extraction du premier plan (vous pouvez modifier pour faire une moyenne de tous les plans)
        float[][] result = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = input[0][y][x];
            }
        }

        return result;
    }

    private float[][] extract2DFromShort3D(short[][][] input) {
        if (input.length == 0 || input[0].length == 0) {
            throw new IllegalArgumentException("Cube 3D vide");
        }

        int numPlanes = input.length;
        int height = input[0].length;
        int width = input[0][0].length;

        System.out.println("Cube 3D détecté: " + numPlanes + " plans de " + width + "x" + height);

        float[][] result = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = input[0][y][x] & 0xFFFF;
            }
        }

        return result;
    }

    private float[][] extract2DFromInt3D(int[][][] input) {
        if (input.length == 0 || input[0].length == 0) {
            throw new IllegalArgumentException("Cube 3D vide");
        }

        int numPlanes = input.length;
        int height = input[0].length;
        int width = input[0][0].length;

        System.out.println("Cube 3D détecté: " + numPlanes + " plans de " + width + "x" + height);

        float[][] result = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = input[0][y][x];
            }
        }

        return result;
    }

    private float[][] extract2DFromDouble3D(double[][][] input) {
        if (input.length == 0 || input[0].length == 0) {
            throw new IllegalArgumentException("Cube 3D vide");
        }

        int numPlanes = input.length;
        int height = input[0].length;
        int width = input[0][0].length;

        System.out.println("Cube 3D détecté: " + numPlanes + " plans de " + width + "x" + height);

        float[][] result = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = (float) input[0][y][x];
            }
        }

        return result;
    }

    private float[][] extract2DFromByte3D(byte[][][] input) {
        if (input.length == 0 || input[0].length == 0) {
            throw new IllegalArgumentException("Cube 3D vide");
        }

        int numPlanes = input.length;
        int height = input[0].length;
        int width = input[0][0].length;

        System.out.println("Cube 3D détecté: " + numPlanes + " plans de " + width + "x" + height);

        float[][] result = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = input[0][y][x] & 0xFF;
            }
        }

        return result;
    }

    // ========== Conversions 1D -> 2D ==========

    private float[][] convert1DToFloat(float[] input, BasicHDU<?> hdu) {
        int[] axes = hdu.getAxes();
        if (axes == null || axes.length < 2) {
            throw new IllegalArgumentException("Impossible de déterminer les dimensions de l'image");
        }
        int width = axes[0];
        int height = axes[1];

        float[][] result = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = input[y * width + x];
            }
        }
        return result;
    }

    private float[][] convert1DToFloat(short[] input, BasicHDU<?> hdu) {
        int[] axes = hdu.getAxes();
        if (axes == null || axes.length < 2) {
            throw new IllegalArgumentException("Impossible de déterminer les dimensions de l'image");
        }
        int width = axes[0];
        int height = axes[1];

        float[][] result = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = input[y * width + x] & 0xFFFF;
            }
        }
        return result;
    }

    private float[][] convert1DToFloat(int[] input, BasicHDU<?> hdu) {
        int[] axes = hdu.getAxes();
        if (axes == null || axes.length < 2) {
            throw new IllegalArgumentException("Impossible de déterminer les dimensions de l'image");
        }
        int width = axes[0];
        int height = axes[1];

        float[][] result = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = input[y * width + x];
            }
        }
        return result;
    }

    // ========== Méthodes publiques ==========

    public void saveFits(Path outputPath) throws Exception {
        Fits fits = new Fits();
        ImageHDU hdu = (ImageHDU) Fits.makeHDU(data);
        fits.addHDU(hdu);

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

    public void setPixel(int x, int y, float value) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            data[y][x] = value;
        }
    }

    public float[][] getData() {
        return data;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Path getPath() {
        return path;
    }

    public String getFileName() {
        return path.getFileName().toString();
    }

    public void setTransform(ImageAligner.AffineTransform transform) {
        this.transform = transform;
    }

    public ImageAligner.AffineTransform getTransform() {
        return transform;
    }

    /**
     * Crée une copie alignée avec transformation affine complète
     * (rotation + échelle + translation) avec interpolation bilinéaire
     */
    public FitsImage createAlignedCopy() {
        try {
            FitsImage copy = new FitsImage(this.path);
            copy.data = new float[height][width];

            // Pour chaque pixel de l'image de destination
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // Transformer le point de destination vers la source
                    double[] srcPoint = transform.applyInverse(x, y);
                    double srcX = srcPoint[0];
                    double srcY = srcPoint[1];

                    // Interpolation bilinéaire
                    float value = interpolate(srcX, srcY);
                    copy.data[y][x] = value;
                }
            }

            return copy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Interpolation bilinéaire pour un échantillonnage sub-pixel lisse
     */
    private float interpolate(double x, double y) {
        // Si hors limites, retourner 0
        if (x < 0 || x >= width - 1 || y < 0 || y >= height - 1) {
            return 0;
        }

        // Coordonnées entières
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;

        // Fractions
        double dx = x - x0;
        double dy = y - y0;

        // Vérifier les limites
        if (x1 >= width) x1 = width - 1;
        if (y1 >= height) y1 = height - 1;

        // Interpolation bilinéaire
        float v00 = data[y0][x0];
        float v10 = data[y0][x1];
        float v01 = data[y1][x0];
        float v11 = data[y1][x1];

        float v0 = (float) (v00 * (1 - dx) + v10 * dx);
        float v1 = (float) (v01 * (1 - dx) + v11 * dx);

        return (float) (v0 * (1 - dy) + v1 * dy);
    }
}