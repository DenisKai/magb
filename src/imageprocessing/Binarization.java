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


		float total_intensity = 0;
		for (int t = 0; t < histogram.length; t++) {
			total_intensity += t * histogram[t];
		}

		float intensity_b = 0; // Kumultative Hintergrund-Intensität
		float weight_p0 = 0; // Kumultative Wahrscheinlichkeit p_i des Hintergrundes
		float weight_p1 = 0; // Kumultative Wahrscheinlichkeit p_i des Vordergrunds (1 - p0)

		for (int t = 0; t < histogram.length; t++) {
			weight_p0 += histogram[t];
			if (weight_p0 == 0) continue;

			weight_p1 = n - weight_p0;
			if (weight_p1 == 0) break;

			intensity_b += (float) (t * histogram[t]);

			float mu_0 = intensity_b / weight_p0;
			float mu_1 = (total_intensity - intensity_b) / weight_p1;

			float o_inter = weight_p0 * weight_p1 * (mu_0 - mu_1) * (mu_0 - mu_1);

			// Interklassenvarianz maximiert == Intraklassenvarianz minimiert
			if (o_inter > max_varianz) {
				max_varianz = o_inter;
				threshold = t;
			}
		}

		System.out.printf("Otsu threshold is: %s%n", threshold);
		return threshold;
	}
}
