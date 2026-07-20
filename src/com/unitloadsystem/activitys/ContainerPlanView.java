package com.unitloadsystem.activitys;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.View;

import com.unitloadsystem.container.ContainerLoadCalculator;
import com.unitloadsystem.container.ContainerLoadPlanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ContainerPlanView extends View {
    private static final float ISO_X = 0.866f;
    private static final float ISO_Y = 0.42f;
    private static final float ISO_Z = 0.58f;
    private static final int[] LOAD_COLORS = {
            Color.rgb(0, 114, 178), Color.rgb(213, 94, 0), Color.rgb(0, 121, 92),
            Color.rgb(167, 87, 145), Color.rgb(216, 143, 0), Color.rgb(74, 149, 181)
    };

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float[] pointX = new float[8];
    private final float[] pointY = new float[8];
    private final List<ContainerLoadPlanner.Placement> placements =
            new ArrayList<ContainerLoadPlanner.Placement>();

    private ContainerLoadCalculator.ContainerType containerType;
    private float rate = 1f;
    private float originX;
    private float originY;

    public ContainerPlanView(Context context) {
        super(context);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(0.8f));
        strokePaint.setColor(Color.rgb(31, 48, 60));
        shellPaint.setStyle(Paint.Style.STROKE);
        shellPaint.setStrokeWidth(dp(1.3f));
        shellPaint.setColor(Color.rgb(79, 103, 118));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        setPadding(dp(8), dp(12), dp(8), dp(14));
    }

    public void setPlan(ContainerLoadCalculator.ContainerType containerType,
                        ContainerLoadPlanner.ContainerPlan plan,
                        String contentDescription) {
        this.containerType = containerType;
        placements.clear();
        placements.addAll(plan.getPlacements());
        Collections.sort(placements, DRAW_ORDER);
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
        if (containerType == null) {
            setMeasuredDimension(width, resolveSize(dp(190), heightMeasureSpec));
            return;
        }
        float availableWidth = Math.max(1f, width - getPaddingLeft() - getPaddingRight());
        float projectedWidth = (containerType.widthMm + containerType.lengthMm) * ISO_X;
        float projectedHeight = (containerType.widthMm + containerType.lengthMm) * ISO_Y
                + containerType.heightMm * ISO_Z;
        rate = Math.min(availableWidth / projectedWidth, dp(250) / projectedHeight);
        int desiredHeight = Math.round(projectedHeight * rate)
                + getPaddingTop() + getPaddingBottom();
        desiredHeight = Math.max(dp(180), Math.min(dp(278), desiredHeight));
        setMeasuredDimension(width, resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (containerType == null) {
            return;
        }
        float projectedWidth = (containerType.widthMm + containerType.lengthMm)
                * ISO_X * rate;
        originX = (getWidth() - projectedWidth) / 2f
                + containerType.lengthMm * ISO_X * rate;
        originY = getPaddingTop() + containerType.heightMm * ISO_Z * rate;

        drawFloor(canvas);
        for (int i = 0; i < placements.size(); i++) {
            drawPlacement(canvas, placements.get(i));
        }
        drawContainerShell(canvas);
    }

    private void drawFloor(Canvas canvas) {
        project(0, 0, 0, 0);
        project(containerType.widthMm, 0, 0, 1);
        project(containerType.widthMm, containerType.lengthMm, 0, 2);
        project(0, containerType.lengthMm, 0, 3);
        drawFace(canvas, Color.rgb(226, 234, 238), 0, 1, 2, 3);
    }

    private void drawPlacement(Canvas canvas, ContainerLoadPlanner.Placement placement) {
        float bottom = 0f;
        List<ContainerLoadPlanner.UnitLoad> tiers = placement.getTiers();
        for (int i = 0; i < tiers.size(); i++) {
            ContainerLoadPlanner.UnitLoad tier = tiers.get(i);
            float top = bottom + tier.getHeightMm();
            int color = LOAD_COLORS[Math.floorMod(tier.getLabel().hashCode(), LOAD_COLORS.length)];
            drawCuboid(canvas, placement.getXMm(), placement.getYMm(), bottom,
                    placement.getWidthMm(), placement.getLengthMm(), top - bottom, color);
            if (i == tiers.size() - 1) {
                float centerX = (pointX[0] + pointX[1] + pointX[2] + pointX[3]) / 4f;
                float centerY = (pointY[0] + pointY[1] + pointY[2] + pointY[3]) / 4f;
                textPaint.setTextSize(Math.max(dp(7), Math.min(dp(11),
                        Math.abs(pointX[1] - pointX[0]) * 0.2f)));
                canvas.drawText(tier.getLabel(), centerX, centerY + dp(3), textPaint);
            }
            bottom = top;
        }
    }

    private void drawCuboid(Canvas canvas, float x, float y, float z,
                            float width, float length, float height, int color) {
        float right = x + width;
        float front = y + length;
        float top = z + height;
        project(x, y, top, 0);
        project(right, y, top, 1);
        project(right, front, top, 2);
        project(x, front, top, 3);
        project(right, y, z, 4);
        project(right, front, z, 5);
        project(x, front, z, 6);
        drawFace(canvas, adjustColor(color, 0.72f), 1, 2, 5, 4);
        drawFace(canvas, adjustColor(color, 0.86f), 3, 2, 5, 6);
        drawFace(canvas, adjustColor(color, 1.10f), 0, 1, 2, 3);
    }

    private void drawContainerShell(Canvas canvas) {
        project(0, 0, 0, 0);
        project(containerType.widthMm, 0, 0, 1);
        project(containerType.widthMm, containerType.lengthMm, 0, 2);
        project(0, containerType.lengthMm, 0, 3);
        project(0, 0, containerType.heightMm, 4);
        project(containerType.widthMm, 0, containerType.heightMm, 5);
        project(containerType.widthMm, containerType.lengthMm, containerType.heightMm, 6);
        project(0, containerType.lengthMm, containerType.heightMm, 7);

        drawLine(canvas, 0, 1); drawLine(canvas, 1, 2);
        drawLine(canvas, 2, 3); drawLine(canvas, 3, 0);
        drawLine(canvas, 4, 5); drawLine(canvas, 5, 6);
        drawLine(canvas, 6, 7); drawLine(canvas, 7, 4);
        drawLine(canvas, 0, 4); drawLine(canvas, 1, 5);
        drawLine(canvas, 2, 6); drawLine(canvas, 3, 7);
    }

    private void drawLine(Canvas canvas, int from, int to) {
        canvas.drawLine(pointX[from], pointY[from], pointX[to], pointY[to], shellPaint);
    }

    private void drawFace(Canvas canvas, int color, int... indices) {
        path.reset();
        path.moveTo(pointX[indices[0]], pointY[indices[0]]);
        for (int i = 1; i < indices.length; i++) {
            path.lineTo(pointX[indices[i]], pointY[indices[i]]);
        }
        path.close();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(color);
        canvas.drawPath(path, fillPaint);
        canvas.drawPath(path, strokePaint);
    }

    private void project(float x, float y, float z, int index) {
        pointX[index] = originX + (x - y) * ISO_X * rate;
        pointY[index] = originY + (x + y) * ISO_Y * rate - z * ISO_Z * rate;
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

    private static final Comparator<ContainerLoadPlanner.Placement> DRAW_ORDER =
            new Comparator<ContainerLoadPlanner.Placement>() {
                @Override
                public int compare(ContainerLoadPlanner.Placement left,
                                   ContainerLoadPlanner.Placement right) {
                    int depth = Integer.compare(left.getXMm() + left.getYMm(),
                            right.getXMm() + right.getYMm());
                    if (depth != 0) {
                        return depth;
                    }
                    return left.getTiers().get(0).getId().compareTo(
                            right.getTiers().get(0).getId());
                }
            };
}
