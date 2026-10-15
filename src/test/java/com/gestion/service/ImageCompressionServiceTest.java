package com.gestion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ImageCompressionServiceTest {

    private ImageCompressionService imageCompressionService;

    @BeforeEach
    void setUp() {
        imageCompressionService = new ImageCompressionService();
    }

    @Test
    void testCompressTransparentPngSmallImage_NoBogusColorspaceException() throws IOException {
        // Reproduit exactement le cas qui provoquait "Bogus input colorspace" :
        // Un logo PNG de petite taille (<= 800x600) avec canal alpha / transparence (TYPE_4BYTE_ABGR)
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(255, 0, 0, 128)); // Rouge semi-transparent
        g.fillRect(0, 0, 200, 200);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] pngBytes = baos.toByteArray();

        // Appel de la méthode
        byte[] result = imageCompressionService.compressImage(pngBytes, "image/png");

        assertNotNull(result);
        assertTrue(result.length > 0);
        // Doit être un JPEG valide (magic bytes FF D8)
        assertEquals((byte) 0xFF, result[0]);
        assertEquals((byte) 0xD8, result[1]);
    }

    @Test
    void testCompressLargeTransparentPng_ResizedAndCompressed() throws IOException {
        BufferedImage img = new BufferedImage(1200, 900, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0, 128, 255, 200));
        g.fillOval(50, 50, 800, 600);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] pngBytes = baos.toByteArray();

        byte[] result = imageCompressionService.compressImage(pngBytes, "image/png");

        assertNotNull(result);
        assertTrue(result.length > 0);
        assertEquals((byte) 0xFF, result[0]);
        assertEquals((byte) 0xD8, result[1]);

        BufferedImage readBack = ImageIO.read(new java.io.ByteArrayInputStream(result));
        assertNotNull(readBack);
        assertTrue(readBack.getWidth() <= 800);
        assertTrue(readBack.getHeight() <= 600);
    }

    @Test
    void testCompressJpeg_Success() throws IOException {
        BufferedImage img = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, 300, 300);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", baos);
        byte[] jpegBytes = baos.toByteArray();

        byte[] result = imageCompressionService.compressImage(jpegBytes, "image/jpeg");

        assertNotNull(result);
        assertTrue(result.length > 0);
        assertEquals((byte) 0xFF, result[0]);
        assertEquals((byte) 0xD8, result[1]);
    }

    @Test
    void testCorruptedOrNonImage_ReturnsOriginalGracefully() throws IOException {
        byte[] dummyData = "Not an image data".getBytes();
        byte[] result = imageCompressionService.compressImage(dummyData, "image/jpeg");
        assertArrayEquals(dummyData, result);
    }
}
