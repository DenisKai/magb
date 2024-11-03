package imageprocessing;

import gui.OptionPane;
import imageprocessing.grayValueConverter.GrayValue;
import main.Picsi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import utils.Parallel;

public class EdgeDetector implements IImageProcessor {
    final float[][] H_x = {
            {-3, 0, 3},
            {-10, 0, 10},
            {-3, 0, 3}
    };

    final float[][] H_y = {
            {-3, -10, -3},
            {0, 0, 0},
            {3, 10, 3}
    };
    final float H_norm_factor = calculateAbsoluteSum(H_x);  // Should be 32
    final int intensity_offset = 128;

    @Override
    public boolean isEnabled(int imageType) {
        return (imageType == Picsi.IMAGE_TYPE_RGBA || imageType == Picsi.IMAGE_TYPE_RGB || imageType == Picsi.IMAGE_TYPE_GRAY || imageType == Picsi.IMAGE_TYPE_GRAY32);
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        Object[] operations = {"Horizontal", "Vertical", "Beide"};
        int ch = OptionPane.showOptionDialog("Welche Kanten sollen ausgegeben werden?", SWT.ICON_INFORMATION, operations, 0);
        if (ch < 0 || ch > 2) ch = 2;

        if (imageType == Picsi.IMAGE_TYPE_RGB || imageType == Picsi.IMAGE_TYPE_RGBA) {
            inData = GrayValue.extractGrayValue(inData);
        }

        // Glättung Bild
        //inData = GaussianFilter.gaussian(3, inData);

        ImageData out = detectEdges(inData, ch);
        return out;
    }

    private ImageData detectEdges(ImageData inData, int edgeType) {
        ImageData horizontal_image = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_GRAY);
        float[][] horizontal_weights = new float[inData.width][inData.height];

        int x_center = (int) Math.ceil(H_x[0].length / 2f);
        int y_center = (int) Math.ceil(H_x.length / 2f);

        // Horizontal edge detection (H_y)
        Parallel.For(0, inData.height, v -> {
            for (int u = 0; u < inData.width; u++) {
                // edge case: u+offset_u < 0 || v+offset_v <0 || u+offset_u >= width || v+offset_v >= height
                float sum = 0;
                for (int x = 0; x < H_y[0].length; x++) {
                    for (int y = 0; y < H_y.length; y++) {
                        int u_off = x - x_center;
                        int v_off = y - y_center;

                        if ((u + u_off < 0 && v + v_off < 0) || (u + u_off >= inData.width && v + v_off >= inData.height)) {
                            sum += H_y[y][x] * inData.getPixel(u, v);
                        } else if (u + u_off < 0 || u + u_off >= inData.width) {
                            sum += H_y[y][x] * inData.getPixel(u, v + v_off);
                        } else if (v + v_off < 0 || v + v_off >= inData.height) {
                            sum += H_y[y][x] * inData.getPixel(u + u_off, v);
                        } else {
                            sum += H_y[y][x] * inData.getPixel(u + u_off, v + v_off);
                        }
                    }
                }
                sum /= H_norm_factor;
                horizontal_weights[u][v] = sum;
            }
        });

        if (edgeType == 0) {
            for (int v = 0; v < horizontal_weights[0].length; v++) {
                for (int u = 0; u < horizontal_weights.length; u++) {
                    horizontal_image.setPixel(u, v, ImageProcessing.clamp8((int) (horizontal_weights[u][v] + intensity_offset)));
                }
            }

            return horizontal_image;
        }

        // Vertical edge detection
        ImageData vertical_image = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_GRAY);
        float[][] vertical_weights = new float[inData.width][inData.height];

        Parallel.For(0, inData.height, v -> {
            for (int u = 0; u < inData.width; u++) {
                // edge case: u+offset_u < 0 || v+offset_v <0 || u+offset_u >= width || v+offset_v >= height
                float sum = 0;
                for (int x = 0; x < H_x[0].length; x++) {
                    for (int y = 0; y < H_x.length; y++) {
                        int u_off = x - x_center;
                        int v_off = y - y_center;

                        if ((u + u_off < 0 && v + v_off < 0) || (u + u_off >= inData.width && v + v_off >= inData.height)) {
                            sum += H_x[y][x] * inData.getPixel(u, v);
                        } else if (u + u_off < 0 || u + u_off >= inData.width) {
                            sum += H_x[y][x] * inData.getPixel(u, v + v_off);
                        } else if (v + v_off < 0 || v + v_off >= inData.height) {
                            sum += H_x[y][x] * inData.getPixel(u + u_off, v);
                        } else {
                            sum += H_x[y][x] * inData.getPixel(u + u_off, v + v_off);
                        }
                    }
                }
                sum /= H_norm_factor;
                vertical_weights[u][v] = sum;
            }
        });

        if (edgeType == 1) {
            for (int v = 0; v < vertical_weights[0].length; v++) {
                for (int u = 0; u < vertical_weights.length; u++) {
                    vertical_image.setPixel(u, v, ImageProcessing.clamp8((int) (vertical_weights[u][v] + intensity_offset)));
                }
            }

            return vertical_image;
        }


        // Both edge-types combined
        for (int v = 0; v < inData.height; v++) {
            for (int u = 0; u < inData.width; u++) {
                float sum = (float) Math.sqrt((horizontal_weights[u][v] * horizontal_weights[u][v]) + (vertical_weights[u][v] * vertical_weights[u][v]));
                vertical_image.setPixel(u, v, ImageProcessing.clamp8(sum));
            }
        }

        return vertical_image;
    }

    private int calculateAbsoluteSum(float[][] array) {
        int sum = 0;

        // Traverse through each element of the 2D array
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                // Add the absolute value of each element to the sum
                sum += Math.abs(array[i][j]);
            }
        }

        return sum;
    }

    private float[][] convolveX(float[][] in, float[] filterX, float[][] out) {
        int center = (int) Math.ceil(filterX.length / 2f);
        float filter_sum = 0;
        for (int i = 0; i < filterX.length; i++) {
            filter_sum += Math.abs(filterX[i]);
        }

        for (int v = 0; v < in.length; v++) {
            for (int u = 0; u < in[0].length; u++) {
                float sum = 0;

                for (int i = 0; i < filterX.length; i++) {
                    int offset = i - center;

                    // edge cases u + offse < 0|| u + offset >= in[0].length
                    if (u + offset < 0 || u + offset >= in[0].length) {
                        sum += in[v][u] * filterX[i];
                    } else {
                        sum += in[v][u + offset] * filterX[i];
                    }
                }

                sum /= filter_sum;
                out[v][u] = sum;
            }
        }
        return out;
    }

    private float[][] convolveY(float[][] in, float[] filterY, float[][] out) {
        int center = (int) Math.ceil(filterY.length / 2f);
        int filter_sum = 0;
        for (int i = 0; i < filterY.length; i++) {
            filter_sum += Math.abs(filterY[i]);
        }

        for (int v = 0; v < in.length; v++) {
            for (int u = 0; u < in[0].length; u++) {

                float sum = 0;
                for (int j = 0; j < filterY.length; j++) {
                    int offset_v = j - center;
                    // edge cases v + offset < 0 || v + offset >= in.length
                    if (v + offset_v < 0 || v + offset_v >= in.length) {
                        sum += in[v][u] * filterY[j];
                    } else {
                        sum += in[v + offset_v][u] * filterY[j];
                    }
                }

                sum /= filter_sum;
                out[v][u] = sum;
            }
        }

        return out;
    }
}
