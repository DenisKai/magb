package imageprocessing;

import imageprocessing.grayValueConverter.GrayValue;
import main.Picsi;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

public class BinaryValue implements IImageProcessor {
    @Override
    public boolean isEnabled(int imageType) {
        return (
                imageType == Picsi.IMAGE_TYPE_RGBA ||
                        imageType == Picsi.IMAGE_TYPE_RGB ||
                        imageType == Picsi.IMAGE_TYPE_GRAY ||
                        imageType == Picsi.IMAGE_TYPE_GRAY32
        );
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        ImageData out = (ImageData) inData.clone();
        if (imageType == Picsi.IMAGE_TYPE_RGB || imageType == Picsi.IMAGE_TYPE_RGBA) {
            out = GrayValue.extractGrayValue(out);
        }

        return binarize(out);
    }

    public static ImageData binarize(ImageData out) {
        RGB[] rgb = new RGB[2];
        rgb[0] = new RGB(255, 255, 255);    //white
        rgb[1] = new RGB(0, 0, 0);          //black

        ImageData binary = new ImageData(
                out.width,
                out.height,
                8,
                new PaletteData(rgb)
        );

        int prevError = 0;
        for (int i = 0; i < out.data.length; i++) {
            int intensity = (out.data[i] & 0xFF) + prevError;

            int error = (intensity < 128) ? 0 : 255;
            int binaryVal = (intensity < 128) ? 1 : 0;
            prevError = intensity - error;

            binary.data[i] = (byte) binaryVal;
        }

        return binary;
    }
}
