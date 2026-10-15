package com.gestion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Service
public class ImageCompressionService {

    private static final Logger log = LoggerFactory.getLogger(ImageCompressionService.class);

    private static final float COMPRESSION_QUALITY = 0.7f; // 70% qualité
    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 600;

    public byte[] compressImage(byte[] originalImage, String contentType) throws IOException {
        if (originalImage == null || originalImage.length == 0) {
            return originalImage;
        }

        try {
            // Lire l'image originale
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(originalImage));
            if (bufferedImage == null) {
                log.warn("Format d'image non décodable par ImageIO (contentType: {}). Image conservée sans compression.", contentType);
                return originalImage;
            }

            // Redimensionner et convertir en TYPE_INT_RGB sur fond blanc
            // Cela élimine le canal alpha (ARGB, ABGR) et normalise l'espace colorimétrique,
            // évitant l'erreur JPEG native "Bogus input colorspace" (notamment pour les logos PNG).
            BufferedImage rgbImage = resizeAndConvertToRgb(bufferedImage);

            // Compresser en JPEG
            return compressBufferedImage(rgbImage);
        } catch (Exception e) {
            log.warn("Erreur lors de la compression de l'image ({}) : {}. Utilisation des données d'origine.",
                    contentType, e.getMessage());
            return originalImage;
        }
    }

    private BufferedImage resizeAndConvertToRgb(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculer nouvelles dimensions en gardant le ratio
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        if (originalWidth > MAX_WIDTH || originalHeight > MAX_HEIGHT) {
            double widthRatio = (double) MAX_WIDTH / originalWidth;
            double heightRatio = (double) MAX_HEIGHT / originalHeight;
            double ratio = Math.min(widthRatio, heightRatio);

            newWidth = Math.max(1, (int) (originalWidth * ratio));
            newHeight = Math.max(1, (int) (originalHeight * ratio));
        }

        // Si l'image est déjà TYPE_INT_RGB, sans alpha et aux bonnes dimensions, on peut la conserver
        if (newWidth == originalWidth && newHeight == originalHeight
                && originalImage.getType() == BufferedImage.TYPE_INT_RGB
                && !hasAlpha(originalImage)) {
            return originalImage;
        }

        // Toujours créer un BufferedImage TYPE_INT_RGB avec fond blanc pour garantir la compatibilité JPEG
        BufferedImage rgbImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = rgbImage.createGraphics();
        try {
            g2d.setPaint(Color.WHITE);
            g2d.fillRect(0, 0, newWidth, newHeight);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        } finally {
            g2d.dispose();
        }

        return rgbImage;
    }

    private boolean hasAlpha(BufferedImage image) {
        if (image == null) return false;
        ColorModel cm = image.getColorModel();
        return cm != null && cm.hasAlpha();
    }

    private byte[] compressBufferedImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (writers.hasNext()) {
            ImageWriter writer = writers.next();
            try {
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(COMPRESSION_QUALITY);
                }

                try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                    writer.setOutput(ios);
                    writer.write(null, new IIOImage(image, null, null), param);
                }
            } finally {
                writer.dispose();
            }
        } else {
            ImageIO.write(image, "jpeg", baos);
        }

        return baos.toByteArray();
    }

    public byte[] decompressImage(byte[] compressedImage) {
        return compressedImage;
    }
}

