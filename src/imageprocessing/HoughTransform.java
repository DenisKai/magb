package imageprocessing;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;

import main.Picsi;
import utils.Parallel;

/**
 * Hough Transform
 * @author Christoph Stamm
 *
 */
public class HoughTransform implements IImageProcessor {
	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_BINARY;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		return showParamSpace(rhoThetaTransform(inData));
	}

	private static int[][] rhoThetaTransform(ImageData inData) {
		final int tMax = 180;
		final int rMax = 180;
		final int rMaxD2 = rMax/2;
		final int bg = 0;
		final int wD2 = inData.width/2;
		final int hD2 = inData.height/2;
		final double dTheta = Math.PI/tMax;
		final double dRho = Math.ceil(Math.hypot(wD2, hD2))/rMaxD2;
		int[][] paramSpace = new int[rMax][tMax];

		Parallel.For(0, inData.height, v -> {
			int v_r = v - wD2;

			for (int u = 0; u < inData.width; u++) {
				if (inData.getPixel(u, v) != 0)
					continue;

				int u_r = u - wD2;

				// from 0 to PI, each degree should be calculated (theta)
				// Formula: x*cos(θ) + y*sin(θ) = r
				for (int t = 0; t < tMax; t++) {
					double theta = t * dTheta;
					double rho = ((u_r * Math.cos(theta) + v_r * Math.sin(theta)) / dRho) + rMaxD2;
					if (0 <= rho && rho < rMax) {
						paramSpace[(int) rho][t]++;
					}
				}
			}
		});

		return paramSpace;
	}
	
	private static ImageData showParamSpace(int[][] paramSpace) {
		final int size = 3*180;
		final int h = paramSpace.length;
		final int w = paramSpace[0].length;
		
		ImageData outData = ImageProcessing.createImage(size, size, Picsi.IMAGE_TYPE_GRAY);
		
		// compute maximum value in paramSpace
		int[] max = new int[] {0};
		Parallel.For(0, h, 
			// creator
			() -> new int[] {0},
			// loop body
			(v, m) -> {
				for(int u = 0; u < w; u++) {
					if (paramSpace[v][u] > m[0]) m[0] = paramSpace[v][u];
				}
			},
			// reducer
			m -> {
				if (m[0] > max[0]) max[0] = m[0];
			}
		);
		
		final int maxVal = max[0];
		
		Parallel.For(0, outData.height, v -> {
			for(int u = 0; u < outData.width; u++) {
				outData.setPixel(u, v, 255 - paramSpace[v*h/size][u*w/size]*255/maxVal);
			}
		});
		return outData;
	}
	
}
