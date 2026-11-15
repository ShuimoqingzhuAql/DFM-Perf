package io.github.deltaconfig;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/* loaded from: classes.dex */
final class AppBackgroundDrawable extends Drawable {
    private static final int IMAGE_ALPHA = 102;
    private final Bitmap image;
    private final Paint imagePaint = new Paint(3);
    private final Paint basePaint = new Paint();
    private final RectF destination = new RectF();
    private int drawableAlpha = 255;

    AppBackgroundDrawable(Resources resources) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        this.image = BitmapFactory.decodeResource(resources, R.drawable.app_background, options);
        this.basePaint.setColor(resources.getColor(R.color.background, null));
        this.imagePaint.setAlpha(IMAGE_ALPHA);
    }

    @Override // android.graphics.drawable.Drawable
    protected void onBoundsChange(Rect rect) {
        if (this.image == null) {
            return;
        }
        float max = Math.max(rect.width() / this.image.getWidth(), rect.height() / this.image.getHeight());
        float width = this.image.getWidth() * max;
        float height = this.image.getHeight() * max;
        float width2 = rect.left + ((rect.width() - width) * 0.5f);
        float height2 = rect.top + ((rect.height() - height) * 0.35f);
        this.destination.set(width2, height2, width + width2, height + height2);
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        int save = canvas.save();
        canvas.clipRect(getBounds());
        canvas.drawRect(getBounds(), this.basePaint);
        if (this.image != null) {
            canvas.drawBitmap(this.image, (Rect) null, this.destination, this.imagePaint);
        }
        canvas.restoreToCount(save);
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int i) {
        this.drawableAlpha = Math.max(0, Math.min(255, i));
        this.basePaint.setAlpha(this.drawableAlpha);
        this.imagePaint.setAlpha((this.drawableAlpha * IMAGE_ALPHA) / 255);
        invalidateSelf();
    }

    @Override // android.graphics.drawable.Drawable
    public int getAlpha() {
        return this.drawableAlpha;
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
        this.imagePaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return this.drawableAlpha == 255 ? -1 : -3;
    }
}
