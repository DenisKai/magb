package imageprocessing;

import main.Picsi;
import org.eclipse.swt.graphics.ImageData;
import utils.Parallel;

/**
 * Image segmentation (binarization) using Otsu's method
 * Image foreground = black
 * Palette: background, foreground
 * @author Christoph Stamm
 *
 */
public class Binarization implements IImageProcessor {
	public static int s_background = 0; // white
	public static int s_foreground = 1; // black

	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_GRAY;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		final int threshold = otsuThreshold(inData);
		System.out.println(threshold);

		return binarize(inData, threshold, false, true);
	}

	/**
	 * Binarization of grayscale image
	 * @param inData grayscale image
	 * @param threshold
	 * @param smallValuesAreForeground true: Image foreground <= threshold, false: Image foreground > threshold
	 * @param binary true: output is binary image, false: output is grayscale image
	 * @return binarized image
	 */
	public static ImageData binarize(ImageData inData, int threshold, boolean smallValuesAreForeground, boolean binary) {
		ImageData outData = ImageProcessing.createImage(inData.width, inData.height, (binary) ? Picsi.IMAGE_TYPE_BINARY : Picsi.IMAGE_TYPE_GRAY);
		final int fg = (smallValuesAreForeground) ? s_foreground : s_background;
		final int bg = (smallValuesAreForeground) ? s_background : s_foreground;

		Parallel.For(0, inData.height, v -> {
			for (int u=0; u < inData.width; u++) {
				outData.setPixel(u, v, (inData.getPixel(u,v) <= threshold) ? fg : bg);
			}
		});
		return outData;
	}

	/**
	 * Computes a global threshold for binarization using Otsu's method
	 * @param inData grayscale image
	 * @return threshold
	 */
	public static int otsuThreshold(ImageData inData) {
		int[] histogram = ImageProcessing.histogram(inData, (int) Math.pow(2, inData.depth));

		int n = inData.width * inData.height; // Anzahl Pixel

		int threshold = 0; // Schwellwert
		double max_varianz = 0;

		for (int t = 0; t < histogram.length; t++) {
			int i;
			float prob_p0 = 0;
			float prob_p1 = 0;
			float mu_0 = 0;
			float mu_1 = 0;

			for (i = 0; i <= t; i++) {
				float pi = (float) histogram[i] / n;
				prob_p0 += pi;
				mu_0 += i * pi;
			}

			for (int j = i + 1; j < histogram.length; j++) {
				float pi = (float) histogram[j] / n;
				prob_p1 += pi;
				mu_1 += i * pi;
			}

			mu_0 = mu_0 / prob_p0;
			mu_1 = mu_1 / prob_p1;
			float mu = prob_p0 * mu_0 + prob_p1 * mu_1;

			float var_0 = 0;
			float var_1 = 0;

			for (i = 0; i <= t; i++) {
				float pi = (float) histogram[i] / n;
				var_0 += pi * (i - mu_0) * (i - mu_0);
			}

			for (int j = i + 1; j < histogram.length; j++) {
				float pi = (float) histogram[j] / n;
				var_1 += pi * (i - mu_1) * (i - mu_1);
			}

			// inTRAklassenvarianz
			float o_intra = var_0 * prob_p0 + var_1 * prob_p1;

			// inTERklassenvarianz
			float o_inter = (mu_0 - mu) * (mu_0 - mu) * prob_p0 + (mu_1 - mu) * (mu_1 - mu) * prob_p1;

			if (o_inter > max_varianz) {
				max_varianz = o_inter;
				threshold = t;
			}
		}

		System.out.println(threshold);
		return threshold;
	}
}
