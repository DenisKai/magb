package imageprocessing;

import gui.OptionPane;
import gui.RectTracker;
import main.Picsi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import utils.BoundedPQ;
import utils.Parallel;

import java.util.ArrayList;

/**
 * Pattern matching based on correlation coefficient
 *
 * @author Christoph Stamm
 */
public class PatternMatching implements IImageProcessor {
    public static class PMResult implements Comparable<PMResult> {
        public ROI m_roi;
        public double m_cl;

        public PMResult(ROI roi, double cl) {
            m_roi = roi;
            m_cl = cl;
        }

        public int compareTo(PMResult pm) {
            if (m_cl < pm.m_cl) return -1;
            else if (m_cl > pm.m_cl) return 1;
            else return 0;
        }
    }

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        // let the user choose the operation
        Object[] operations = {"Pattern Matching", "PM with modified Pattern", "User defined Pattern"};
        int f = OptionPane.showOptionDialog("Pattern Matching Operation", SWT.ICON_INFORMATION, operations, 0);
        if (f < 0) return null;

        final int intensityOffset = -50;
        final int contrastFactor = 4;
        final boolean predefinedPattern = f < 2;

        // pattern region
        Rectangle pr = null;

        if (predefinedPattern) {
            pr = new Rectangle(200, 310, 70, 50);
        } else {
            RectTracker rt = new RectTracker();
            pr = rt.start(70, 50);
        }

        final int nResults = 10 * 10;    // search nResults best matches
        final ROI pattern = new ROI((f > 0) ? (ImageData) inData.clone() : inData, pr);
        final int pw = pattern.getWidth();
        final int ph = pattern.getHeight();

        // pre-processing
        if (f == 1) {
            for (int v = 0; v < pattern.getHeight(); v++) {
                for (int u = 0; u < pattern.getWidth(); u++) {
                    int pixel = pattern.getPixel(u, v);
                    pixel = ImageProcessing.clamp8(pixel * contrastFactor);
                    pixel = ImageProcessing.clamp8(pixel + intensityOffset);
                    pattern.setPixel(u, v, pixel);
                }
            }
        }

        // pattern matching
        BoundedPQ<PMResult> results = pm(inData, pattern, nResults);

        // create output
        ImageData outData = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_RGB);

        // copy inData to outData
        Parallel.For(0, inData.height, v -> {
            for (int u = 0; u < inData.width; u++) {
                RGB rgb = inData.palette.getRGB(inData.getPixel(u, v));
                outData.setPixel(u, v, outData.palette.getPixel(rgb));
            }
        });

        return createOutput(outData, results, nResults);
    }

    /**
     * Pattern matching based on correlation coefficient
     *
     * @param inData
     * @param pattern
     * @param nResults number of best results
     * @return results
     */
    public static BoundedPQ<PMResult> pm(ImageData inData, ROI pattern, int nResults) {
        final int pw = pattern.getWidth();
        final int ph = pattern.getHeight();
        final int K = ph * pw;
        final float K_sqrt = (float) Math.sqrt(K);
        BoundedPQ<PMResult> results = new BoundedPQ<>(nResults);

        float R_mean = 0;
        for (int v = 0; v < ph; v++) {
            for (int u = 0; u < pw; u++) {
                R_mean += pattern.getPixel(u, v);
            }
        }
        R_mean /= K;

        float sigma_r = 0;
        for (int v = 0; v < ph; v++) {
            for (int u = 0; u < pw; u++) {
                int R = pattern.getPixel(u, v);
                sigma_r += (R - R_mean) * (R - R_mean);
            }
        }
        sigma_r = (float) Math.sqrt(sigma_r / K);

        // Calculate C_L(r,s)
        for (int s = 0; s < inData.height - ph; s++) {
            for (int r = 0; r < inData.width - pw; r++) {
                //  Only calculate pattern-match in areas inside of image
                if (r + pw < inData.x + inData.width && s + ph < inData.y + inData.height) {
                    float I_mean = 0;
                    int I_sum_sqrd = 0;
                    int product_I_R = 0;
                    for (int j = 0; j < ph; j++) {
                        for (int i = 0; i < pw; i++) {
                            int I = inData.getPixel(r + i, s + j);
                            I_mean += I;
                            I_sum_sqrd += I * I;
                            int R = pattern.getPixel(i, j);
                            product_I_R += I * R;
                        }
                    }
                    I_mean /= K;

                    // Zähler: Sum(I*R) - K * I_mean * R_mean
                    float numerator = product_I_R - K * I_mean * R_mean;

                    // Nenner: sqrt( SUM(I(r+i,s+j))^2 - K * (I_mean)^2) * o_R * sqrt(K)
                    float denominator = (float) Math.sqrt(I_sum_sqrd - K * (I_mean * I_mean)) * sigma_r * K_sqrt;

                    float C_L = numerator / denominator;
                    PMResult result = new PMResult(new ROI(inData, new Rectangle(r, s, pw, ph)), C_L);
                    results.add(result);
                }
            }
        }

        return results;
    }

    /**
     * Show best matching results as rectangles in the input image
     *
     * @param outData  output image
     * @param pq
     * @param nResults
     * @return
     */
    private ImageData createOutput(ImageData outData, BoundedPQ<PMResult> pq, int nResults) {
        ArrayList<PMResult> results = new ArrayList<>();

        // create image and write text into image
        Display display = Picsi.s_shell.getDisplay();
        Image output = new Image(display, outData);
        GC gc = new GC(output);

        // set font
        gc.setForeground(new Color(display, 255, 0, 0)); // red
        gc.setBackground(new Color(display, 255, 255, 255)); // white
        gc.setFont(new Font(display, "Segoe UI", 8, 0));

        for (int i = 0; i < nResults; i++) {
            final PMResult pm = pq.removeMax();

            if (pm != null) {
                int j = 0;
                while (j < results.size() && !pm.m_roi.overlaps(results.get(j).m_roi)) j++;
                if (j == results.size()) {
                    final Rectangle r = pm.m_roi.m_rect;

                    results.add(pm);

                    gc.drawRectangle(r);
                    gc.drawText(String.format("%.2f", pm.m_cl), r.x, r.y + r.height, true);
                }
            }
        }

        gc.dispose();

        outData = output.getImageData();
        output.dispose();
        return outData;
    }

}
