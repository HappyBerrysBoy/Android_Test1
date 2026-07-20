package com.unitloadsystem.mixed;

import java.io.Serializable;

public final class BoxDemand implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final int widthMm;
    private final int lengthMm;
    private final int heightMm;
    private final double weightKg;
    private final int quantity;
    private final int maxStackLayers;

    public BoxDemand(String id, String name, int widthMm, int lengthMm,
                     int heightMm, double weightKg, int quantity) {
        this(id, name, widthMm, lengthMm, heightMm, weightKg, quantity, 10000);
    }

    public BoxDemand(String id, String name, int widthMm, int lengthMm,
                     int heightMm, double weightKg, int quantity, int maxStackLayers) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.widthMm = requirePositive(widthMm, "widthMm");
        this.lengthMm = requirePositive(lengthMm, "lengthMm");
        this.heightMm = requirePositive(heightMm, "heightMm");
        if (weightKg < 0d) {
            throw new IllegalArgumentException("weightKg must be >= 0");
        }
        this.weightKg = weightKg;
        this.quantity = requirePositive(quantity, "quantity");
        this.maxStackLayers = requirePositive(maxStackLayers, "maxStackLayers");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getWidthMm() {
        return widthMm;
    }

    public int getLengthMm() {
        return lengthMm;
    }

    public int getHeightMm() {
        return heightMm;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getMaxStackLayers() {
        return maxStackLayers;
    }

    public int getMaxStackHeightMm() {
        return heightMm * maxStackLayers;
    }

    public int getFootprintAreaMm2() {
        return widthMm * lengthMm;
    }

    public long getVolumeMm3() {
        return (long) widthMm * (long) lengthMm * (long) heightMm;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be > 0");
        }
        return value;
    }
}
