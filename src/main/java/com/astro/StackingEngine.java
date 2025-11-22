package com.astro;

import java.util.*;

public class StackingEngine {
    
    public enum StackingMethod {
        AVERAGE("Moyenne"),
        MEDIAN("Médiane"),
        SIGMA_CLIP("Sigma Clipping"),
        MAXIMUM("Maximum"),
        MINIMUM("Minimum"),
        WEIGHTED_AVERAGE("Moyenne Pondérée");

        private final String displayName;

        StackingMethod(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static FitsImage stackImages(List<FitsImage> images, StackingMethod method, 
                                       ImageAligner.ProgressCallback callback) throws Exception {
        if (images.isEmpty()) {
            throw new IllegalArgumentException("Aucune image à empiler");
        }

        // Get dimensions from first image
        int width = images.get(0).getWidth();
        int height = images.get(0).getHeight();

        // Create aligned copies
        List<FitsImage> alignedImages = new ArrayList<>();
        for (FitsImage img : images) {
            alignedImages.add(img.createAlignedCopy());
        }

        // Create result image
        float[][] resultData = new float[height][width];

        int totalPixels = width * height;
        int processedPixels = 0;

        // Stack pixel by pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float stackedValue = stackPixel(alignedImages, x, y, method);
                resultData[y][x] = stackedValue;

                processedPixels++;
                if (callback != null && processedPixels % 10000 == 0) {
                    int progress = (int) ((processedPixels * 100.0) / totalPixels);
                    callback.onProgress(progress, "Empilement: " + progress + "%");
                }
            }
        }

        if (callback != null) {
            callback.onProgress(100, "Empilement terminé!");
        }

        // Create result FITS image
        FitsImage result = images.get(0);
        float[][] originalData = result.getData();
        System.arraycopy(resultData, 0, originalData, 0, resultData.length);

        return result;
    }

    private static float stackPixel(List<FitsImage> images, int x, int y, StackingMethod method) {
        return switch (method) {
            case AVERAGE -> stackAverage(images, x, y);
            case MEDIAN -> stackMedian(images, x, y);
            case SIGMA_CLIP -> stackSigmaClip(images, x, y);
            case MAXIMUM -> stackMaximum(images, x, y);
            case MINIMUM -> stackMinimum(images, x, y);
            case WEIGHTED_AVERAGE -> stackWeightedAverage(images, x, y);
        };
    }

    private static float stackAverage(List<FitsImage> images, int x, int y) {
        double sum = 0;
        int count = 0;

        for (FitsImage image : images) {
            float value = image.getPixel(x, y);
            if (value > 0) {
                sum += value;
                count++;
            }
        }

        return count > 0 ? (float) (sum / count) : 0;
    }

    private static float stackMedian(List<FitsImage> images, int x, int y) {
        List<Float> values = new ArrayList<>();

        for (FitsImage image : images) {
            float value = image.getPixel(x, y);
            if (value > 0) {
                values.add(value);
            }
        }

        if (values.isEmpty()) return 0;

        Collections.sort(values);
        int size = values.size();

        if (size % 2 == 0) {
            return (values.get(size / 2 - 1) + values.get(size / 2)) / 2;
        } else {
            return values.get(size / 2);
        }
    }

    private static float stackSigmaClip(List<FitsImage> images, int x, int y) {
        List<Float> values = new ArrayList<>();

        for (FitsImage image : images) {
            float value = image.getPixel(x, y);
            if (value > 0) {
                values.add(value);
            }
        }

        if (values.isEmpty()) return 0;
        if (values.size() < 4) return stackAverage(images, x, y);

        // Calculate mean and standard deviation
        double mean = 0;
        for (float value : values) {
            mean += value;
        }
        mean /= values.size();

        double variance = 0;
        for (float value : values) {
            double diff = value - mean;
            variance += diff * diff;
        }
        double stdDev = Math.sqrt(variance / values.size());

        // Remove outliers (values more than 2 sigma away)
        List<Float> clipped = new ArrayList<>();
        double lowerBound = mean - 2 * stdDev;
        double upperBound = mean + 2 * stdDev;

        for (float value : values) {
            if (value >= lowerBound && value <= upperBound) {
                clipped.add(value);
            }
        }

        if (clipped.isEmpty()) return (float) mean;

        // Calculate average of clipped values
        double sum = 0;
        for (float value : clipped) {
            sum += value;
        }

        return (float) (sum / clipped.size());
    }

    private static float stackMaximum(List<FitsImage> images, int x, int y) {
        float max = Float.MIN_VALUE;

        for (FitsImage image : images) {
            float value = image.getPixel(x, y);
            if (value > max) {
                max = value;
            }
        }

        return max == Float.MIN_VALUE ? 0 : max;
    }

    private static float stackMinimum(List<FitsImage> images, int x, int y) {
        float min = Float.MAX_VALUE;

        for (FitsImage image : images) {
            float value = image.getPixel(x, y);
            if (value > 0 && value < min) {
                min = value;
            }
        }

        return min == Float.MAX_VALUE ? 0 : min;
    }

    private static float stackWeightedAverage(List<FitsImage> images, int x, int y) {
        double weightedSum = 0;
        double totalWeight = 0;

        for (int i = 0; i < images.size(); i++) {
            FitsImage image = images.get(i);
            float value = image.getPixel(x, y);
            
            if (value > 0) {
                // Weight decreases with noise (we approximate quality by image order)
                double weight = 1.0 / (1.0 + i * 0.1);
                weightedSum += value * weight;
                totalWeight += weight;
            }
        }

        return totalWeight > 0 ? (float) (weightedSum / totalWeight) : 0;
    }
}
