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
    private double offsetX = 0;
    private double offsetY = 0;

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

    // Conversion des tableaux 1D en 2D
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

    public double getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    public FitsImage createAlignedCopy() {
        try {
            FitsImage copy = new FitsImage(this.path);
            copy.data = new float[height][width];
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int srcX = (int) Math.round(x + offsetX);
                    int srcY = (int) Math.round(y + offsetY);
                    
                    if (srcX >= 0 && srcX < width && srcY >= 0 && srcY < height) {
                        copy.data[y][x] = this.data[srcY][srcX];
                    } else {
                        copy.data[y][x] = 0;
                    }
                }
            }
            
            return copy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
