package com.astro;

import nom.tam.fits.*;
import nom.tam.util.BufferedDataOutputStream;

import java.io.*;
import java.nio.file.Path;

/**
 * Gestion des images FITS avec support:
 * - Images monochromes et RGB (3 canaux)
 * - Alignement avec canvas élargi (pas de rognage)
 * - Interpolation bilinéaire pour transformations
 */
public class FitsImage {
    protected final Path path;
    protected float[][] data;  // Pour images grayscale
    protected float[][][] colorData;  // Pour images RGB [canal][y][x]
    protected int width;
    protected int height;
    protected boolean isColor = false;
    protected ImageAligner.AffineTransform transform = ImageAligner.AffineTransform.identity();
    protected ImageAligner.CanvasInfo canvasInfo = null;

    public FitsImage(Path path) throws Exception {
        this.path = path;
        loadFits();
    }

    // Constructeur pour créer une image vide
    protected FitsImage(Path path, int width, int height, boolean isColor) {
        this.path = path;
        this.width = width;
        this.height = height;
        this.isColor = isColor;
        if (isColor) {
            this.colorData = new float[3][height][width];
            this.data = new float[height][width]; // Version mono pour détection étoiles
        } else {
            this.data = new float[height][width];
        }
    }

    // Constructeur statique pour créer une image vide
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

