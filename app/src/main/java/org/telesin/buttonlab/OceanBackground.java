package org.telesin.buttonlab;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Original, static water-light artwork. Blurred separately behind the crisp interface. */
final class OceanBackground extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @Override public void draw(Canvas canvas) {
        Rect b = getBounds(); float w=b.width(), h=b.height();
        paint.setColor(Color.WHITE); // Reset alpha before every draw, including redraws.
        paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xff05263d,0xff075d76,0xff071e36},new float[]{0,.48f,1},Shader.TileMode.CLAMP));
        canvas.drawRect(b,paint);
        paint.setShader(new RadialGradient(w*.05f,h*.1f,w*.9f,new int[]{0x992ae0da,0x001478a3},null,Shader.TileMode.CLAMP)); canvas.drawRect(b,paint);
        paint.setShader(new RadialGradient(w*.95f,h*.55f,w*.8f,new int[]{0x9922a4d9,0x001478a3},null,Shader.TileMode.CLAMP)); canvas.drawRect(b,paint);
        paint.setShader(new RadialGradient(w*.3f,h*.95f,w*.8f,new int[]{0x6633a7a8,0x001478a3},null,Shader.TileMode.CLAMP)); canvas.drawRect(b,paint);
        // Broad diffuse light rays; no ongoing animation or background timers.
        paint.setShader(null); paint.setColor(0x163ee4ed);
        Path ray = new Path(); ray.moveTo(-w*.1f,0); ray.lineTo(w*.12f,0); ray.lineTo(w*.8f,h); ray.lineTo(w*.3f,h); ray.close(); canvas.drawPath(ray,paint);
        ray.reset(); ray.moveTo(w*.5f,0); ray.lineTo(w*.6f,0); ray.lineTo(w*1.4f,h); ray.lineTo(w,h); ray.close(); canvas.drawPath(ray,paint);
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
