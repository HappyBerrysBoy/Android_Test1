package com.unitloadsystem.mixed;

import java.io.Serializable;

public final class PalletSpec implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final int widthMm;
    private final int lengthMm;
    private final int heightMm;

    public PalletSpec(String id, String name, int widthMm, int lengthMm, int heightMm) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.widthMm = requirePositive(widthMm, "widthMm");
        this.lengthMm = requirePositive(lengthMm, "lengthMm");
        this.heightMm = requirePositive(heightMm, "heightMm");
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

    public int getFootprintAreaMm2() {
        return widthMm * lengthMm;
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
