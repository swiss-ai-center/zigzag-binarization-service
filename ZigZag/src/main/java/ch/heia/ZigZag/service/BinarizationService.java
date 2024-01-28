package ch.heia.ZigZag.service;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import sugarcube.imapro.zigzag.ZigZagFilter;
import java.net.URI;
import java.util.Arrays;


@Service
public class BinarizationService {

    public static final int DEFAULT_BIN_MODE = 2;

    public static final int DEFAULT_WINDOW_SIZE = 30;

    /**
     * binarizeImageB64 takes a b64 image as input (string), decodes it and performs binarization on it using Zigzag
     * filter with the specified mode.
     * @param original the B64 encoded image to binarize.
     * @param mode determines what mode the zigzag will be used with
     * @return the binarized image as a BufferedImage
     */
    public BufferedImage binarizeImageB64(String original, int mode, int windowSize) {
        byte[] byteImage = null;
        String image = "";

        try {
            image = new String(original.getBytes());
            String imageB64 = image.substring(image.indexOf(",") + 1);
            byteImage = Base64.decodeBase64(imageB64);
            assert (Arrays.equals(byteImage, java.util.Base64.getDecoder().decode(imageB64)));
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            System.out.println(e.getMessage());
        }

        BufferedImage originalImageBuffered = null;


        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(byteImage);
            originalImageBuffered = ImageIO.read(stream);
            stream.close();
        } catch (IOException e) {
            System.out.println("Could not read image byteArray : " + e.getMessage());
        }

        ZigZagFilter filter = new ZigZagFilter(windowSize, 100, mode).setHistoricalWhiteThreshold(245);
        return filter.filterImplementation(originalImageBuffered);
    }

    /**
     * binarizeImageB64 takes an image byte array as input and performs binarization on it using Zigzag
     * filter with the specified mode.
     * @param byteImage the image byte array to binarize.
     * @param mode determines what mode the zigzag will be used with
     * @return the binarized image as a BufferedImage
     */
    public BufferedImage binarizeImageBytes(byte[] byteImage, int mode, int windowSize) {
        BufferedImage originalImageBuffered = null;
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(byteImage);
            originalImageBuffered = ImageIO.read(stream);
            stream.close();
        } catch (IOException e) {
            System.out.println("Could not read image byteArray : " + e.getMessage());
        }
        ZigZagFilter filter = new ZigZagFilter(windowSize, 100, mode).setHistoricalWhiteThreshold(245);
        return filter.filterImplementation(originalImageBuffered);
    }

    /**
     * binarizeImageB64 takes an image url as input (string), downloads it and performs binarization on it using Zigzag
     * filter with the specified mode.
     * @param imageUrl the B64 encoded image to binarize
     * @param mode determines what mode the zigzag will be used with
     * @return the binarized image as a BufferedImage
     */

    public BufferedImage binarizeImageUrl(String imageUrl, int mode, int windowSize) {
        ZigZagFilter filter = new ZigZagFilter(windowSize, 100, mode).setHistoricalWhiteThreshold(245);
        return filter.filterImplementation(downloadImage(imageUrl));
    }


    /**
     * downloadImage take an image url as input, downloads the image and returns it as a BufferedImage.
     * @param url image's url
     * @return downloaded image as a BufferedImage
     */
    private BufferedImage downloadImage(String url) {
        try{
            return ImageIO.read(URI.create(url).toURL());
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Problem reading URL : "  + e.getMessage());
        }
        return null;
    }

}
