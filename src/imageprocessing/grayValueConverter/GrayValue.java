package imageprocessing.grayValueConverter;

import imageprocessing.IImageProcessor;
import main.Picsi;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import utils.Parallel;

import java.util.ArrayList;
import java.util.List;

public class GrayValue implements IImageProcessor {
    @Override
    public boolean isEnabled(int imageType) {
        return (imageType == Picsi.IMAGE_TYPE_RGB || imageType == Picsi.IMAGE_TYPE_RGBA);
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        ImageData outData = (ImageData) inData.clone();
        extractGrayValue(outData);
        return outData;
    }

    private void extractGrayValue(ImageData outData) {
         Parallel.For(0, outData.height, v -> {
            for (int u = 0; u < outData.width; u++) {
                int temp = outData.getPixel(u, v);
                int red = (temp & 0xFF0000) >> 16;
                int green = (temp & 0x00FF00) >> 8;
                int blue = temp & 0x0000FF;

                int intensity = (int)(red*0.2 + green*0.6 + blue*0.2);
                int out = intensity;
                out = (out << 8) | intensity;
                out = (out << 8) | intensity;

                outData.setPixel(u, v, out);
            }
        });
    }
}
