package com.unitloadsystem.mixed;

import java.io.Serializable;

public final class Placement implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String boxId;
    private final String boxName;
    private final int widthMm;
    private final int lengthMm;
    private final int heightMm;
    private final double weightKg;
    private final int xMm;
    private final int yMm;
    private final int zMm;
    private final boolean rotated;

    public Placement(String boxId, String boxName, int widthMm, int lengthMm,
                     int heightMm, double weightKg,
                     int xMm, int yMm, int zMm, boolean rotated) {
        this.boxId = boxId;
        this.boxName = boxName;
        this.widthMm = widthMm;
        this.lengthMm = lengthMm;
        this.heightMm = heightMm;
        this.weightKg = weightKg;
        this.xMm = xMm;
        this.yMm = yMm;
        this.zMm = zMm;
        this.rotated = rotated;
    }

    public String getBoxId() {
        return boxId;
    }

    public String getBoxName() {
        return boxName;
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

    public int getXMm() {
        return xMm;
    }

    public int getYMm() {
        return yMm;
    }

    public int getZMm() {
        return zMm;
    }

    public boolean isRotated() {
        return rotated;
    }
}
