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
        return zoom(Rotation.basicRotation(inData, a, imageType), z, imageType);
    }

    public static ImageData zoom(ImageData in, float scale, int imageType) {
        ImageData out = (ImageData) in.clone();

        double[][] scaling = {
                {(double) scale, 0, 0},
                {0, (double) scale, 0},
                {0, 0, 1}
        };
        Matrix scalingM = new Matrix(scaling);

        Parallel.For(0, out.height, v -> {
            for (int u = 0; u < out.width; u++) {
                double[][] input = {
                        {u},
                        {v},
                        {1}
                };
                Matrix inputM = new Matrix(input);

                Matrix projection = scalingM.multiply(inputM);
                int proj_u = (int) projection.el(0,0);
                int proj_v = (int) projection.el(1,0);


            }
        });

        return out;
    }
}
