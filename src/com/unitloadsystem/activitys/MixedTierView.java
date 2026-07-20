package com.unitloadsystem.activitys;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MixedTierView extends View {
    public static final class BoxCell {
        public final String label;
        public final int typeIndex;
        public final int x;
        public final int y;
        public final int width;
        public final int length;

        public BoxCell(String label, int typeIndex, int x, int y, int width, int length) {
            this.label = label;
            this.typeIndex = typeIndex;
            this.x = x;
            this.y = y;
            this.width = width;
            this.length = length;
        }
    }

    private static final int[] BOX_COLORS = {
            Color.rgb(0, 114, 178), Color.rgb(213, 94, 0), Color.rgb(0, 121, 92),
            Color.rgb(167, 87, 145), Color.rgb(216, 143, 0), Color.rgb(74, 149, 181)
    };

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint patternPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF palletRect = new RectF();
    private final RectF cellRect = new RectF();
    private final ArrayList<BoxCell> cells = new ArrayList<BoxCell>();
    private int palletWidth = 1;
    private int palletLength = 1;

    public MixedTierView(Context context) {
        super(context);
        initialize();
    }

    public MixedTierView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    private void initialize() {
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(1.5f));
        borderPaint.setColor(Color.rgb(23, 36, 46));
        patternPaint.setStyle(Paint.Style.STROKE);
        patternPaint.setStrokeWidth(dp(1f));
        patternPaint.setColor(Color.argb(155, 255, 255, 255));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setTier(int palletWidth, int palletLength, List<BoxCell> cells,
                        String contentDescription) {
        this.palletWidth = Math.max(1, palletWidth);
        this.palletLength = Math.max(1, palletLength);
        this.cells.clear();
        this.cells.addAll(cells);
        setContentDescription(contentDescription);
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int availableWidth = Math.max(dp(180), MeasureSpec.getSize(widthMeasureSpec));
        int drawingWidth = Math.max(dp(120), availableWidth - getPaddingLeft() - getPaddingRight());
        int desired = Math.round(drawingWidth * (palletLength / (float) palletWidth));
        desired = Math.max(dp(150), Math.min(dp(220), desired));
        setMeasuredDimension(availableWidth, desired + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float inset = dp(5);
        palletRect.set(getPaddingLeft() + inset, getPaddingTop() + inset,
                getWidth() - getPaddingRight() - inset,
                getHeight() - getPaddingBottom() - inset);
        fillPaint.setColor(Color.rgb(225, 233, 236));
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(palletRect, fillPaint);
        borderPaint.setStrokeWidth(dp(2));
        canvas.drawRect(palletRect, borderPaint);

        float scaleX = palletRect.width() / palletWidth;
        float scaleY = palletRect.height() / palletLength;
        for (BoxCell cell : cells) {
            cellRect.set(palletRect.left + cell.x * scaleX,
                    palletRect.top + cell.y * scaleY,
                    palletRect.left + (cell.x + cell.width) * scaleX,
                    palletRect.top + (cell.y + cell.length) * scaleY);
            cellRect.inset(dp(1), dp(1));
            drawCell(canvas, cellRect, cell);
        }
    }

    private void drawCell(Canvas canvas, RectF rect, BoxCell cell) {
        int color = BOX_COLORS[Math.floorMod(cell.typeIndex, BOX_COLORS.length)];
        fillPaint.setColor(color);
        canvas.drawRect(rect, fillPaint);
        borderPaint.setStrokeWidth(dp(1.2f));
        canvas.drawRect(rect, borderPaint);

        canvas.save();
        canvas.clipRect(rect);
        int pattern = Math.floorMod(cell.typeIndex, 4);
        float gap = dp(9);
        if (pattern == 0) {
            for (float x = rect.left - rect.height(); x < rect.right; x += gap) {
                canvas.drawLine(x, rect.bottom, x + rect.height(), rect.top, patternPaint);
            }
        } else if (pattern == 1) {
            for (float y = rect.top + gap; y < rect.bottom; y += gap) {
                canvas.drawLine(rect.left, y, rect.right, y, patternPaint);
            }
        } else if (pattern == 2) {
            for (float x = rect.left + gap / 2; x < rect.right; x += gap) {
                for (float y = rect.top + gap / 2; y < rect.bottom; y += gap) {
                    canvas.drawCircle(x, y, dp(1.2f), patternPaint);
                }
            }
        } else {
            for (float x = rect.left - rect.height(); x < rect.right; x += gap) {
                canvas.drawLine(x, rect.top, x + rect.height(), rect.bottom, patternPaint);
                canvas.drawLine(x, rect.bottom, x + rect.height(), rect.top, patternPaint);
            }
        }
        canvas.restore();

        float textSize = Math.max(dp(7), Math.min(dp(16),
                Math.min(rect.width(), rect.height()) * 0.34f));
        textPaint.setTextSize(textSize);
        float availableTextWidth = Math.max(dp(4), rect.width() - dp(5));
        float measuredTextWidth = textPaint.measureText(cell.label);
        if (measuredTextWidth > availableTextWidth) {
            textSize = Math.max(dp(6), textSize * availableTextWidth / measuredTextWidth);
            textPaint.setTextSize(textSize);
        }
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = rect.centerY() - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(cell.label, rect.centerX(), baseline, textPaint);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
