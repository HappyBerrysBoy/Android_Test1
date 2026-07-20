package com.unitloadsystem.activitys;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.unitloadsystem.beans.PalletViewBean;
import com.unitloadsystem.common.CommonFunction;
import com.unitloadsystem.container.ContainerLoadCalculator;
import com.unitloadsystem.container.ContainerLoadPlanner;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CalculationResultActivity extends LocalizedActivity {
    private static final String STATE_RESULT_MODE = "state_result_mode";
    private static final String STATE_CONTAINER_TYPE = "state_container_type";
    private static final int MODE_2D = 0;
    private static final int MODE_3D = 1;
    private static final int MODE_CONTAINER = 2;

    private float boxLength;
    private float boxWidth;
    private float boxHeight;
    private float boxWeight;
    private int boxQuantity;
    private int boxLayers;
    private String boxDimension;
    private String boxWeightUnit;
    private String boxName;
    private Bundle layouts;
    private int resultMode;
    private String selectedContainerTypeName;
    private final ArrayList<DiagramBinding> diagramBindings = new ArrayList<DiagramBinding>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calculationresult_layout);
        resultMode = savedInstanceState == null
                ? MODE_2D : savedInstanceState.getInt(STATE_RESULT_MODE, MODE_2D);
        selectedContainerTypeName = savedInstanceState == null
                ? null : savedInstanceState.getString(STATE_CONTAINER_TYPE);
        if (resultMode == MODE_CONTAINER && selectedContainerTypeName == null) {
            resultMode = MODE_2D;
        }
        readCalculationInput(getIntent());
        showSummary();
        renderPalletResults();
        updateModeButtons();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_RESULT_MODE, resultMode);
        outState.putString(STATE_CONTAINER_TYPE, selectedContainerTypeName);
        super.onSaveInstanceState(outState);
    }

    private void readCalculationInput(Intent intent) {
        boxLength = intent.getFloatExtra("Length", 0f);
        boxWidth = intent.getFloatExtra("Width", 0f);
        boxHeight = intent.getFloatExtra("Height", 0f);
        boxWeight = intent.getFloatExtra("BoxWeight", 0f);
        boxQuantity = intent.getIntExtra("BoxQuantity", 0);
        boxLayers = intent.getIntExtra("BoxLayer", 0);
        boxDimension = intent.getStringExtra("BoxDimension");
        boxWeightUnit = intent.getStringExtra("BoxWeightUnit");
        boxName = intent.getStringExtra("BoxName");
        layouts = intent.getBundleExtra("Layouts");

        if (boxDimension == null) {
            boxDimension = "mm";
        }
        if (boxWeightUnit == null) {
            boxWeightUnit = "kg";
        }
        if (boxName == null) {
            boxName = getString(R.string.directInput);
        }
    }

    private void showSummary() {
        TextView palletHeader = (TextView) findViewById(R.id.resultPalletHeader);
        TextView boxHeader = (TextView) findViewById(R.id.resultBoxHeader);
        String boxSummary = getString(R.string.resultBoxSummary,
                boxName,
                formatNumber(toDisplayUnit(boxLength)),
                formatNumber(toDisplayUnit(boxWidth)),
                formatNumber(toDisplayUnit(boxHeight)),
                boxDimension,
                formatNumber(boxWeight),
                boxWeightUnit,
                boxQuantity,
                boxLayers);

        Bundle pallet = layouts == null ? null : layouts.getBundle("Layout0");
        if (pallet == null) {
            palletHeader.setText(R.string.noCalculationResult);
            boxHeader.setText(boxSummary);
            return;
        }
        String palletDimension = pallet.getString("PalletDimen", "mm");
        String palletSummary = getString(R.string.resultPalletSummary,
                pallet.getString("PalletName", ""),
                formatNumber(toPalletDisplayUnit(pallet.getInt("ContainerWidth") - 1,
                        palletDimension)),
                formatNumber(toPalletDisplayUnit(pallet.getInt("ContainerLength") - 1,
                        palletDimension)),
                palletDimension,
                formatNumber(toPalletDisplayUnit(pallet.getInt("PalletHeight"),
                        palletDimension)));
        palletHeader.setText(palletSummary);
        boxHeader.setText(boxSummary);
    }

    private void renderPalletResults() {
        LinearLayout container = (LinearLayout) findViewById(R.id.layoutResult);
        container.removeAllViews();
        diagramBindings.clear();

        if (resultMode == MODE_CONTAINER) {
            renderContainerResults(container);
            return;
        }

        if (layouts == null || layouts.size() == 0) {
            TextView empty = new TextView(this);
            empty.setText(R.string.noCalculationResult);
            empty.setTextColor(getResources().getColor(R.color.textSecondary));
            empty.setTextSize(15);
            empty.setPadding(dp(8), dp(28), dp(8), dp(28));
            container.addView(empty);
            return;
        }

        for (int i = 0; i < layouts.size(); i++) {
            Bundle layout = layouts.getBundle("Layout" + i);
            if (layout != null) {
                container.addView(createPalletResultView(container, layout));
            }
        }
    }

    private View createPalletResultView(LinearLayout parent, Bundle layout) {
        View item = getLayoutInflater().inflate(R.layout.result_pallet_item, parent, false);

        int containerWidth = layout.getInt("ContainerWidth");
        int containerLength = layout.getInt("ContainerLength");
        int palletHeight = layout.getInt("PalletHeight");
        String palletDimension = layout.getString("PalletDimen");
        if (palletDimension == null) {
            palletDimension = "mm";
        }

        float displayWidth = toPalletDisplayUnit(Math.max(0, containerWidth - 1), palletDimension);
        float displayLength = toPalletDisplayUnit(Math.max(0, containerLength - 1), palletDimension);
        float displayPalletHeight = toPalletDisplayUnit(palletHeight, palletDimension);
        ((TextView) item.findViewById(R.id.resultPalletName)).setText(layout.getString("PalletName"));
        ((TextView) item.findViewById(R.id.resultPalletSize)).setText(
                getString(R.string.resultPalletSize, formatNumber(displayWidth),
                        formatNumber(displayLength), palletDimension,
                        formatNumber(displayPalletHeight)));
        ((TextView) item.findViewById(R.id.resultStackHeightValue)).setText(
                formatNumber(toDisplayUnit(boxHeight * boxLayers + palletHeight))
                        + " " + boxDimension);
        ((TextView) item.findViewById(R.id.resultTotalWeightValue)).setText(
                formatNumber(boxWeight * boxQuantity) + " " + boxWeightUnit);

        ArrayList<PalletViewBean> split = getStack(layout, "SplitStack");
        ArrayList<PalletViewBean> pinWheel = getStack(layout, "PinWheelStack");

        bindStrategy(item, true, split, layout.getDouble("SplitStackShare"),
                containerWidth, containerLength, palletHeight, 0, 0);
        bindStrategy(item, false, pinWheel, layout.getDouble("PinWheelStackShare"),
                containerWidth, containerLength, palletHeight,
                layout.getInt("PinWheelStackRowCount"),
                layout.getInt("PinWheelStackColCount"));

        return item;
    }

    private void bindStrategy(View item, boolean first, ArrayList<PalletViewBean> stack, double share,
                              int containerWidth, int containerLength, int palletHeight,
                              int rowCount, int colCount) {
        int titleId = first ? R.id.strategyOneTitle : R.id.strategyTwoTitle;
        int metaId = first ? R.id.strategyOneMeta : R.id.strategyTwoMeta;
        int needId = first ? R.id.strategyOneNeed : R.id.strategyTwoNeed;
        int diagramId = first ? R.id.strategyOneDiagram : R.id.strategyTwoDiagram;
        int requiredPallets = getRequiredPalletCount(stack.size());

        ((TextView) item.findViewById(titleId)).setText(
                first ? R.string.resultStrategyOne : R.string.resultStrategyTwo);
        ((TextView) item.findViewById(metaId)).setText(
                getString(R.string.resultStrategyMeta, stack.size(), Math.round(share * 100)));
        ((TextView) item.findViewById(needId)).setText(
                getString(R.string.resultNeedCount, requiredPallets));
        FrameLayout diagram = (FrameLayout) item.findViewById(diagramId);
        DiagramBinding binding = new DiagramBinding(diagram, stack,
                containerWidth, containerLength, palletHeight,
                boxWidth, boxLength, boxHeight, boxLayers,
                !first, rowCount, colCount,
                first ? R.color.resultBoxPrimary : R.color.resultBoxSecondary);
        diagramBindings.add(binding);
        binding.show(this, resultMode == MODE_3D);
    }

    private void renderContainerResults(LinearLayout parent) {
        List<ContainerResultRenderer.Section> sections =
                new ArrayList<ContainerResultRenderer.Section>();
        if (layouts != null) {
            for (int i = 0; i < layouts.size(); i++) {
                Bundle layout = layouts.getBundle("Layout" + i);
                if (layout == null) {
                    continue;
                }
                int palletWidth = Math.max(0, layout.getInt("ContainerWidth") - 1);
                int palletLength = Math.max(0, layout.getInt("ContainerLength") - 1);
                int loadedHeight = Math.round(boxHeight * boxLayers)
                        + layout.getInt("PalletHeight");
                String palletName = layout.getString("PalletName", "");

                ArrayList<PalletViewBean> split = getStack(layout, "SplitStack");
                ArrayList<PalletViewBean> pinWheel = getStack(layout, "PinWheelStack");
                int splitPallets = getRequiredPalletCount(split.size());
                int pinWheelPallets = getRequiredPalletCount(pinWheel.size());
                boolean usePinWheel = pinWheelPallets > 0
                        && (splitPallets <= 0 || pinWheelPallets < splitPallets);
                int recommendedPallets = usePinWheel ? pinWheelPallets : splitPallets;
                sections.add(new ContainerResultRenderer.Section(
                        palletName,
                        createUnitLoads(usePinWheel ? "S2" : "S1", recommendedPallets,
                                palletWidth, palletLength, loadedHeight)));
            }
        }
        ContainerLoadCalculator.ContainerType type =
                ContainerResultRenderer.findContainerType(selectedContainerTypeName);
        if (type != null) {
            ContainerResultRenderer.render(this, parent, sections, type);
        }
    }

    private List<ContainerLoadPlanner.UnitLoad> createUnitLoads(
            String prefix, int count, int widthMm, int lengthMm, int heightMm) {
        List<ContainerLoadPlanner.UnitLoad> loads =
                new ArrayList<ContainerLoadPlanner.UnitLoad>();
        for (int i = 0; i < count; i++) {
            String label = "P" + (i + 1);
            loads.add(new ContainerLoadPlanner.UnitLoad(
                    prefix + "-" + (i + 1), label,
                    widthMm, lengthMm, heightMm));
        }
        return loads;
    }

    public void show2d(View view) {
        setResultMode(MODE_2D);
    }

    public void show3d(View view) {
        setResultMode(MODE_3D);
    }

    public void showContainer(View view) {
        ContainerResultRenderer.chooseContainerType(this, selectedContainerTypeName,
                new ContainerResultRenderer.OnContainerTypeSelected() {
                    @Override
                    public void onContainerTypeSelected(ContainerLoadCalculator.ContainerType type) {
                        selectedContainerTypeName = type.name;
                        resultMode = MODE_CONTAINER;
                        renderPalletResults();
                        updateModeButtons();
                    }
                });
    }

    private void setResultMode(int mode) {
        if (resultMode == mode) {
            return;
        }
        resultMode = mode;
        renderPalletResults();
        updateModeButtons();
    }

    private void updateModeButtons() {
        Button mode2d = (Button) findViewById(R.id.mode2d);
        Button mode3d = (Button) findViewById(R.id.mode3d);
        Button modeContainer = (Button) findViewById(R.id.modeContainer);
        updateModeButton(mode2d, resultMode == MODE_2D);
        updateModeButton(mode3d, resultMode == MODE_3D);
        updateModeButton(modeContainer, resultMode == MODE_CONTAINER);
    }

    private void updateModeButton(Button button, boolean selected) {
        button.setBackgroundResource(selected
                ? R.drawable.mode_segment_selected : R.drawable.mode_segment_unselected);
        button.setTextColor(getResources().getColor(
                selected ? R.color.headerText : R.color.textSecondary));
        button.setSelected(selected);
    }

    private ArrayList<PalletViewBean> getStack(Bundle layout, String key) {
        Bundle stackBundle = layout.getBundle(key);
        if (stackBundle == null) {
            return new ArrayList<PalletViewBean>();
        }
        ArrayList<PalletViewBean> stack = stackBundle.getParcelableArrayList("Layout");
        return stack == null ? new ArrayList<PalletViewBean>() : stack;
    }

    private int getRequiredPalletCount(int boxesPerLayer) {
        if (boxesPerLayer <= 0 || boxLayers <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) boxQuantity / (boxesPerLayer * boxLayers));
    }

    private float toDisplayUnit(float value) {
        return CommonFunction.changeToInch(value, boxDimension);
    }

    private float toPalletDisplayUnit(float value, String palletDimension) {
        return CommonFunction.changeToInch(value, palletDimension);
    }

    private String formatNumber(float value) {
        return new DecimalFormat("#,##0.##").format(value);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public void btnBack(View view) {
        finish();
    }

    private static class DiagramBinding {
        private final FrameLayout container;
        private final ArrayList<PalletViewBean> stack;
        private final float palletWidth;
        private final float palletLength;
        private final float palletHeight;
        private final float boxWidth;
        private final float boxLength;
        private final float boxHeight;
        private final int layers;
        private final boolean pinWheel;
        private final int rowCount;
        private final int colCount;
        private final int boxColor;

        DiagramBinding(FrameLayout container, ArrayList<PalletViewBean> stack,
                       float palletWidth, float palletLength,
                       float palletHeight,
                       float boxWidth, float boxLength, float boxHeight, int layers,
                       boolean pinWheel, int rowCount, int colCount, int boxColor) {
            this.container = container;
            this.stack = stack;
            this.palletWidth = palletWidth;
            this.palletLength = palletLength;
            this.palletHeight = palletHeight;
            this.boxWidth = boxWidth;
            this.boxLength = boxLength;
            this.boxHeight = boxHeight;
            this.layers = layers;
            this.pinWheel = pinWheel;
            this.rowCount = rowCount;
            this.colCount = colCount;
            this.boxColor = boxColor;
        }

        void show(Context context, boolean show3d) {
            container.removeAllViews();
            View diagram = show3d
                    ? new IsometricStackDiagramView(context, stack, palletWidth, palletLength,
                            palletHeight, boxWidth, boxLength, boxHeight, layers,
                            pinWheel, rowCount, colCount, boxColor)
                    : new StackDiagramView(context, stack, palletWidth, palletLength,
                            boxWidth, boxLength, pinWheel, rowCount, colCount, boxColor);
            container.addView(diagram,
                    new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private static class StackDiagramView extends View {
        private final ArrayList<PalletViewBean> stack;
        private final float containerWidth;
        private final float containerLength;
        private final float boxWidth;
        private final float boxLength;
        private final boolean pinWheel;
        private final int rowCount;
        private final int colCount;
        private final Paint palletPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float rate;
        private float originX;
        private float originY;

        StackDiagramView(Context context, ArrayList<PalletViewBean> stack,
                         float containerWidth, float containerLength,
                         float boxWidth, float boxLength,
                         boolean pinWheel, int rowCount, int colCount, int boxColor) {
            super(context);
            this.stack = stack;
            this.containerWidth = Math.max(1f, containerWidth);
            this.containerLength = Math.max(1f, containerLength);
            this.boxWidth = boxWidth;
            this.boxLength = boxLength;
            this.pinWheel = pinWheel;
            this.rowCount = rowCount;
            this.colCount = colCount;

            palletPaint.setColor(getResources().getColor(R.color.resultPalletSurface));
            palletPaint.setStyle(Paint.Style.FILL);
            borderPaint.setColor(getResources().getColor(R.color.resultPalletBorder));
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(dp(2));
            boxPaint.setColor(getResources().getColor(boxColor));
            boxPaint.setStyle(Paint.Style.FILL);
            setPadding(dp(8), dp(8), dp(8), dp(8));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            if (width <= 0) {
                width = getResources().getDisplayMetrics().widthPixels;
            }
            float availableWidth = Math.max(1, width - getPaddingLeft() - getPaddingRight());
            float drawingWidth = Math.min(availableWidth, dp(420));
            rate = drawingWidth / containerWidth;
            int desiredHeight = Math.round(containerLength * rate) + getPaddingTop() + getPaddingBottom();
            setMeasuredDimension(width, resolveSize(desiredHeight, heightMeasureSpec));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float drawingWidth = containerWidth * rate;
            float drawingHeight = containerLength * rate;
            originX = (getWidth() - drawingWidth) / 2f;
            originY = getPaddingTop();

            RectF pallet = new RectF(originX, originY, originX + drawingWidth, originY + drawingHeight);
            canvas.drawRect(pallet, palletPaint);

            if (pinWheel) {
                drawPinWheel(canvas);
            } else {
                drawStack(canvas, 0);
            }
            canvas.drawRect(pallet, borderPaint);
        }

        private void drawStack(Canvas canvas, int startIndex) {
            for (int i = startIndex; i < stack.size(); i++) {
                PalletViewBean box = stack.get(i);
                boolean horizontal = "H".equals(box.getdirection());
                float width = (horizontal ? boxWidth : boxLength) * rate;
                float length = (horizontal ? boxLength : boxWidth) * rate;
                drawBox(canvas,
                        originX + box.getx() * rate,
                        originY + box.gety() * rate,
                        width,
                        length);
            }
        }

        private void drawPinWheel(Canvas canvas) {
            float scaledWidth = boxWidth * rate;
            float scaledLength = boxLength * rate;

            for (int i = 0; i < rowCount; i++) {
                for (int j = 0; j < colCount; j++) {
                    drawBox(canvas, originX + scaledWidth * i, originY + scaledLength * j,
                            scaledWidth, scaledLength);
                    drawBox(canvas, originX + scaledWidth * rowCount + scaledLength * j,
                            originY + scaledWidth * i, scaledLength, scaledWidth);
                    drawBox(canvas, originX + scaledLength * j,
                            originY + colCount * scaledLength + scaledWidth * i,
                            scaledLength, scaledWidth);
                    drawBox(canvas, originX + scaledLength * colCount + scaledWidth * i,
                            originY + scaledWidth * rowCount + scaledLength * j,
                            scaledWidth, scaledLength);
                }
            }
            drawStack(canvas, rowCount * colCount * 4);
        }

        private void drawBox(Canvas canvas, float left, float top, float width, float height) {
            float gap = dp(1);
            canvas.drawRect(new RectF(left, top,
                    Math.max(left, left + width - gap),
                    Math.max(top, top + height - gap)), boxPaint);
        }

        private int dp(int value) {
            return Math.round(value * getResources().getDisplayMetrics().density);
        }
    }

    private static class IsometricStackDiagramView extends View {
        private static final float ISO_X = 0.866f;
        private static final float ISO_Y = 0.42f;
        private static final float ISO_Z = 0.58f;
        private final List<BoxFootprint> boxes;
        private final float palletWidth;
        private final float palletLength;
        private final float palletHeight;
        private final float boxHeight;
        private final int layers;
        private final int baseColor;
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float rate;
        private float originX;
        private float originY;

        IsometricStackDiagramView(Context context, ArrayList<PalletViewBean> stack,
                                  float palletWidth, float palletLength,
                                  float palletHeight,
                                  float boxWidth, float boxLength, float boxHeight, int layers,
                                  boolean pinWheel, int rowCount, int colCount, int boxColor) {
            super(context);
            this.palletWidth = Math.max(1f, palletWidth);
            this.palletLength = Math.max(1f, palletLength);
            this.palletHeight = Math.max(1f, palletHeight);
            this.boxHeight = Math.max(1f, boxHeight);
            this.layers = Math.max(1, layers);
            this.baseColor = getResources().getColor(boxColor);
            this.boxes = buildFootprints(stack, boxWidth, boxLength,
                    pinWheel, rowCount, colCount);

            strokePaint.setColor(adjustColor(baseColor, 0.62f));
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(dp(0.7f));
            setPadding(dp(5), dp(5), dp(5), dp(7));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            if (width <= 0) {
                width = getResources().getDisplayMetrics().widthPixels;
            }
            float availableWidth = Math.max(1,
                    width - getPaddingLeft() - getPaddingRight());
            float drawingWidth = Math.min(availableWidth, dp(420));
            float projectedWidth = (palletWidth + palletLength) * ISO_X;
            rate = drawingWidth / Math.max(1f, projectedWidth);

            float loadedHeight = boxHeight * layers;
            float projectedHeight = (palletWidth + palletLength) * ISO_Y
                    + loadedHeight * ISO_Z + palletHeight * ISO_Z;
            int desiredHeight = Math.round(projectedHeight * rate)
                    + getPaddingTop() + getPaddingBottom();
            setMeasuredDimension(width, resolveSize(desiredHeight, heightMeasureSpec));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float projectedWidth = (palletWidth + palletLength) * ISO_X * rate;
            originX = (getWidth() - projectedWidth) / 2f
                    + palletLength * ISO_X * rate;
            originY = getPaddingTop() + boxHeight * layers * ISO_Z * rate;

            drawPallet(canvas);
            for (int layer = 0; layer < layers; layer++) {
                float bottom = layer * boxHeight;
                float top = bottom + boxHeight;
                for (BoxFootprint box : boxes) {
                    drawCuboid(canvas, box, bottom, top, layer);
                }
            }
        }

        private void drawPallet(Canvas canvas) {
            PointF topBack = project(0, 0, 0);
            PointF topRight = project(palletWidth, 0, 0);
            PointF topFront = project(palletWidth, palletLength, 0);
            PointF topLeft = project(0, palletLength, 0);
            PointF bottomRight = project(palletWidth, 0, -palletHeight);
            PointF bottomFront = project(palletWidth, palletLength, -palletHeight);
            PointF bottomLeft = project(0, palletLength, -palletHeight);

            drawPolygon(canvas, adjustColor(
                    getResources().getColor(R.color.resultPalletSurface), 0.78f),
                    topLeft, topFront, bottomFront, bottomLeft);
            drawPolygon(canvas, getResources().getColor(R.color.resultPalletBorder),
                    topRight, topFront, bottomFront, bottomRight);
            drawPolygon(canvas, getResources().getColor(R.color.resultPalletSurface),
                    topBack, topRight, topFront, topLeft);
        }

        private void drawCuboid(Canvas canvas, BoxFootprint box,
                                float bottom, float top, int layer) {
            int layerColor = layer % 2 == 0 ? baseColor : adjustColor(baseColor, 0.94f);
            PointF topBack = project(box.x, box.y, top);
            PointF topRight = project(box.x + box.width, box.y, top);
            PointF topFront = project(box.x + box.width, box.y + box.length, top);
            PointF topLeft = project(box.x, box.y + box.length, top);
            PointF bottomRight = project(box.x + box.width, box.y, bottom);
            PointF bottomFront = project(box.x + box.width, box.y + box.length, bottom);
            PointF bottomLeft = project(box.x, box.y + box.length, bottom);

            drawPolygon(canvas, adjustColor(layerColor, 0.72f),
                    topRight, topFront, bottomFront, bottomRight);
            drawPolygon(canvas, adjustColor(layerColor, 0.86f),
                    topLeft, topFront, bottomFront, bottomLeft);
            drawPolygon(canvas, adjustColor(layerColor, 1.14f),
                    topBack, topRight, topFront, topLeft);
        }

        private void drawPolygon(Canvas canvas, int color, PointF... points) {
            Path path = new Path();
            path.moveTo(points[0].x, points[0].y);
            for (int i = 1; i < points.length; i++) {
                path.lineTo(points[i].x, points[i].y);
            }
            path.close();
            fillPaint.setColor(color);
            fillPaint.setStyle(Paint.Style.FILL);
            canvas.drawPath(path, fillPaint);
            canvas.drawPath(path, strokePaint);
        }

        private PointF project(float x, float y, float z) {
            return new PointF(
                    originX + (x - y) * ISO_X * rate,
                    originY + (x + y) * ISO_Y * rate - z * ISO_Z * rate);
        }

        private static List<BoxFootprint> buildFootprints(
                ArrayList<PalletViewBean> stack, float boxWidth, float boxLength,
                boolean pinWheel, int rowCount, int colCount) {
            ArrayList<BoxFootprint> result = new ArrayList<BoxFootprint>();
            int startIndex = 0;

            if (pinWheel) {
                for (int i = 0; i < rowCount; i++) {
                    for (int j = 0; j < colCount; j++) {
                        result.add(new BoxFootprint(boxWidth * i, boxLength * j,
                                boxWidth, boxLength));
                        result.add(new BoxFootprint(boxWidth * rowCount + boxLength * j,
                                boxWidth * i, boxLength, boxWidth));
                        result.add(new BoxFootprint(boxLength * j,
                                colCount * boxLength + boxWidth * i,
                                boxLength, boxWidth));
                        result.add(new BoxFootprint(boxLength * colCount + boxWidth * i,
                                boxWidth * rowCount + boxLength * j,
                                boxWidth, boxLength));
                    }
                }
                startIndex = rowCount * colCount * 4;
            }

            for (int i = startIndex; i < stack.size(); i++) {
                PalletViewBean box = stack.get(i);
                boolean horizontal = "H".equals(box.getdirection());
                result.add(new BoxFootprint(box.getx(), box.gety(),
                        horizontal ? boxWidth : boxLength,
                        horizontal ? boxLength : boxWidth));
            }

            Collections.sort(result, new Comparator<BoxFootprint>() {
                @Override
                public int compare(BoxFootprint first, BoxFootprint second) {
                    return Float.compare(first.x + first.y, second.x + second.y);
                }
            });
            return result;
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

        private static class BoxFootprint {
            final float x;
            final float y;
            final float width;
            final float length;

            BoxFootprint(float x, float y, float width, float length) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.length = length;
            }
        }
    }
}
