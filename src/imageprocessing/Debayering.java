package imageprocessing;

import gui.OptionPane;
import main.Picsi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import utils.Parallel;

/**
 * Debayering
 *
 * @author Christoph Stamm
 */
public class Debayering implements IImageProcessor {
    static final int Bypp = 3;

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        Object[] outputTypes = {"Simple", "Good"};
        int ch = OptionPane.showOptionDialog("Debayering algorithms", SWT.ICON_QUESTION, outputTypes, 0);
        if (ch < 0) return null;

        // Debayering of raw input image
        if (ch == 0) return debayering1(inData);
        else return debayering2(inData);
    }

    /**
     * TODO: Simple Debayering
     *
     * @param inData raw data
     * @return RGB image
     */
    private ImageData debayering1(ImageData inData) {
        ImageData outData = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_RGB);

        Parallel.For(0, outData.height, v -> {
            RGB rgb = new RGB(0, 0, 0);

            for (int u = 0; u < outData.width; u++) {
                int b = -1;
                int g = -1;
                int r = -1;

                // do interpolation
                // 3 Colors
                // B -> 0,0 | 0,2 | .... | 2,0 | 2,2 | .... ==> If u % 2 == 0 && v % 2 == 0
                // G -> 0,1 & 1,0 |
                // R -> 1,1 | 3,1 | .... | 1,3 | 3,3 | .... ==> uf u % 2 != 0 && v % 2 != 0
                if (u % 2 == 0 && v % 2 == 0) { // Blue
                    b = inData.getPixel(u, v);

                    if (u == 0 && v == 0) { // top left
                        g = (inData.getPixel(u + 1, v) + inData.getPixel(u, v + 1)) >> 1;
                        r = (inData.getPixel(u + 1, v + 1));
                    } else if (u == inData.width - 1 && v == 0) {   // top right
                        g = (inData.getPixel(u - 1, v) + inData.getPixel(u, v + 1)) >> 1;
                        r = (inData.getPixel(u - 1, v + 1));
                    } else if (u == 0 && v == inData.height - 1) {  // bottom left
                        g = (inData.getPixel(u + 1, v) + inData.getPixel(u, v - 1)) >> 1;
                        r = (inData.getPixel(u + 1, v - 1));
                    } else if (u == inData.width - 1 && v == inData.height - 1) {   // bottom right
                        g = (inData.getPixel(u - 1, v) + inData.getPixel(u, v - 1)) >> 1;
                        r = (inData.getPixel(u - 1, v - 1));
                    } else if (v == 0) {    // top row
                        g = (inData.getPixel(u - 1, v) + inData.getPixel(u + 1, v) + inData.getPixel(u, v + 1)) / 3;
                        r = (inData.getPixel(u - 1, v + 1) + inData.getPixel(u + 1, v + 1)) >> 1;
                    } else if (v == inData.height - 1) {    //bottom row
                        g = (inData.getPixel(u - 1, v) + inData.getPixel(u + 1, v) + inData.getPixel(u, v - 1)) / 3;
                        r = (inData.getPixel(u - 1, v - 1) + inData.getPixel(u + 1, v - 1)) >> 1;
                    } else if (u == 0) {  //left row
                        g = (inData.getPixel(u + 1, v) + inData.getPixel(u, v - 1) + inData.getPixel(u, v + 1)) / 3;
                        r = (inData.getPixel(u + 1, v - 1) + inData.getPixel(u + 1, v + 1)) >> 1;
                    } else if (u == inData.width - 1) { //right row
                        g = (inData.getPixel(u - 1, v) + inData.getPixel(u, v - 1) + inData.getPixel(u, v + 1)) / 3;
                        r = (inData.getPixel(u - 1, v - 1) + inData.getPixel(u - 1, v + 1)) >> 1;
                    } else {    // all inner pixels
                        g = (inData.getPixel(u - 1, v) + inData.getPixel(u + 1, v) + inData.getPixel(u, v - 1) + inData.getPixel(u, v + 1)) >> 2;
                        r = (inData.getPixel(u - 1, v - 1) + inData.getPixel(u + 1, v - 1) + inData.getPixel(u - 1, v + 1) + inData.getPixel(u + 1, v + 1)) >> 2;
                    }
                } else if (u % 2 != 0 && v % 2 != 0) {  // Red
                    r = inData.getPixel(u, v);

                    if (u == inData.width - 1 && v == inData.height - 1) {   // bottom right
                        g = (inData.getPixel(u - 1, v) + inData.getPixel(u, v - 1)) >> 1;
                        b = (inData.getPixel(u - 1, v - 1));
                    } else if (v == inData.height - 1) {    //bottom row
                        g = (inData.getPixel(u - 1, v) + inData.getPixel(u + 1, v) + inData.getPixel(u, v - 1)) / 3;
                        b = (inData.getPixel(u - 1, v - 1) + inData.getPixel(u + 1, v - 1)) >> 1;
                    } else if (u == inData.width - 1) { //right row
                        g = (inData.getPixel(u - 1, v) + inData.getPixel(u, v - 1) + inData.getPixel(u, v + 1)) / 3;
                        b = (inData.getPixel(u - 1, v - 1) + inData.getPixel(u - 1, v + 1)) >> 1;
                    } else {    // all inner pixels
                        g = (inData.getPixel(u - 1, v) + inData.getPixel(u + 1, v) + inData.getPixel(u, v - 1) + inData.getPixel(u, v + 1)) >> 2;
                        b = (inData.getPixel(u - 1, v - 1) + inData.getPixel(u + 1, v - 1) + inData.getPixel(u - 1, v + 1) + inData.getPixel(u + 1, v + 1)) >> 2;
                    }
                } else {
                    g = inData.getPixel(u, v);

                    if (u % 2 != 0) {   //top row green
                        if (u == inData.width - 1 && v == 0) {   // top right
                            b = (inData.getPixel(u - 1, v));
                            r = (inData.getPixel(u, v + 1));
                        } else if (u == inData.width - 1 && v == inData.height - 1) {   // bottom right
                            b = (inData.getPixel(u - 1, v));
                            r = (inData.getPixel(u, v - 1));
                        } else if (v == 0) {    // top row
                            b = (inData.getPixel(u - 1, v) + inData.getPixel(u + 1, v));
                            r = (inData.getPixel(u, v + 1));
                        } else if (v == inData.height - 1) {    //bottom row
                            b = (inData.getPixel(u - 1, v) + inData.getPixel(u + 1, v)) >> 1;
                            r = (inData.getPixel(u, v - 1));
                        } else if (u == inData.width - 1) { //right row
                            b = (inData.getPixel(u - 1, v));
                            r = (inData.getPixel(u, v - 1) + inData.getPixel(u, v + 1)) >> 1;
                        } else {    // all inner pixels
                            b = (inData.getPixel(u - 1, v) + inData.getPixel(u + 1, v)) >> 1;
                            r = (inData.getPixel(u, v - 1) + inData.getPixel(u, v + 1)) >> 1;
                        }
                    } else {    //bottom row green
                        if (u == 0 && v == inData.height - 1) {  // bottom left
                            b = (inData.getPixel(u, v - 1));
                            r = (inData.getPixel(u + 1, v));
                        } else if (u == inData.width - 1 && v == inData.height - 1) {   // bottom right
                            b = (inData.getPixel(u, v - 1));
                            r = (inData.getPixel(u - 1, v));
                        } else if (v == inData.height - 1) {    //bottom row
                            b = (inData.getPixel(u, v - 1));
                            r = (inData.getPixel(u - 1, v) + inData.getPixel(u + 1, v)) >> 1;
                        } else if (u == 0) {  //left row
                            b = (inData.getPixel(u, v - 1) + inData.getPixel(u, v + 1)) >> 1;
                            r = (inData.getPixel(u + 1, v));
                        } else if (u == inData.width - 1) { //right row
                            b = (inData.getPixel(u, v - 1) + inData.getPixel(u, v + 1)) >> 1;
                            r = (inData.getPixel(u - 1, v));
                        } else {    // all inner pixels
                            b = (inData.getPixel(u, v - 1) + inData.getPixel(u, v + 1)) >> 1;
                            r = (inData.getPixel(u - 1, v) + inData.getPixel(u + 1, v)) >> 1;
                        }
                    }
                }

                rgb.red = r;
                rgb.green = g;
                rgb.blue = b;

                outData.setPixel(u, v, outData.palette.getPixel(rgb));
            }
        });
        return outData;
    }

    /**
     * Advanced Debayering
     *
     * @param inData raw data
     * @return RGB image
     */
    private ImageData debayering2(ImageData inData) {
        ImageData outData = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_RGB);

        // interpolation of green channel
        Parallel.For(0, outData.height, v -> {
            RGB rgb = new RGB(0, 0, 0);

            //TODO green

            for (int u = 0; u < outData.width; u++) {
                outData.setPixel(u, v, outData.palette.getPixel(rgb));
            }
        });

        // interpolation of blue and red channels
        Parallel.For(0, outData.height, v -> {
            RGB rgb = new RGB(0, 0, 0);

            //TODO implement advanced method


            for (int u = 0; u < outData.width; u++) {
                outData.setPixel(u, v, outData.palette.getPixel(rgb));
            }
        });
        return outData;
    }

}
