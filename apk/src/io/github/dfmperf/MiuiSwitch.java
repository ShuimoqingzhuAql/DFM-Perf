package io.github.dfmperf;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** MIUIX 风格的开关：胶囊轨道 + 白色圆钮，颜色跟随主题主色。 */
public class MiuiSwitch extends View {
    public interface OnCheckedChangeListener {
        void onCheckedChanged(MiuiSwitch view, boolean checked);
    }

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();
    private boolean checked;
    private float anim;
    private ValueAnimator animator;
    private OnCheckedChangeListener listener;

    public MiuiSwitch(Context c) {
        super(c);
        init();
    }

    public MiuiSwitch(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    private void init() {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(Ui.dp(getContext(), 1));
        strokePaint.setColor(0x22000000);
        setClickable(true);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener l) {
        this.listener = l;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean value) {
        setChecked(value, true);
    }

    public void setChecked(boolean value, boolean animate) {
        if (checked == value) {
            anim = value ? 1f : 0f;
            invalidate();
            return;
        }
        checked = value;
        if (!animate) {
            anim = value ? 1f : 0f;
            invalidate();
            return;
        }
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(anim, value ? 1f : 0f);
        animator.setDuration(160);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                anim = (Float) a.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = Ui.dp(getContext(), 56);
        int h = Ui.dp(getContext(), 34);
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float trackH = Ui.dp(getContext(), 30);
        float top = (h - trackH) / 2f;
        trackRect.set(0, top, w, top + trackH);
        float radius = trackH / 2f;

        int offColor = Ui.TRACK;
        int onColor = Ui.PRIMARY;
        trackPaint.setColor(blend(offColor, onColor, anim));
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);

        float knobR = trackH / 2f - Ui.dp(getContext(), 2);
        float left = knobR + Ui.dp(getContext(), 2) + Ui.dp(getContext(), 1);
        float right = w - knobR - Ui.dp(getContext(), 3);
        float cx = left + (right - left) * anim;
        float cy = h / 2f;
        knobPaint.setColor(Ui.KNOB);
        canvas.drawCircle(cx, cy, knobR, knobPaint);
        canvas.drawCircle(cx, cy, knobR, strokePaint);
    }

    private static int blend(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                if (isEnabled()) {
                    setChecked(!checked, true);
                    if (listener != null) listener.onCheckedChanged(this, checked);
                }
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }
}