            // Gestion des images RGB (3 canaux)
            if (rawData instanceof float[][][]) {
                float[][][] data3D = (float[][][]) rawData;
                if (data3D.length == 3) {
                    loadColorFloat3D(data3D);
                    System.out.println("Format: float[][][] RGB (3 canaux x " + width + "x" + height + ")");
                } else {
                    this.data = extract2DFromFloat3D(data3D);
                    System.out.println("Format: float[][][] (3D cube - extraction plan 1)");
                }
            } else if (rawData instanceof short[][][]) {
                short[][][] data3D = (short[][][]) rawData;
                if (data3D.length == 3) {
                    loadColorShort3D(data3D);
                    System.out.println("Format: short[][][] RGB (3 canaux x " + width + "x" + height + ")");
                } else {
                    this.data = extract2DFromShort3D(data3D);
                    System.out.println("Format: short[][][] (3D cube - extraction plan 1)");
                }
            } else if (rawData instanceof int[][][]) {
                int[][][] data3D = (int[][][]) rawData;
                if (data3D.length == 3) {
                    loadColorInt3D(data3D);
                    System.out.println("Format: int[][][] RGB (3 canaux x " + width + "x" + height + ")");
                } else {
                    this.data = extract2DFromInt3D(data3D);
                    System.out.println("Format: int[][][] (3D cube - extraction plan 1)");
                }
            } else if (rawData instanceof double[][][]) {
                double[][][] data3D = (double[][][]) rawData;
                if (data3D.length == 3) {
                    loadColorDouble3D(data3D);
                    System.out.println("Format: double[][][] RGB (3 canaux x " + width + "x" + height + ")");
                } else {
                    this.data = extract2DFromDouble3D(data3D);
                    System.out.println("Format: double[][][] (3D cube - extraction plan 1)");
                }
            }
            // Gestion des images 2D mono
            else if (rawData instanceof float[][] floatData) {
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
            }
            // Gestion des images 1D à convertir en 2D
            else if (rawData instanceof float[]) {
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

            System.out.println("Image chargée: " + width + "x" + height + " pixels" +
                    (isColor ? " (RGB)" : " (Mono)"));
        }
    }

    // ========== Chargement des images RGB ==========

    private void loadColorFloat3D(float[][][] input) {
        isColor = true;
        height = input[0].length;
        width = input[0][0].length;
        colorData = input;

        // Créer une version mono (canal vert) pour la détection d'étoiles
        data = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                data[y][x] = colorData[1][y][x]; // Canal vert (G)
            }
        }
    }

    private void loadColorShort3D(short[][][] input) {
        isColor = true;
        height = input[0].length;
        width = input[0][0].length;
        colorData = new float[3][height][width];

        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    colorData[c][y][x] = input[c][y][x] & 0xFFFF;
                }
            }
        }

        // Version mono
        data = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                data[y][x] = colorData[1][y][x];
            }
        }
    }

    private void loadColorInt3D(int[][][] input) {
        isColor = true;
        height = input[0].length;
        width = input[0][0].length;
        colorData = new float[3][height][width];

        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    colorData[c][y][x] = input[c][y][x];
                }
            }
        }

        data = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                data[y][x] = colorData[1][y][x];
            }
        }
    }

    private void loadColorDouble3D(double[][][] input) {
        isColor = true;
        height = input[0].length;
        width = input[0][0].length;
        colorData = new float[3][height][width];

        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    colorData[c][y][x] = (float) input[c][y][x];
                }
            }
        }

        data = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                data[y][x] = colorData[1][y][x];
            }
        }
    }

    // ========== Conversions 2D -> float[][] ==========

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
            for (int x = 0; x < width; x++) {
                result[y][x] = (float) input[y][x];
            }
        }
        return result;
    }

    private float[][] convertToFloat(byte[][] input) {
        float[][] result = new float[input.length][input[0].length];
        for (int y = 0; y < input.length; y++) {
            for (int x = 0; x < input[0].length; x++) {
                result[y][x] = input[y][x] & 0xFF;
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

        if (isColor && colorData != null) {
            // Sauvegarder en format RGB
            ImageHDU hdu = (ImageHDU) Fits.makeHDU(colorData);
            fits.addHDU(hdu);
        } else {
            // Sauvegarder en mono
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

    public float[][] getData() {
        return data;
    }

    public float[][][] getColorData() {
        return colorData;
    }

    public boolean isColor() {
        return isColor;
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

    public void setCanvasInfo(ImageAligner.CanvasInfo canvasInfo) {
        this.canvasInfo = canvasInfo;
    }

    public ImageAligner.CanvasInfo getCanvasInfo() {
        return canvasInfo;
    }

    /**
     * Crée une copie alignée avec canvas élargi pour ne rien rogner
     * @param canvasWidth Largeur du canvas élargi
     * @param canvasHeight Hauteur du canvas élargi
     * @param offsetX Décalage X pour placer l'image dans le canvas
     * @param offsetY Décalage Y pour placer l'image dans le canvas
     */
    public FitsImage createAlignedCopy(int canvasWidth, int canvasHeight, int offsetX, int offsetY) {
        try {
            FitsImage copy = FitsImage.createEmpty(this.path, canvasWidth, canvasHeight, this.isColor);

            if (isColor && colorData != null) {
                // Transformer chaque canal RGB
                for (int c = 0; c < 3; c++) {
                    for (int y = 0; y < canvasHeight; y++) {
                        for (int x = 0; x < canvasWidth; x++) {
                            // Transformer le point de destination vers la source
                            double[] srcPoint = transform.applyInverse(x - offsetX, y - offsetY);
                            double srcX = srcPoint[0];
                            double srcY = srcPoint[1];

                            float value = interpolateChannel(c, srcX, srcY);
                            copy.colorData[c][y][x] = value;
                        }
                    }
                }

                // Mettre à jour la version mono (canal vert)
                for (int y = 0; y < canvasHeight; y++) {
                    for (int x = 0; x < canvasWidth; x++) {
                        copy.data[y][x] = copy.colorData[1][y][x];
                    }
                }
            } else {
                // Image mono
                for (int y = 0; y < canvasHeight; y++) {
                    for (int x = 0; x < canvasWidth; x++) {
                        // Transformer le point de destination vers la source
                        double[] srcPoint = transform.applyInverse(x - offsetX, y - offsetY);
                        double srcX = srcPoint[0];
                        double srcY = srcPoint[1];

                        float value = interpolate(srcX, srcY);
                        copy.data[y][x] = value;
                    }
                }
            }

            return copy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Interpolation bilinéaire pour un canal spécifique (RGB)
     */
    private float interpolateChannel(int channel, double x, double y) {
        if (x < 0 || x >= width - 1 || y < 0 || y >= height - 1) {
            return 0;
        }

        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;

        double dx = x - x0;
        double dy = y - y0;

        if (x1 >= width) x1 = width - 1;
        if (y1 >= height) y1 = height - 1;

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
        if (x < 0 || x >= width - 1 || y < 0 || y >= height - 1) {
            return 0;
        }

        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;

        double dx = x - x0;
        double dy = y - y0;

        if (x1 >= width) x1 = width - 1;
        if (y1 >= height) y1 = height - 1;

        float v00 = data[y0][x0];
        float v10 = data[y0][x1];
        float v01 = data[y1][x0];
        float v11 = data[y1][x1];

        float v0 = (float) (v00 * (1 - dx) + v10 * dx);
        float v1 = (float) (v01 * (1 - dx) + v11 * dx);

        return (float) (v0 * (1 - dy) + v1 * dy);
    }
}