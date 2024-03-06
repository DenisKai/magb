package imageprocessing;

import gui.OptionPane;
import main.Picsi;
import org.eclipse.swt.graphics.ImageData;
import utils.Matrix;
import utils.Parallel;

public class RotateAndZoom implements IImageProcessor {
    @Override
    public boolean isEnabled(int imageType) {
        return (imageType == Picsi.IMAGE_TYPE_RGB
                || imageType == Picsi.IMAGE_TYPE_GRAY
                || imageType == Picsi.IMAGE_TYPE_GRAY32
                || imageType == Picsi.IMAGE_TYPE_RGBA
        );
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        float a = OptionPane.showFloatDialog("Rotation (GUZS)?", 45.0f);
        float z = OptionPane.showFloatDialog("Zoom?", 1.0f);
        return transform(Rotation.basicRotation(inData, a, imageType), z);
    }

    public static ImageData transform(ImageData in, float scale) {
        ImageData out = (ImageData) in.clone();

        double[][] translation = {
                {1, 0, -out.width / 2},
                {0, 1, -out.height / 2},
                {0, 0, 1}
        };
        Matrix center_tranM = new Matrix(translation);

        double[][] scaling = {
                {(double) scale, 0, 0},
                {0, (double) scale, 0},
                {0, 0, 1}
        };
        Matrix scalingM = new Matrix(scaling);

        double[][] topLeft = {
                {1, 0, out.width / 2},
                {0, 1, out.height / 2},
                {0, 0, 1}
        };
        Matrix topLeft_tranM = new Matrix(topLeft);

        Matrix transformation = topLeft_tranM.multiply(scalingM).multiply(center_tranM);
        Matrix inverseScaling = transformation.inverse();

        Parallel.For(0, out.height, v -> {
            for (int u = 0; u < out.width; u++) {
                double[][] input = {
                        {u},
                        {v},
                        {1}
                };
                Matrix inputM = new Matrix(input);

                //nearest neighbour
                Matrix source = inverseScaling.multiply(inputM);
                int src_u = (int) source.el(0, 0);
                int src_v = (int) source.el(1, 0);

                out.setPixel(u, v, in.getPixel(src_u, src_v));
            }
        });

        return out;
    }
}
