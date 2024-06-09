package imageprocessing;

import imageprocessing.grayValueConverter.GrayValue;
import main.Picsi;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

public class Dithering implements IImageProcessor {
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

    public static ImageData binarize(ImageData outData) {
        RGB[] rgb = new RGB[2];
        rgb[0] = new RGB(255, 255, 255);    //white
        rgb[1] = new RGB(0, 0, 0);          //black

        ImageData binary = new ImageData(
                outData.width,
                outData.height,
                8,
                new PaletteData(rgb)
        );

        for (int u = 0; u < outData.width; u++) {
            for (int v = 0; v < outData.height; v++) {
                int oldVal = outData.getPixel(u, v);
                int newVal = (oldVal < 128) ? 0 : 255;
                int error = oldVal - newVal;

                // Verteilung error auf Nachbarn
                //       (x)    7/16
                //  3/16  5/16  1/16
                if (u + 1 < outData.width) {
                    int tN = outData.getPixel(u + 1, v);
                    outData.setPixel(u + 1, v, tN + (error * 7) / 16);
                }

                if (u + 1 < outData.width && v + 1 < outData.height) {
                    int tN = outData.getPixel(u + 1, v + 1);
                    outData.setPixel(u + 1, v + 1, tN + error / 16);
                }

                if (v + 1 < outData.height) {
                    int tN = outData.getPixel(u, v + 1);
                    outData.setPixel(u, v + 1, tN + (error * 5) / 16);
                }

                if (u - 1 >= 0) {
                    int tN = outData.getPixel(u - 1, v);
                    outData.setPixel(u - 1, v, tN + (error * 3) / 16);
                }

                outData.setPixel(u, v, newVal);
            }
        }

        return outData;
    }
}
