package com.ceramique.service;

import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Service
public class ImageCompressionService {

    private static final float COMPRESSION_QUALITY = 0.7f; // 70% qualité
    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 600;

    public byte[] compressImage(byte[] originalImage, String contentType) throws IOException {
        if (originalImage == null || originalImage.length == 0) {
            return originalImage;
        }

        // Lire l'image originale
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(originalImage));
        if (bufferedImage == null) {
            return originalImage;
        }

        // Redimensionner si nécessaire
        BufferedImage resizedImage = resizeImage(bufferedImage);

        // Compresser
        return compressBufferedImage(resizedImage, getFormatName(contentType));
    }

    private BufferedImage resizeImage(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculer nouvelles dimensions en gardant le ratio
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        if (originalWidth > MAX_WIDTH || originalHeight > MAX_HEIGHT) {
            double widthRatio = (double) MAX_WIDTH / originalWidth;
            double heightRatio = (double) MAX_HEIGHT / originalHeight;
            double ratio = Math.min(widthRatio, heightRatio);

            newWidth = (int) (originalWidth * ratio);
            newHeight = (int) (originalHeight * ratio);
        }

        if (newWidth == originalWidth && newHeight == originalHeight) {
            return originalImage;
        }

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return resizedImage;
    }

    private byte[] compressBufferedImage(BufferedImage image, String formatName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if ("jpeg".equalsIgnoreCase(formatName) || "jpg".equalsIgnoreCase(formatName)) {
            // Compression JPEG avec qualité
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(COMPRESSION_QUALITY);

                try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                    writer.setOutput(ios);
                    writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
                }
                writer.dispose();
            }
        } else {
            // Pour PNG et autres formats
            ImageIO.write(image, formatName, baos);
        }

        return baos.toByteArray();
    }

    private String getFormatName(String contentType) {
        if (contentType == null) return "jpeg";
        
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "jpeg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/bmp" -> "bmp";
            default -> "jpeg";
        };
    }

    public byte[] decompressImage(byte[] compressedImage) {
        // Pour les images, la décompression se fait automatiquement lors de la lecture
        // Cette méthode peut être utilisée pour des traitements supplémentaires si nécessaire
        return compressedImage;
    }
}