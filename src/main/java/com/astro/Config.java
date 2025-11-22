package com.astro;

/**
 * Configuration globale pour l'application FITS Stacker
 */
public class Config {
    
    // Détection d'étoiles
    public static final int DEFAULT_MAX_STARS = 100;
    public static final double STAR_THRESHOLD_MULTIPLIER = 3.0;
    public static final int MIN_STAR_SIZE = 3;
    public static final int MAX_STAR_SIZE = 30;
    
    // Alignement
    public static final int MIN_MATCHING_STARS = 10;
    public static final double MAX_DISTANCE_TOLERANCE = 5.0;
    public static final double TRIANGLE_MATCH_TOLERANCE = 0.1;
    public static final int MAX_TRIANGLE_STARS = 20;
    
    // Sigma Clipping
    public static final double SIGMA_CLIP_THRESHOLD = 2.0;
    public static final int MIN_VALUES_FOR_SIGMA_CLIP = 4;
    
    // Interface
    public static final int WINDOW_WIDTH = 1000;
    public static final int WINDOW_HEIGHT = 700;
    public static final int PROGRESS_UPDATE_INTERVAL = 10000; // pixels
    
    // Couleurs interface
    public static final java.awt.Color TITLE_BG_COLOR = new java.awt.Color(40, 40, 50);
    public static final java.awt.Color TITLE_FG_COLOR = new java.awt.Color(100, 180, 255);
    public static final java.awt.Color SUBTITLE_COLOR = new java.awt.Color(180, 180, 180);
    public static final java.awt.Color BORDER_COLOR = new java.awt.Color(70, 70, 80);
    public static final java.awt.Color ALIGN_BUTTON_COLOR = new java.awt.Color(70, 130, 180);
    public static final java.awt.Color STACK_BUTTON_COLOR = new java.awt.Color(60, 150, 90);
    
    // Performance
    public static final int RECOMMENDED_MEMORY_MB = 4096;
    
    private Config() {
        // Classe utilitaire, pas d'instanciation
    }
    
    /**
     * Retourne une description des réglages actuels
     */
    public static String getConfigSummary() {
        return String.format("""
            Configuration FITS Stacker:
            - Étoiles détectées max: %d
            - Seuil détection: %.1fσ
            - Étoiles min pour alignement: %d
            - Tolérance alignement: %.1f pixels
            - Sigma clipping: %.1fσ
            """, 
            DEFAULT_MAX_STARS,
            STAR_THRESHOLD_MULTIPLIER,
            MIN_MATCHING_STARS,
            MAX_DISTANCE_TOLERANCE,
            SIGMA_CLIP_THRESHOLD
        );
    }
}
