package com.astro;

public class Star {
    private final double x;
    private final double y;
    private final double brightness;
    private final double flux;

    public Star(double x, double y, double brightness, double flux) {
        this.x = x;
        this.y = y;
        this.brightness = brightness;
        this.flux = flux;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getBrightness() {
        return brightness;
    }

    public double getFlux() {
        return flux;
    }

    public double distanceTo(Star other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return String.format("Star[x=%.2f, y=%.2f, brightness=%.2f]", x, y, brightness);
    }
}
