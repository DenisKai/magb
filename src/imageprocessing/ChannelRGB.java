package imageprocessing;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

import main.Picsi;
import utils.Parallel;

/**
 * RGB channel visualizer
 * @author Christoph Stamm
 *
 */
public class ChannelRGB implements IImageProcessor {
	int m_channel;
	
	public ChannelRGB(int channel) {
		assert 0 <= channel && channel < 3 : "wrong channel: " + channel;
		m_channel = channel;
	}

	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_RGB || imageType == Picsi.IMAGE_TYPE_INDEXED;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		return getChannel(inData, m_channel);
	}

	public static ImageData getChannel(ImageData inData, int channel) {
		ImageData outData = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_GRAY);

		// parallel image loop
		Parallel.For(0, inData.height, v -> {
			for (int u=0; u < inData.width; u++) {
				RGB rgb = inData.palette.getRGB(inData.getPixel(u,v));
				switch(channel) {
				case 0: outData.setPixel(u, v, rgb.red); break;
				case 1: outData.setPixel(u, v, rgb.green); break;
				case 2: outData.setPixel(u, v, rgb.blue); break;
				}
			}
		});
		return outData;		
	}
}
