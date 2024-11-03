package imageprocessing;

import gui.OptionPane;
import main.Picsi;
import org.eclipse.swt.graphics.ImageData;
import utils.Parallel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MedianFilter implements IImageProcessor {
    @Override
    public boolean isEnabled(int imageType) {
        return (imageType == Picsi.IMAGE_TYPE_RGBA || imageType == Picsi.IMAGE_TYPE_RGB || imageType == Picsi.IMAGE_TYPE_GRAY || imageType == Picsi.IMAGE_TYPE_GRAY32);
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        int filterSize = OptionPane.showIntegerDialog("Filtergrösse (min. 3)?", 3);
        if (filterSize < 3) filterSize = 3;

        return medianFilter(filterSize, inData);
    }

    public static ImageData medianFilter(int filterSize, ImageData in) {
        ImageData intermediate = (ImageData) in.clone();
        ImageData out = (ImageData) in.clone();

        // create 1D-median filter
        int[] filter = createMedianFilter(filterSize);
        int filterCenter = (int) Math.ceil(filter.length / 2f);

        // Horizontal run of image
        Parallel.For(0, intermediate.height, v -> {
            for (int u = 0; u < intermediate.width; u++) {
                List<Float> values = new ArrayList<>();
                for (int i = 0; i < filter.length; i++) {
                    int offset = i - filterCenter;

                    for (int j = 0; j < filter[i]; j++) {   // for weighted filter
                        if (u + offset < 0 || u + offset >= in.width) {
                            values.add((float) in.getPixel(u, v));
                        } else {
                            values.add((float) in.getPixel(u + offset, v));
                        }
                    }
                }

                values.sort(Comparator.naturalOrder());
                int value = ImageProcessing.clamp8(Math.round(values.get(values.size() / 2)));
                intermediate.setPixel(u, v, value);
            }
        });

        // Vertical run of image (using intermediate out-pixel-values from horizontal run)
        Parallel.For(0, intermediate.width, u -> {
            for (int v = 0; v < intermediate.height; v++) {
                List<Float> values = new ArrayList<>();
                for (int i = 0; i < filter.length; i++) {
                    int offset = i - filterCenter;

                    for (int j = 0; j < filter[i]; j++) {   // for weighted filter
                        if (v + offset < 0 || v + offset >= in.width) {
                            values.add((float) intermediate.getPixel(u, v));
                        } else {
                            values.add((float) intermediate.getPixel(u, v + offset));
                        }
                    }
                }

                values.sort(Comparator.naturalOrder());
                int value = ImageProcessing.clamp8(Math.round(values.get(values.size() / 2)));
                out.setPixel(u, v, value);
            }
        });

        return out;
    }

    // Create median 1D-Filter
    private static int[] createMedianFilter(int size) {
        if (size % 2 == 0) size++;
        int[] filter = new int[size];
        for (int i = 0; i < size; i++) {
            filter[i] = 1;
        }

        return filter;
    }
}
