package org.telesin.buttonlab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Original illustration of the five-button controller, not a product photograph. */
public final class ControllerGuide extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public ControllerGuide(Context context) {
        super(context);
        setContentDescription("Button guide: Capture at top left, M1 at top center, M2 at right, W and T below");
    }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(width, Math.round(width * 0.72f));
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(getWidth() / 320f, getHeight() / 230f);
        paint.setColor(0xffedf5fc);
        canvas.drawRoundRect(new RectF(0, 0, 320, 230), 16, 16, paint);
        paint.setColor(0xff142d48);
        canvas.drawRoundRect(new RectF(48, 38, 273, 199), 54, 54, paint);
        key(canvas, 98, 86, "●", 0xffff8a3d);
        key(canvas, 163, 86, "M1", 0xff0d9488);
        key(canvas, 223, 134, "M2", 0xff344c65);
        key(canvas, 98, 150, "W", 0xff344c65);
        key(canvas, 160, 150, "T", 0xff344c65);
        paint.setColor(0xff142d48);
        paint.setTextSize(13);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Capture", 98, 28, paint);
        canvas.drawText("Hold M1 · camera wake", 160, 221, paint);
        canvas.restore();
    }
    private void key(Canvas canvas, float x, float y, String label, int color) {
        paint.setColor(color);
        canvas.drawCircle(x, y, 25, paint);
        paint.setColor(0xffffffff);
        paint.setTextSize(18);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label, x, y - (paint.ascent() + paint.descent()) / 2, paint);
    }
}
