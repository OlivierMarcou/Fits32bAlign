package com.astro;

import java.util.*;

public class StarDetector {
    private static final int MIN_STAR_SIZE = 3;
    private static final int MAX_STAR_SIZE = 30;
    private static final double THRESHOLD_MULTIPLIER = 3.0;

    public static List<Star> detectStars(FitsImage image, int maxStars) {
        float[][] data = image.getData();
        int width = image.getWidth();
        int height = image.getHeight();

        // Calculate background statistics
        double mean = calculateMean(data);
        double stdDev = calculateStdDev(data, mean);
        double threshold = mean + THRESHOLD_MULTIPLIER * stdDev;

        // Find local maxima
        List<Star> stars = new ArrayList<>();
        boolean[][] processed = new boolean[height][width];

        for (int y = MAX_STAR_SIZE; y < height - MAX_STAR_SIZE; y++) {
            for (int x = MAX_STAR_SIZE; x < width - MAX_STAR_SIZE; x++) {
                if (processed[y][x]) continue;

                float value = data[y][x];
                if (value > threshold) {
                    // Check if it's a local maximum
                    if (isLocalMaximum(data, x, y, 3)) {
                        // Calculate centroid
                        StarProperties props = calculateStarProperties(data, x, y, threshold);
                        if (props != null && props.flux > 0) {
                            stars.add(new Star(props.centerX, props.centerY, value, props.flux));
                            markProcessed(processed, (int) props.centerX, (int) props.centerY, 5);
                        }
                    }
                }
            }
        }

        // Sort by brightness and keep the brightest stars
        stars.sort((a, b) -> Double.compare(b.getFlux(), a.getFlux()));
        
        if (stars.size() > maxStars) {
            stars = stars.subList(0, maxStars);
        }

        return stars;
    }

    private static boolean isLocalMaximum(float[][] data, int x, int y, int radius) {
        float centerValue = data[y][x];
        
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx == 0 && dy == 0) continue;
                
                int ny = y + dy;
                int nx = x + dx;
                
                if (ny >= 0 && ny < data.length && nx >= 0 && nx < data[0].length) {
                    if (data[ny][nx] > centerValue) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }

    private static StarProperties calculateStarProperties(float[][] data, int seedX, int seedY, double threshold) {
        int height = data.length;
        int width = data[0].length;

        double sumX = 0;
        double sumY = 0;
        double sumFlux = 0;
        int count = 0;

        // Use a small window around the seed point
        int radius = 10;
        
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int y = seedY + dy;
                int x = seedX + dx;
                
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    float value = data[y][x];
                    if (value > threshold * 0.5) { // Lower threshold for star extent
                        double weight = value - threshold * 0.5;
                        sumX += x * weight;
                        sumY += y * weight;
                        sumFlux += weight;
                        count++;
                    }
                }
            }
        }

        if (count >= MIN_STAR_SIZE && count <= MAX_STAR_SIZE * MAX_STAR_SIZE) {
            double centerX = sumX / sumFlux;
            double centerY = sumY / sumFlux;
            return new StarProperties(centerX, centerY, sumFlux);
        }

        return null;
    }

    private static void markProcessed(boolean[][] processed, int x, int y, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int ny = y + dy;
                int nx = x + dx;
                if (ny >= 0 && ny < processed.length && nx >= 0 && nx < processed[0].length) {
                    processed[ny][nx] = true;
                }
            }
        }
    }

    private static double calculateMean(float[][] data) {
        double sum = 0;
        int count = 0;
        
        for (float[] row : data) {
            for (float value : row) {
                sum += value;
                count++;
            }
        }
        
        return sum / count;
    }

    private static double calculateStdDev(float[][] data, double mean) {
        double sumSquares = 0;
        int count = 0;
        
        for (float[] row : data) {
            for (float value : row) {
                double diff = value - mean;
                sumSquares += diff * diff;
                count++;
            }
        }
        
        return Math.sqrt(sumSquares / count);
    }

    private static class StarProperties {
        final double centerX;
        final double centerY;
        final double flux;

        StarProperties(double centerX, double centerY, double flux) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.flux = flux;
        }
    }
}
