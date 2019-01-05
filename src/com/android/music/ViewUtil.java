package com.android.music;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.view.Window;
import android.view.animation.PathInterpolator;
import android.widget.TextView;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

public class ViewUtil {

    public final static int ANIM_TIME = 500;

    public static Animator createBackgroundColorTransition(final View v, @ColorInt final int startColor, @ColorInt final int endColor) {
        return createColorAnimator(v, "backgroundColor", startColor, endColor);
    }

    public static Animator createTextColorTransition(final TextView v, @ColorInt final int startColor, @ColorInt final int endColor) {
        return createColorAnimator(v, "textColor", startColor, endColor);
    }

    private static Animator createColorAnimator(Object target, String propertyName, @ColorInt int startColor, @ColorInt int endColor) {
        ObjectAnimator animator;
        animator = ObjectAnimator.ofArgb(target, propertyName, startColor, endColor);
        animator.setInterpolator(new PathInterpolator(0.4f, 0f, 1f, 1f));
        animator.setDuration(ANIM_TIME);
        return animator;
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static Drawable setTint(Drawable d, int color) {
        Drawable wrappedDrawable = DrawableCompat.wrap(d);
        DrawableCompat.setTint(wrappedDrawable, color);
        return wrappedDrawable;
    }

    public static void clearStatusBarMods(@NonNull Window window) {
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    public static void restoreStatusBarMods(@NonNull Window window) {
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }
}