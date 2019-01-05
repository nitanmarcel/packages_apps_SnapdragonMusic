package com.android.music;

import android.annotation.ColorInt;
import android.graphics.Color;
import android.support.v7.graphics.Palette;

public class PaletteParser {

    private @ColorInt
    int colorPrimary;
    private @ColorInt
    int colorPrimaryDark;
    private @ColorInt
    int colorBackground;
    private @ColorInt
    int colorBackgroundDark;
    private @ColorInt
    int colorAccent;
    private @ColorInt
    int colorText;
    private @ColorInt
    int colorTextPrimary;
    private @ColorInt
    int colorTextSecondary;
    private @ColorInt
    int colorControlActivated;

    private Palette palette;

    public PaletteParser(Palette palette) {
        this.palette = palette;
        extractColors();
    }

    public int getColorControlActivated() {
        return colorControlActivated;
    }

    private void extractColors() {
        Palette.Swatch swatch = palette.getVibrantSwatch();

        //Make sure we get a fallback swatch if LightVibrantSwatch is not available
        if (swatch == null)
            swatch = palette.getDarkVibrantSwatch();

        //Make sure we get another fallback swatch if DarkVibrantSwatch is not available
        if (swatch == null)
            swatch = palette.getDominantSwatch();

        colorTextPrimary = swatch.getBodyTextColor();
        colorTextSecondary = swatch.getTitleTextColor();

        colorPrimary = swatch.getRgb();
        colorPrimaryDark = ColorUtil.manipulateColor(colorPrimary, 0.70f);

        if (ColorUtil.isColorLight(getColorPrimary())) {
            colorControlActivated = Color.BLACK;
            colorText = Color.DKGRAY;
        } else {
            colorControlActivated = Color.WHITE;
            colorText = Color.LTGRAY;
        }

        colorBackground = ColorUtil.manipulateColor(colorPrimary, 0.35f);
        colorBackgroundDark = ColorUtil.manipulateColor(colorBackground, 0.70f);

        colorAccent = palette.getVibrantColor(Color.LTGRAY);
    }

    public int getColorPrimary() {
        return colorPrimary;
    }

    public int getColorPrimaryDark() {
        return colorPrimaryDark;
    }

    public int getColorBackground() {
        return colorBackground;
    }

    public int getColorBackgroundDark() {
        return colorBackgroundDark;
    }

    public int getColorAccent() {
        return colorAccent;
    }

    public int getColorTextPrimary() {
        return colorTextPrimary;
    }

    public int getColorTextSecondary() {
        return colorTextSecondary;
    }
}
