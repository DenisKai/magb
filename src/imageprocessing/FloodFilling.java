package imageprocessing;

import main.Picsi;
import org.eclipse.swt.graphics.ImageData;

import java.awt.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * Flood Filling
 *
 * @author Christoph Stamm
 */
public class FloodFilling implements IImageProcessor {
    public static int s_background = 0; // white
    public static int s_foreground = 1; // black

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData input, int imageType) {
        final int threshold = Binarization.otsuThreshold(input);

        ImageData binary = Binarization.binarize(input, threshold, false, false);

        int nLabels = floodFill(binary);
        System.out.println("Anzahl Muenzen: " + nLabels);

        return falseColor(binary, nLabels + 2);
    }

    /**
     * Labeling of a binarized grayscale image
     *
     * @param imageData input: grayscale image with intensities 0 and 1 only, output: labeled foreground regions
     * @return number of regions
     */
    public static int floodFill(ImageData imageData) {
        assert ImageProcessing.determineImageType(imageData) == Picsi.IMAGE_TYPE_GRAY;

        int m = 2; // label number/value of pixel

        boolean[][] visited = new boolean[imageData.width][imageData.height];
        Queue<Point> queue = new LinkedList<>();

        int[] neighbour_x = {1, -1, 0, 0};
        int[] neighbour_y = {0, 0, 1, -1};

        for (int v = 0; v < imageData.height; v++) {
            for (int u = 0; u < imageData.width; u++) {
                if (imageData.getPixel(u, v) == s_foreground && !visited[u][v]) {    //add Vordergrund to queue
                    queue.add(new Point(u, v));
                    visited[u][v] = true;

                    while (!queue.isEmpty()) {
                        Point point = queue.poll();
                        int x = point.x;
                        int y = point.y;

                        imageData.setPixel(x, y, m);

                        // add neighbours
                        for (int i = 0; i < 4; i++) {
                            int x_n = x + neighbour_x[i];
                            int y_n = y + neighbour_y[i];

                            if (x_n >= 0 &&
                                    x_n < imageData.width &&
                                    y_n >= 0 &&
                                    y_n < imageData.height &&
                                    !visited[x_n][y_n] &&
                                    imageData.getPixel(x_n, y_n) == s_foreground) {
                                queue.add(new Point(x_n, y_n));
                                visited[x_n][y_n] = true;
                            }
                        }
                    }

                    m++;
                }
            }
        }

        // Background has to be white -> 255 in grayscale
        for (int v = 0; v < imageData.height; v++) {
            for (int u = 0; u < imageData.width; u++) {
                if (!visited[u][v]) {
                    imageData.setPixel(u, v, 255);
                }
            }
        }

        return m - 2;   // -2 not counting background and 'default' foreground
    }

    /**
     * False color presentation of labeled grayscale image
     *
     * @param inData labeled grayscale image
     * @param n      number of different false colors (<= 256)
     * @return indexed color image
     */
    public static ImageData falseColor(ImageData inData, int n) {
        assert ImageProcessing.determineImageType(inData) == Picsi.IMAGE_TYPE_GRAY;
        assert 0 < n && n <= 256;

        ImageData outData = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_RGB);

        Random random = new Random();
        Color[] colors = new Color[n];
        colors[0] = Color.WHITE;    // background
        colors[1] = Color.BLACK;    // foreground (shouldn't exist -> all labeled

        for (int i = 2; i < n; i++) {
            colors[i] = new Color(
                    random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256));
        }


        for (int v = 0; v < inData.height; v++) {
            for (int u = 0; u < inData.width; u++) {
                int value = inData.getPixel(u, v);
                if (value != 255) {  // all non-Background pixels will be colored
                    outData.setPixel(u, v, colors[value].getRGB());
                } else {
                    outData.setPixel(u, v, Color.WHITE.getRGB());
                }
            }
        }

        return outData;
    }
}
