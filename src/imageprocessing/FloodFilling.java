package imageprocessing;

import imageprocessing.grayValueConverter.GrayValue;
import main.Picsi;
import org.eclipse.swt.graphics.ImageData;

import java.awt.*;
import java.util.LinkedList;
import java.util.Queue;

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
        ImageData grayData = GrayValue.extractGrayValue(input);
        final int threshold = Binarization.otsuThreshold(grayData);

        ImageData binary = Binarization.binarize(grayData, threshold, false, true);

        int nLabels = floodFill(binary);
        System.out.println("Anzahl Mnzen: " + nLabels);

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
        boolean particle_flag = false;
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(0, 0));

        // TODO
        for (int u = 0; u < imageData.width; u++) {
            for (int v = 0; v < imageData.height; v++) {

                if (imageData.getPixel(u, v) == 1) {    //add to queue
                    queue.add(new Point(u, v));
                }

                while(!queue.isEmpty()) {
                    particle_flag = true;
                    Point point = queue.poll();
                    int x = point.x;
                    int y = point.y;

                    if (x < 0 || x >= imageData.width || y < 0 || y >= imageData.height) {
                        continue;
                    }

                    if (imageData.getPixel(x, y) != 1) {
                        continue; // Ignore points that are not the target color
                    }

                    imageData.setPixel(x, y, m);

                    // add neighbours
                    queue.add(new Point(x + 1, y)); //right
                    queue.add(new Point(x, y + 1)); //lower
                    queue.add(new Point(x - 1, y)); //left
                    queue.add(new Point(x, y - 1)); //upper
                }

                if (particle_flag) {
                    m++;
                }
            }
        }

        return m;
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

        // TODO

        return null;
    }
}
