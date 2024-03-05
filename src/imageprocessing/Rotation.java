package imageprocessing;

import gui.OptionPane;
import org.eclipse.swt.graphics.ImageData;
import utils.Matrix;
import utils.Parallel;

import java.util.ArrayList;

public class Rotation implements IImageProcessor {
    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        float a = 45.0f;
        float b = OptionPane.showFloatDialog("Rotation (GUZS)?", a);
        return basicRotation(inData, b, imageType);
    }

    // Source to Target
    public static ImageData basicRotation(ImageData in, float a, int imageType) {
        ImageData copy = ImageProcessing.createImage(in.width, in.height, imageType);
        //convert to radians, counter clock wise
        a = (a % 360) * ((float) (Math.PI / 180));

        double[][] middle = {
                {Math.ceil(copy.width / 2)},
                {Math.ceil(copy.height / 2)}
        };

        double[][] translation = {
                {1, 0, -middle[0][0]},
                {0, 1, -middle[1][0]},
                {0, 0, 1}
        };
        Matrix center_tranM = new Matrix(translation);

        double[][] formulaRotation = {
                {Math.cos(a), Math.sin(a), 0},
                {-Math.sin(a), Math.cos(a), 0},
                {0, 0, 1}
        };
        Matrix roM = new Matrix(formulaRotation);

        double[][] topLeftM = {
                {1, 0, middle[0][0]},
                {0, 1, middle[1][0]},
                {0, 0, 1}
        };
        Matrix topLeft_tranM = new Matrix(topLeftM);
        Matrix transformation = topLeft_tranM.multiply(roM).multiply(center_tranM);
        Matrix invTrans = transformation.inverse();

        int black = 0;
        //assign values in target, important change to (mathematical) formula
        // u = u' and v = v' here
        Parallel.For(0, copy.height, v -> {
            for (int u = 0; u < copy.width; u++) {
                double[][] inputV = {
                        {u},
                        {v},
                        {1}
                };
                Matrix inputM = new Matrix(inputV);
                Matrix source = invTrans.multiply(inputM);

                int source_u = (int) source.el(0, 0);
                int source_v = (int) source.el(1, 0);

                if ((source_u < 0 || in.width <= source_u) || (source_v < 0 || in.height <= source_v)) {
                    copy.setPixel(u, v, black);
                } else {
                    copy.setPixel(u, v, in.getPixel(source_u, source_v));
                }
            }
        });

        return copy;
    }

    public static ImageData rotateAndFit(ImageData in, float a, int imageType) {
        return null;
    }
}
