package imageprocessing;

import gui.OptionPane;
import imageprocessing.grayValueConverter.GrayValue;
import main.Picsi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import utils.Parallel;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Corner detection
 *
 * @author Christoph Stamm
 */
public class CornerDetection implements IImageProcessor {
    private static class Corner implements Comparable<Corner> {
        public int m_u, m_v;
        public double m_q;

        public Corner(int u, int v, double q) {
            m_u = u;
            m_v = v;
            m_q = q;
        }

        public int compareTo(Corner c) {
            return Double.compare(c.m_q, m_q);
        }

        public int dist2(Corner c) {
            int du = c.m_u - m_u;
            int dv = c.m_v - m_v;
            return du * du + dv * dv;
        }
    }

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY || imageType == Picsi.IMAGE_TYPE_RGB || imageType == Picsi.IMAGE_TYPE_INDEXED;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        // let the user choose the corner detection method
        Object[] methods = {"Harris", "Median-Difference"};
        int f = OptionPane.showOptionDialog("Corner Detection Method", SWT.ICON_INFORMATION, methods, 0);

        if (imageType == Picsi.IMAGE_TYPE_RGB || imageType == Picsi.IMAGE_TYPE_INDEXED) {
            inData = GrayValue.extractGrayValue(inData);
        }

