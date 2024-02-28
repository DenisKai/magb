package imageprocessing.grayValueConverter;

import imageprocessing.IImageProcessor;
import main.Picsi;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

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
        return null;
    }

    private void extractGrayValue(ImageData outData) {
        List<Integer> red = new ArrayList<>();
        List<Integer> green = new ArrayList<>();
        List<Integer> blue = new ArrayList<>();

        //RED: outData.data[i] & 0x1111_1111;
        //Green: (outData.data[i] >>> 8) & 0x1111_1111;
        //Blue: (outData.data[i] >>> 16 & 0x1111_1111;


    }
}
