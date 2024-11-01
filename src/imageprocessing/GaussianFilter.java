package imageprocessing;

import gui.OptionPane;
import main.Picsi;
import org.eclipse.swt.graphics.ImageData;
import utils.Parallel;

public class GaussianFilter implements IImageProcessor {
    @Override
    public boolean isEnabled(int imageType) {
        return (imageType == Picsi.IMAGE_TYPE_RGBA || imageType == Picsi.IMAGE_TYPE_RGB || imageType == Picsi.IMAGE_TYPE_GRAY || imageType == Picsi.IMAGE_TYPE_GRAY32);
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        int sigma = OptionPane.showIntegerDialog("Sigma (min. 1)?", 1);
        if (sigma < 1) sigma = 1;

        return gaussian(sigma, inData);
    }

    private ImageData gaussian(int sigma, ImageData in) {
        ImageData intermediate = (ImageData) in.clone();
        ImageData out = (ImageData) in.clone();

        // create gaussian 1D-filter
        float[] filter = createGaussianMatrix(sigma);
        int center = (int) Math.ceil(filter.length / 2f);

        // Horizontal run of image
        Parallel.For(0, intermediate.height, v -> {
            for (int u = 0; u < intermediate.width; u++) {
                // calculate gaussian-sum for each pixel
                float sum = 0f;
                for (int i = 0; i < filter.length; i++) {
                    int offset = i - center;
                    // Edge-cases: u + offset < 0 || u + offset >= width
                    if (u + offset < 0 || u + offset >= in.width) {
                        sum += in.getPixel(u, v) * filter[i];
                    } else {
                        sum += in.getPixel(u + offset, v) * filter[i];
                    }
                }

                sum = ImageProcessing.clamp8(sum);
                intermediate.setPixel(u, v, (int) sum);
            }
        });

        // Vertical run of image (using intermediate out-pixel-values from horizontal run)
        Parallel.For(0, intermediate.width, u -> {
            for (int v = 0; v < intermediate.height; v++) {
                float sum = 0;
                for (int i = 0; i < filter.length; i++) {
                    int offset = i - center;

                    // Edge-cases: v + offset < 0 || v + offset >= height
                    if (v + offset < 0 || v + offset >= intermediate.height) {
                        sum += intermediate.getPixel(u, v) * filter[i];
                    } else {
                        sum += intermediate.getPixel(u, v + offset) * filter[i];
                    }
                }

                sum = ImageProcessing.clamp8(sum);
                out.setPixel(u, v, (int) sum);
            }
        });

        return out;
    }

    // Create gaussian-filter-matrix
    private float[] createGaussianMatrix(int sigma) {
        int size = (int) Math.ceil(5 * sigma);
        if (size % 2 == 0) size++;

        int center = (int) Math.ceil(size / 2f);

        float[] gaussFilter = new float[size];
        float sum = 0;
        for (int i = 0; i < size; i++) {
            int x = i - center;
            gaussFilter[i] = (float) Math.exp(-(x * x) / (2f * (sigma * sigma)));
            sum += gaussFilter[i];
        }

        // Normalize values
        for (int i = 0; i < size; i++) {
            gaussFilter[i] /= sum;
        }

        return gaussFilter;
    }
}