        switch (f) {
            case 0:
                return harris(inData);
            case 1:
                return median(inData);
            default:
                return null;
        }

    }

    public static ImageData harris(ImageData inData) {
        float alpha = 0.05f;
        float threshold = 20000;
        float dmin = 10;

        Float aFloat = OptionPane.showFloatDialog("alpha", alpha);
        if (aFloat == null) return null;
        alpha = aFloat;

        Float tFloat = OptionPane.showFloatDialog("threshold", threshold);
        if (tFloat == null) return null;
        threshold = tFloat;

        Float dFloat = OptionPane.showFloatDialog("dmin", dmin);
        if (dFloat == null) return null;
        dmin = dFloat;

        // filters
        final float[] hp = {2.f / 9, 5.f / 9, 2.f / 9}; // Gauss filter
        final float[] hd = {-0.453014f, 0, 0.453014f}; // derivative filter
        final float[] hb = {1.f / 64, 6.f / 64, 15.f / 64, 20.f / 64, 15.f / 64, 6.f / 64, 1.f / 64}; // Gauss filter

        final float[][] inDataF = new float[inData.height][inData.width];
        final float[][] A = new float[inData.height][inData.width];
        final float[][] B = new float[inData.height][inData.width];
        final float[][] C = new float[inData.height][inData.width];

        // copy inData to float array
        Parallel.For(0, inData.height, v -> {
            for (int u = 0; u < inData.width; u++) {
                inDataF[v][u] = inData.getPixel(u, v);
            }
        });

        // pre-filtering
        convolveXY(inDataF, hp, hp);

        // Compute derivatives
        float[][] I_x = new float[inData.height][inData.width];
        float[][] I_y = new float[inData.height][inData.width];
        float[][] I_xy = new float[inData.height][inData.width];

        convolveX(inDataF, hd, I_x);    // Partial derivative I_x
        convolveY(inDataF, hd, I_y);    // Partial derivative I_y

        // Build Ix^2, IxIy, Iy^2
        for (int v = 0; v < inData.height; v++) {
            for (int u = 0; u < inData.width; u++) {
                I_xy[v][u] = I_x[v][u] * I_y[v][u];
                I_x[v][u] = I_x[v][u] * I_x[v][u];
                I_y[v][u] = I_y[v][u] * I_y[v][u];
            }
        }

        // Gaussian filtering of Ix^2, Iy^2, IxIy
        float[][] I_x_g = new float[inData.height][inData.width];
        float[][] I_y_g = new float[inData.height][inData.width];
        convolveX(I_x, hb, I_x_g);
        convolveY(I_y, hb, I_y_g);
        convolveXY(I_xy, hb, hb);

        // Compute CRF (and Eigenvalues)
        float[][] q = new float[inData.height][inData.width];
        // Strukturmatrix
        // {{A, C}    =    {{I_x^2, I_xy},
        //  {C, B}}         {I_xy, I_y^2}}
        for (int v = 0; v < inData.height; v++) {
            for (int u = 0; u < inData.width; u++) {
                float M_A = I_x_g[v][u];
                float M_B = I_y_g[v][u];
                float M_C = I_xy[v][u];

                float detM = M_A * M_B - (M_C * M_C);
                float traceM = M_A + M_B;
                q[v][u] = detM - alpha * (traceM * traceM);
            }
        }

        // collect corner points in parallel
        final float th = threshold;
        List<Corner> corners = new LinkedList<>();
        ImageData outData = (ImageData) inData.clone();

        Parallel.For(0, outData.height,
                // creator
                () -> new LinkedList<Corner>(),
                // loop body
                (v, list) -> {
                    for (int u = 0; u < outData.width; u++) {
                        if (q[v][u] > th && isLocalMax(q, u, v)) {
                            // add corner
                            list.add(new Corner(u, v, q[v][u]));
                        }

                        // copy darker input image to output image
                        outData.setPixel(u, v, inData.getPixel(u, v) / 2);
                    }
                },
                // reducer
                list -> corners.addAll(list)
        );

        // clean-up neighbors
        List<Corner> good = cleanUpNeighbors(corners, dmin);

        drawCorners(outData, good);
        return outData;
    }

    public static ImageData median(ImageData inData) {
        ImageData medData = MedianFilter.medianFilter(3, inData);

        // let the user choose the parameters
        Integer tInt = OptionPane.showIntegerDialog("threshold", 50);
        if (tInt == null) return null;

        Integer dInt = OptionPane.showIntegerDialog("dmin", 10);
        if (dInt == null) return null;

        // collect corner points in parallel
        final int th = tInt;
        List<Corner> corners = new LinkedList<>();
        ImageData outData = (ImageData) inData.clone();

        Parallel.For(0, outData.height,
                // creator
                () -> new LinkedList<Corner>(),
                // loop body
                (v, list) -> {
                    for (int u = 0; u < outData.width; u++) {
                        final int p1 = inData.getPixel(u, v);
                        final int p2 = medData.getPixel(u, v);
                        final int val = Math.abs(p1 - p2);

                        if (val > th) {
                            // add corner
                            list.add(new Corner(u, v, val));
                        }

                        // copy darker input image to output image
                        outData.setPixel(u, v, inData.getPixel(u, v) / 2);
                    }
                },
                // reducer
                list -> corners.addAll(list)
        );

        // clean-up neighbors
        List<Corner> good = cleanUpNeighbors(corners, dInt);

        drawCorners(outData, good);
        return outData;
    }

    private static void drawCorners(ImageData outData, List<Corner> corners) {
        Parallel.forEach(corners, c -> {
            int h = outData.height;
            int w = outData.width;

            // draw white cross
            if (c.m_v >= 2) outData.setPixel(c.m_u, c.m_v - 2, 255);
            if (c.m_v >= 1) outData.setPixel(c.m_u, c.m_v - 1, 255);
            if (c.m_v < h - 1) outData.setPixel(c.m_u, c.m_v + 1, 255);
            if (c.m_v < h - 2) outData.setPixel(c.m_u, c.m_v + 2, 255);
            outData.setPixel(c.m_u, c.m_v, 255);
            if (c.m_u >= 2) outData.setPixel(c.m_u - 2, c.m_v, 255);
            if (c.m_u >= 1) outData.setPixel(c.m_u - 1, c.m_v, 255);
            if (c.m_u < w - 1) outData.setPixel(c.m_u + 1, c.m_v, 255);
            if (c.m_u < w - 2) outData.setPixel(c.m_u + 2, c.m_v, 255);

        });
    }

    private static boolean isLocalMax(float[][] q, int u, int v) {
        float qc = q[v][u];

        if (u > 0) {
            if (v > 0) {
                if (qc < q[v - 1][u - 1]) return false;
            }
            if (qc < q[v][u - 1]) return false;
            if (v < q.length - 1) {
                if (qc < q[v + 1][u - 1]) return false;
            }
        }
        if (v > 0) {
            if (qc < q[v - 1][u]) return false;
        }
        if (v < q.length - 1) {
            if (qc < q[v + 1][u]) return false;
        }
        if (u < q[v].length - 1) {
            if (v > 0) {
                if (qc < q[v - 1][u + 1]) return false;
            }
            if (qc < q[v][u + 1]) return false;
            if (v < q.length - 1) {
                if (qc < q[v + 1][u + 1]) return false;
            }
        }
        return true;
    }

    private static List<Corner> cleanUpNeighbors(List<Corner> corners, float dmin) {
        final float dmin2 = dmin * dmin;
        List<Corner> good = new LinkedList<>();

        // sort corners by q in descending order
        corners.sort(null);

        while (corners.size() > 0) {
            Corner c = corners.remove(0);
            good.add(c);
            Iterator<Corner> it = corners.iterator();
            while (it.hasNext()) {
                Corner d = it.next();
                if (c.dist2(d) < dmin2) {
                    it.remove();
                }
            }
        }
        return good;
    }

    private static void convolveXY(float[][] data, float[] filterX, float[] filterY) {
        float[][] tmp = new float[data.length][data[0].length];

        // filter in x-direction
        convolveX(data, filterX, tmp);

        // filter in y-direction
        convolveY(tmp, filterY, data);
    }

    private static void convolveX(float[][] data, float[] filterX, float[][] out) {
        final int h = data.length;
        final int w = data[0].length;
        final int fSizeX = filterX.length;
        final int fSizeXD2 = fSizeX / 2;
        final int w1 = w - fSizeXD2;

        // filter in x-direction
        Parallel.For(0, h, v -> {
            // left border handling
            for (int u = 0; u < fSizeXD2; u++) {
                float sum = 0;
                int u0 = u - fSizeXD2;

                for (int i = 0; i < fSizeX; i++, u0++) {
                    if (u0 < 0) sum += data[v][-u0] * filterX[i];
                    else sum += data[v][u0] * filterX[i];
                }
                out[v][u] = sum;
            }
            // middle part
            for (int u = fSizeXD2; u < w1; u++) {
                float sum = 0;
                int u0 = u - fSizeXD2;

                for (int i = 0; i < fSizeX; i++, u0++) {
                    sum += data[v][u0] * filterX[i];
                }
                out[v][u] = sum;
            }
            // right border handling
            for (int u = w1; u < w; u++) {
                float sum = 0;
                int u0 = u - fSizeXD2;

                for (int i = 0; i < fSizeX; i++, u0++) {
                    if (u0 >= w) sum += data[v][2 * w - u0 - 1] * filterX[i];
                    else sum += data[v][u0] * filterX[i];
                }
                out[v][u] = sum;
            }
        });
    }

    private static void convolveY(float[][] data, float[] filterY, float[][] out) {
        final int h = data.length;
        final int w = data[0].length;
        final int fSizeY = filterY.length;
        final int fSizeYD2 = fSizeY / 2;
        final int h1 = h - fSizeYD2;

        // filter in y-direction
        Parallel.For(0, h, v -> {
            if (v < fSizeYD2) {
                // top border handling
                for (int u = 0; u < w; u++) {
                    float sum = 0;
                    int v0 = v - fSizeYD2;

                    for (int j = 0; j < fSizeY; j++, v0++) {
                        if (v0 < 0) sum += data[-v0][u] * filterY[j];
                        else sum += data[v0][u] * filterY[j];
                    }
                    out[v][u] = sum;
                }
            } else if (v < h1) {
                // middle part
                for (int u = 0; u < w; u++) {
                    float sum = 0;
                    int v0 = v - fSizeYD2;

                    for (int j = 0; j < fSizeY; j++, v0++) {
                        sum += data[v0][u] * filterY[j];
                    }
                    out[v][u] = sum;
                }
            } else {
                // bottom border handling
                for (int u = 0; u < w; u++) {
                    float sum = 0;
                    int v0 = v - fSizeYD2;

                    for (int j = 0; j < fSizeY; j++, v0++) {
                        if (v0 >= h) sum += data[2 * h - v0 - 1][u] * filterY[j];
                        else sum += data[v0][u] * filterY[j];
                    }
                    out[v][u] = sum;
                }
            }
        });
    }

}
