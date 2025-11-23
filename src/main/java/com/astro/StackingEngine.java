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

        // Vérifier si on travaille avec des images couleur ou mono
        boolean isColor = images.get(0).isColor();

        System.out.println("=== Début de l'empilement ===");
        System.out.println("Nombre d'images: " + images.size());
        System.out.println("Type: " + (isColor ? "RGB" : "Mono"));
        System.out.println("Méthode: " + method.getDisplayName());

        // Récupérer les informations du canvas depuis la première image
        ImageAligner.CanvasInfo canvasInfo = images.get(0).getCanvasInfo();
        if (canvasInfo == null) {
            // Fallback si pas d'info de canvas (alignement non effectué)
            canvasInfo = new ImageAligner.CanvasInfo(
                    images.get(0).getWidth(),
                    images.get(0).getHeight(),
                    0, 0
            );
        }

        int canvasWidth = canvasInfo.width;
        int canvasHeight = canvasInfo.height;
        int offsetX = canvasInfo.offsetX;
        int offsetY = canvasInfo.offsetY;

        System.out.println("Canvas: " + canvasWidth + "x" + canvasHeight);
        System.out.println("Offset: " + offsetX + ", " + offsetY);

        // Create aligned copies with expanded canvas
        if (callback != null) {
            callback.onProgress(0, "Création des copies alignées...");
        }

        List<FitsImage> alignedImages = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            FitsImage img = images.get(i);

            System.out.println("Alignement image " + (i+1) + ": " + img.getFileName());
            System.out.println("  Transformation: rot=" + Math.toDegrees(img.getTransform().rotation) +
                    "°, scale=" + img.getTransform().scale +
                    ", tx=" + img.getTransform().tx + ", ty=" + img.getTransform().ty);

            FitsImage aligned = img.createAlignedCopy(canvasWidth, canvasHeight, offsetX, offsetY);
            alignedImages.add(aligned);

            // Vérification: compter les pixels non-nuls
            int nonZeroPixels = 0;
            for (int y = 0; y < canvasHeight; y++) {
                for (int x = 0; x < canvasWidth; x++) {
                    if (aligned.getPixel(x, y) > 0) {
                        nonZeroPixels++;
                    }
                }
            }
            System.out.println("  Pixels non-nuls: " + nonZeroPixels + " / " + (canvasWidth*canvasHeight) +
                    " (" + (100.0*nonZeroPixels/(canvasWidth*canvasHeight)) + "%)");

            if (callback != null) {
                int progress = (int) ((i * 20.0) / images.size());
                callback.onProgress(progress, "Alignement image " + (i + 1) + "/" + images.size());
            }
        }

        // Create result image
        FitsImage result = FitsImage.createEmpty(
                images.get(0).getPath(),
                canvasWidth,
                canvasHeight,
                isColor
        );

        int totalPixels = canvasWidth * canvasHeight;
        int processedPixels = 0;

        if (isColor) {
            // Stack each channel separately
            for (int c = 0; c < 3; c++) {
                String channelName = c == 0 ? "Rouge" : (c == 1 ? "Vert" : "Bleu");
                if (callback != null) {
                    callback.onProgress(20 + c * 25, "Empilement canal " + channelName + "...");
                }

                for (int y = 0; y < canvasHeight; y++) {
                    for (int x = 0; x < canvasWidth; x++) {
                        float stackedValue = stackPixelChannel(alignedImages, x, y, c, method);
                        result.setPixel(c, x, y, stackedValue);

                        processedPixels++;
                        if (callback != null && processedPixels % 50000 == 0) {
                            int progress = 20 + c * 25 + (int) (((double) processedPixels / totalPixels) * 25);
                            callback.onProgress(progress, "Canal " + channelName + ": " +
                                    (processedPixels * 100 / totalPixels) + "%");
                        }
                    }
                }
            }

            // Update mono version (green channel)
            for (int y = 0; y < canvasHeight; y++) {
                for (int x = 0; x < canvasWidth; x++) {
                    result.setPixel(x, y, result.getPixel(1, x, y));
                }
            }
        } else {
            // Stack mono image
            if (callback != null) {
                callback.onProgress(20, "Empilement image mono...");
            }

            for (int y = 0; y < canvasHeight; y++) {
                for (int x = 0; x < canvasWidth; x++) {
                    float stackedValue = stackPixel(alignedImages, x, y, method);
                    result.setPixel(x, y, stackedValue);

                    processedPixels++;
                    if (callback != null && processedPixels % 10000 == 0) {
                        int progress = 20 + (int) ((processedPixels * 75.0) / totalPixels);
                        callback.onProgress(progress, "Empilement: " +
                                (processedPixels * 100 / totalPixels) + "%");
                    }
                }
            }
        }

        // Vérification du résultat
        System.out.println("=== Vérification du résultat ===");
        float minVal = Float.MAX_VALUE;
        float maxVal = Float.MIN_VALUE;
        double sumVal = 0;
        int nonZeroCount = 0;

        for (int y = 0; y < canvasHeight; y++) {
            for (int x = 0; x < canvasWidth; x++) {
                float val = result.getPixel(x, y);
                if (val > 0) {
                    nonZeroCount++;
                    minVal = Math.min(minVal, val);
                    maxVal = Math.max(maxVal, val);
                    sumVal += val;
                }
            }
        }

        System.out.println("Pixels non-nuls: " + nonZeroCount + " / " + totalPixels);
        System.out.println("Min: " + minVal + ", Max: " + maxVal + ", Moyenne: " + (sumVal/nonZeroCount));
        System.out.println("=== Empilement terminé ===");

        if (callback != null) {
            callback.onProgress(100, "Empilement terminé!");
        }

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

    private static float stackPixelChannel(List<FitsImage> images, int x, int y, int channel, StackingMethod method) {
        return switch (method) {
            case AVERAGE -> stackAverageChannel(images, x, y, channel);
            case MEDIAN -> stackMedianChannel(images, x, y, channel);
            case SIGMA_CLIP -> stackSigmaClipChannel(images, x, y, channel);
            case MAXIMUM -> stackMaximumChannel(images, x, y, channel);
            case MINIMUM -> stackMinimumChannel(images, x, y, channel);
            case WEIGHTED_AVERAGE -> stackWeightedAverageChannel(images, x, y, channel);
        };
    }

    // ========== Méthodes pour images mono ==========

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
                double weight = 1.0 / (1.0 + i * 0.1);
                weightedSum += value * weight;
                totalWeight += weight;
            }
        }

        return totalWeight > 0 ? (float) (weightedSum / totalWeight) : 0;
    }

    // ========== Méthodes pour images RGB (par canal) ==========

    private static float stackAverageChannel(List<FitsImage> images, int x, int y, int channel) {
        double sum = 0;
        int count = 0;

        for (FitsImage image : images) {
            float value = image.getPixel(channel, x, y);
            if (value > 0) {
                sum += value;
                count++;
            }
        }

        return count > 0 ? (float) (sum / count) : 0;
    }

    private static float stackMedianChannel(List<FitsImage> images, int x, int y, int channel) {
        List<Float> values = new ArrayList<>();

        for (FitsImage image : images) {
            float value = image.getPixel(channel, x, y);
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

    private static float stackSigmaClipChannel(List<FitsImage> images, int x, int y, int channel) {
        List<Float> values = new ArrayList<>();

        for (FitsImage image : images) {
            float value = image.getPixel(channel, x, y);
            if (value > 0) {
                values.add(value);
            }
        }

        if (values.isEmpty()) return 0;
        if (values.size() < 4) return stackAverageChannel(images, x, y, channel);

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

        List<Float> clipped = new ArrayList<>();
        double lowerBound = mean - 2 * stdDev;
        double upperBound = mean + 2 * stdDev;

        for (float value : values) {
            if (value >= lowerBound && value <= upperBound) {
                clipped.add(value);
            }
        }

        if (clipped.isEmpty()) return (float) mean;

        double sum = 0;
        for (float value : clipped) {
            sum += value;
        }

        return (float) (sum / clipped.size());
    }

    private static float stackMaximumChannel(List<FitsImage> images, int x, int y, int channel) {
        float max = Float.MIN_VALUE;

        for (FitsImage image : images) {
            float value = image.getPixel(channel, x, y);
            if (value > max) {
                max = value;
            }
        }

        return max == Float.MIN_VALUE ? 0 : max;
    }

    private static float stackMinimumChannel(List<FitsImage> images, int x, int y, int channel) {
        float min = Float.MAX_VALUE;

        for (FitsImage image : images) {
            float value = image.getPixel(channel, x, y);
            if (value > 0 && value < min) {
                min = value;
            }
        }

        return min == Float.MAX_VALUE ? 0 : min;
    }

    private static float stackWeightedAverageChannel(List<FitsImage> images, int x, int y, int channel) {
        double weightedSum = 0;
        double totalWeight = 0;

        for (int i = 0; i < images.size(); i++) {
            FitsImage image = images.get(i);
            float value = image.getPixel(channel, x, y);

            if (value > 0) {
                double weight = 1.0 / (1.0 + i * 0.1);
                weightedSum += value * weight;
                totalWeight += weight;
            }
        }

        return totalWeight > 0 ? (float) (weightedSum / totalWeight) : 0;
    }
}