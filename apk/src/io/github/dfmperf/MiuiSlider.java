package io.github.dfmperf;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** MIUIX 风格滑条：粗圆角轨道 + 大号白色圆钮，主色填充已选段。 */
public class MiuiSlider extends View {
    public interface OnProgressChanged {
        void onProgressChanged(MiuiSlider view, int progress);
    }

    public interface OnStartTracking {
        void onStartTracking(MiuiSlider view);
    }

    public interface OnCommit {
        void onCommit(MiuiSlider view);
    }

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private int max = 75;
    private int progress = 75;
    private OnProgressChanged listener;
    private OnStartTracking startListener;
    private OnCommit commitListener;

    public MiuiSlider(Context c) {
        super(c);
        init();
    }

    public MiuiSlider(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    private void init() {
        knobStroke.setStyle(Paint.Style.STROKE);
        knobStroke.setStrokeWidth(Ui.dp(getContext(), 1));
        knobStroke.setColor(0x22000000);
    }

    public void setOnProgressChanged(OnProgressChanged l) {
        this.listener = l;
    }

    public void setOnStartTracking(OnStartTracking l) {
        this.startListener = l;
    }

    public void setOnCommit(OnCommit l) {
        this.commitListener = l;
    }

    public void setMax(int m) {
        this.max = Math.max(1, m);
        invalidate();
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int p) {
        int v = Math.max(0, Math.min(max, p));
        if (v == progress) {
            invalidate();
            return;
        }
        progress = v;
        invalidate();
        if (listener != null) listener.onProgressChanged(this, progress);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = Ui.dp(getContext(), 44);
        setMeasuredDimension(w, resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float h = getHeight();
        float w = getWidth();
        float trackH = Ui.dp(getContext(), 10);
        float knobR = Ui.dp(getContext(), 14);
        float pad = knobR;
        float cy = h / 2f;
        float left = 0f;
        float right = w;
        if (right <= left) return;

        // 轨道铺满整宽，与上方标题、下方预设档左右对齐
        rect.set(left, cy - trackH / 2f, right, cy + trackH / 2f);
        trackPaint.setColor(Ui.TRACK);
        canvas.drawRoundRect(rect, trackH / 2f, trackH / 2f, trackPaint);

        float t = max == 0 ? 0 : (float) progress / max;
        float knobX = pad + (w - 2 * pad) * t;
        if (knobX > left) {
            rect.set(left, cy - trackH / 2f, knobX, cy + trackH / 2f);
            activePaint.setColor(Ui.PRIMARY);
            canvas.drawRoundRect(rect, trackH / 2f, trackH / 2f, activePaint);
        }

        knobPaint.setColor(Ui.KNOB);
        canvas.drawCircle(knobX, cy, knobR, knobPaint);
        canvas.drawCircle(knobX, cy, knobR, knobStroke);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled()) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // 关键：别让外层 ScrollView 把纵向滑动抢走，否则手一歪就拖不动了
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                setPressed(true);
                if (startListener != null) startListener.onStartTracking(this);
                updateFromTouch(e.getX());
                return true;
            case MotionEvent.ACTION_MOVE:
                updateFromTouch(e.getX());
                return true;
            case MotionEvent.ACTION_UP:
                updateFromTouch(e.getX());
                setPressed(false);
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                if (commitListener != null) commitListener.onCommit(this);
                return true;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                if (commitListener != null) commitListener.onCommit(this);
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    /** 只在水平方向取值，纵向抖动不影响。 */
    private void updateFromTouch(float x) {
        float knobR = Ui.dp(getContext(), 14);
        float left = knobR;
        float right = getWidth() - knobR;
        if (right <= left) return;
        float clamped = Math.max(left, Math.min(right, x));
        int p = Math.round((clamped - left) / (right - left) * max);
        setProgress(p);
    }
}
