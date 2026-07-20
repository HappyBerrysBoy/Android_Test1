package com.unitloadsystem.activitys;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.View;

import com.unitloadsystem.mixed.PalletSpec;
import com.unitloadsystem.mixed.Placement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mixed3DView extends View {
    private static final float ISO_X = 0.866f;
    private static final float ISO_Y = 0.42f;
    private static final float ISO_Z = 0.58f;
    private static final int[] BOX_COLORS = {
            Color.rgb(0, 114, 178), Color.rgb(213, 94, 0), Color.rgb(0, 121, 92),
            Color.rgb(167, 87, 145), Color.rgb(216, 143, 0), Color.rgb(74, 149, 181)
    };

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path facePath = new Path();
    private final ArrayList<DrawBox> boxes = new ArrayList<DrawBox>();
    private final ArrayList<DrawFace> faces = new ArrayList<DrawFace>();
    private final Map<String, Integer> typeIndices = new HashMap<String, Integer>();
    private final float[] pointX = new float[8];
    private final float[] pointY = new float[8];

    private float palletWidth = 1f;
    private float palletLength = 1f;
    private float palletHeight = 1f;
    private float loadedCargoHeight = 1f;
    private float rate = 1f;
    private float originX;
    private float originY;

    public Mixed3DView(Context context) {
        super(context);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(0.8f));
        strokePaint.setColor(Color.rgb(27, 43, 54));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        setPadding(dp(8), dp(10), dp(8), dp(12));
    }

    public void setLoad(PalletSpec pallet, List<Placement> placements,
                        Map<String, Integer> typeIndices, String contentDescription) {
        palletWidth = Math.max(1, pallet.getWidthMm());
        palletLength = Math.max(1, pallet.getLengthMm());
        palletHeight = Math.max(1, pallet.getHeightMm());
        this.typeIndices.clear();
        this.typeIndices.putAll(typeIndices);
        boxes.clear();

        float maxTop = palletHeight;
        for (Placement placement : placements) {
            maxTop = Math.max(maxTop, placement.getZMm() + placement.getHeightMm());
            boxes.add(new DrawBox(placement));
        }
        loadedCargoHeight = Math.max(1f, maxTop - palletHeight);
        markColumnTops();
        rebuildFaces();
        setContentDescription(contentDescription);
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width <= 0) {
            width = getResources().getDisplayMetrics().widthPixels;
        }
        float availableWidth = Math.max(1, width - getPaddingLeft() - getPaddingRight());
        float maxHeight = dp(300);
        float widthRate = availableWidth / Math.max(1f,
                (palletWidth + palletLength) * ISO_X);
        float heightRate = maxHeight / Math.max(1f,
                (palletWidth + palletLength) * ISO_Y
                        + loadedCargoHeight * ISO_Z + palletHeight * ISO_Z);
        rate = Math.min(widthRate, heightRate);
        int desiredHeight = Math.round(((palletWidth + palletLength) * ISO_Y
                + loadedCargoHeight * ISO_Z + palletHeight * ISO_Z) * rate)
                + getPaddingTop() + getPaddingBottom();
        desiredHeight = Math.max(dp(190), Math.min(dp(320), desiredHeight));
        setMeasuredDimension(width, resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float projectedWidth = (palletWidth + palletLength) * ISO_X * rate;
        originX = (getWidth() - projectedWidth) / 2f + palletLength * ISO_X * rate;
        originY = getPaddingTop() + loadedCargoHeight * ISO_Z * rate;
        drawPallet(canvas);
        for (DrawFace face : faces) {
            drawFace(canvas, face);
        }
    }

    private void drawPallet(Canvas canvas) {
        project(0, 0, 0, 0);
        project(palletWidth, 0, 0, 1);
        project(palletWidth, palletLength, 0, 2);
        project(0, palletLength, 0, 3);
        project(palletWidth, 0, -palletHeight, 4);
        project(palletWidth, palletLength, -palletHeight, 5);
        project(0, palletLength, -palletHeight, 6);
        drawFace(canvas, adjustColor(getResources().getColor(R.color.resultPalletSurface), 0.78f),
                3, 2, 5, 6);
        drawFace(canvas, getResources().getColor(R.color.resultPalletBorder), 1, 2, 5, 4);
        drawFace(canvas, getResources().getColor(R.color.resultPalletSurface), 0, 1, 2, 3);
    }

    private void drawFace(Canvas canvas, DrawFace face) {
        DrawBox box = face.box;
        Placement placement = box.placement;
        float bottom = placement.getZMm() - palletHeight;
        float top = bottom + placement.getHeightMm();
        float left = placement.getXMm();
        float back = placement.getYMm();
        float right = left + placement.getWidthMm();
        float front = back + placement.getLengthMm();
        project(left, back, top, 0);
        project(right, back, top, 1);
        project(right, front, top, 2);
        project(left, front, top, 3);
        project(right, back, bottom, 4);
        project(right, front, bottom, 5);
        project(left, front, bottom, 6);

        drawFace(canvas, face.color, face.indices);
        if (face.type == FACE_TOP && box.topOfColumn) {
            float centerX = (pointX[0] + pointX[1] + pointX[2] + pointX[3]) / 4f;
            float centerY = (pointY[0] + pointY[1] + pointY[2] + pointY[3]) / 4f;
            float topWidth = Math.abs(pointX[1] - pointX[0]);
            textPaint.setTextSize(Math.max(dp(8), Math.min(dp(13), topWidth * 0.22f)));
            canvas.drawText(placement.getBoxId(), centerX, centerY + dp(4), textPaint);
        }
    }

    private void rebuildFaces() {
        faces.clear();
        for (DrawBox box : boxes) {
            Placement placement = box.placement;
            int typeIndex = typeIndices.containsKey(placement.getBoxId())
                    ? typeIndices.get(placement.getBoxId()) : 0;
            int color = BOX_COLORS[Math.floorMod(typeIndex, BOX_COLORS.length)];
            float bottom = placement.getZMm() - palletHeight;
            float top = bottom + placement.getHeightMm();
            float left = placement.getXMm();
            float back = placement.getYMm();
            float right = left + placement.getWidthMm();
            float front = back + placement.getLengthMm();

            addFace(box, FACE_RIGHT, adjustColor(color, 0.70f),
                    averageDepth(right, back, bottom, right, front, top),
                    1, 2, 5, 4);
            addFace(box, FACE_FRONT, adjustColor(color, 0.84f),
                    averageDepth(left, front, bottom, right, front, top),
                    3, 2, 5, 6);
            addFace(box, FACE_TOP, adjustColor(color, 1.10f),
                    averageDepth(left, back, top, right, front, top),
                    0, 1, 2, 3);
        }
        Collections.sort(faces, FACE_DRAW_ORDER);
    }

    private void addFace(DrawBox box, int type, int color, float depth, int... indices) {
        faces.add(new DrawFace(box, type, color, depth, indices));
    }

    private float averageDepth(float x1, float y1, float z1,
                               float x2, float y2, float z2) {
        float x = (x1 + x2) / 2f;
        float y = (y1 + y2) / 2f;
        float z = (z1 + z2) / 2f;
        // The viewer is above the positive X/Y corner, so nearer faces have a larger depth.
        return (x + y) * 0.69f + z;
    }

    private void drawFace(Canvas canvas, int color, int... indices) {
        facePath.reset();
        facePath.moveTo(pointX[indices[0]], pointY[indices[0]]);
        for (int i = 1; i < indices.length; i++) {
            facePath.lineTo(pointX[indices[i]], pointY[indices[i]]);
        }
        facePath.close();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(color);
        canvas.drawPath(facePath, fillPaint);
        canvas.drawPath(facePath, strokePaint);
    }

    private void project(float x, float y, float z, int index) {
        pointX[index] = originX + (x - y) * ISO_X * rate;
        pointY[index] = originY + (x + y) * ISO_Y * rate - z * ISO_Z * rate;
    }

    private void markColumnTops() {
        for (DrawBox candidate : boxes) {
            Placement placement = candidate.placement;
            boolean hasBoxAbove = false;
            int top = placement.getZMm() + placement.getHeightMm();
            for (DrawBox other : boxes) {
                Placement above = other.placement;
                if (above != placement
                        && above.getXMm() == placement.getXMm()
                        && above.getYMm() == placement.getYMm()
                        && above.getWidthMm() == placement.getWidthMm()
                        && above.getLengthMm() == placement.getLengthMm()
                        && above.getZMm() >= top) {
                    hasBoxAbove = true;
                    break;
                }
            }
            candidate.topOfColumn = !hasBoxAbove;
        }
    }

    private static int adjustColor(int color, float factor) {
        return Color.rgb(
                Math.min(255, Math.max(0, Math.round(Color.red(color) * factor))),
                Math.min(255, Math.max(0, Math.round(Color.green(color) * factor))),
                Math.min(255, Math.max(0, Math.round(Color.blue(color) * factor))));
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final int FACE_RIGHT = 0;
    private static final int FACE_FRONT = 1;
    private static final int FACE_TOP = 2;

    private static final Comparator<DrawFace> FACE_DRAW_ORDER = new Comparator<DrawFace>() {
        @Override
        public int compare(DrawFace left, DrawFace right) {
            int depth = Float.compare(left.depth, right.depth);
            if (depth != 0) {
                return depth;
            }
            int type = Integer.compare(left.type, right.type);
            if (type != 0) {
                return type;
            }
            Placement first = left.box.placement;
            Placement second = right.box.placement;
            int x = Integer.compare(first.getXMm(), second.getXMm());
            if (x != 0) {
                return x;
            }
            int y = Integer.compare(first.getYMm(), second.getYMm());
            if (y != 0) {
                return y;
            }
            return first.getBoxId().compareTo(second.getBoxId());
        }
    };

    private static final class DrawBox {
        final Placement placement;
        boolean topOfColumn;

        DrawBox(Placement placement) {
            this.placement = placement;
        }
    }

    private static final class DrawFace {
        final DrawBox box;
        final int type;
        final int color;
        final float depth;
        final int[] indices;

        DrawFace(DrawBox box, int type, int color, float depth, int[] indices) {
            this.box = box;
            this.type = type;
            this.color = color;
            this.depth = depth;
            this.indices = indices;
        }
    }
}
