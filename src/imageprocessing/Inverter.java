package imageprocessing;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import main.Picsi;
import utils.Parallel;

/**
 * Image inverter
 * @author Christoph Stamm
 *
 */
public class Inverter implements IImageProcessor {

	@Override
	public boolean isEnabled(int imageType) {
		return true;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		ImageData outData = (ImageData)inData.clone();
		invert(outData, imageType);
		return outData;
	}

	/**
	 * Invert image data
	 * @param output image
	 * @param imageType
	 */
	public static void invert(ImageData imageData, int imageType) {
		if (imageType == Picsi.IMAGE_TYPE_INDEXED) {
			// change palette
			RGB[] paletteIn = imageData.getRGBs();
			RGB[] paletteOut = new RGB[paletteIn.length];
			
			for (int i=0; i < paletteIn.length; i++) {
				RGB rgbIn = paletteIn[i];
				paletteOut[i] = new RGB(255 - rgbIn.red, 255 - rgbIn.green, 255 - rgbIn.blue);
			}
			imageData.palette = new PaletteData(paletteOut);
		} else {
			// change pixel colors
			Parallel.For(0, imageData.height, v -> {
				for (int u=0; u < imageData.width; u++) {
					int pixel = imageData.getPixel(u,v);
					imageData.setPixel(u, v, ~pixel);
					/*RGB rgb = imageData.palette.getRGB(imageData.getPixel(u,v));
					rgb.red   = 255 - rgb.red;
					rgb.green = 255 - rgb.green;
					rgb.blue  = 255 - rgb.blue;
					imageData.setPixel(u, v, imageData.palette.getPixel(rgb));*/
				}
			});
		}
	}
}
