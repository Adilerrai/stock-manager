package com.gestion.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

public final class ReportGraphicUtils {

    private static BufferedImage defaultHeaderBanner;

    private ReportGraphicUtils() {}

    /**
     * Génère l'image de fond du bandeau d'en-tête (style Odoo) avec une transition
     * en biseau/courbe douce (S-curve) entre le niveau supérieur (y=84) et le niveau inférieur (y=113).
     */
    public static BufferedImage createHeaderBanner(int width, int height, int rightHeight, int cutStartRight, int cutEndLeft) {
        int scale = 2; // Rendu haute résolution (anti-aliasing)
        BufferedImage image = new BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.scale(scale, scale);

            Color bannerColor = new Color(0xE9, 0xED, 0xF2);
            g.setColor(bannerColor);

            GeneralPath path = new GeneralPath();
            path.moveTo(0, 0);
            path.lineTo(width, 0);
            path.lineTo(width, rightHeight);
            path.lineTo(cutStartRight, rightHeight);

            // Courbe de transition douce (S-curve) entre (cutStartRight, rightHeight) et (cutEndLeft, height)
            float ctrlX = (cutStartRight + cutEndLeft) / 2.0f;
            path.curveTo(ctrlX, rightHeight, ctrlX, height, cutEndLeft, height);

            path.lineTo(0, height);
            path.closePath();

            g.fill(path);
        } finally {
            g.dispose();
        }
        return image;
    }

    public static synchronized BufferedImage getDefaultHeaderBanner() {
        if (defaultHeaderBanner == null) {
            defaultHeaderBanner = createHeaderBanner(595, 113, 84, 355, 305);
        }
        return defaultHeaderBanner;
    }
}
