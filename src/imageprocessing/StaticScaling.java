package imageprocessing;

import gui.OptionPane;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import utils.Matrix;
import utils.Parallel;

public class StaticScaling implements IImageProcessor {
    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        Object[] methods = {"Nearest Neighbor", "Bilinear", "Bikubisch"};
        int m = OptionPane.showOptionDialog("Options", SWT.ICON_INFORMATION, methods, 0);
        float z = OptionPane.showFloatDialog("Zoom?", 1.0f);

        return scale(inData, z, m);
    }

    public static ImageData scale(ImageData in, float scale,int option) {
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

                int intensity = 0;
                if (option == 1) {
                    intensity = bilinear(in, source);
                } else {
                    intensity = nearestNeighbour(in, source);
                }

                out.setPixel(u, v, intensity);
            }
        });

        return out;
    }


    private static int nearestNeighbour(ImageData in, Matrix source) {
        int src_u = (int) source.el(0, 0);
        int src_v = (int) source.el(1, 0);

        return in.getPixel(src_u, src_v);
    }

    private static int bilinear(ImageData in, Matrix source) {
        int A, B, C, D = 0;
        A = in.getPixel((int) Math.floor(source.el(0, 0)), (int) Math.floor(source.el(1, 0)));
        B = in.getPixel((int) Math.ceil(source.el(0, 0)), (int) Math.floor(source.el(1, 0)));
        C = in.getPixel((int) Math.floor(source.el(0, 0)), (int) Math.ceil(source.el(1, 0)));
        D = in.getPixel((int) Math.ceil(source.el(0, 0)), (int) Math.ceil(source.el(1, 0)));

        int a, b = 0;
        a = (int) (source.el(0, 0) - Math.floor(source.el(0, 0)));
        b = (int) (source.el(1, 0) - Math.floor(source.el(1, 0)));
        int E = A + a * (B-A);
        int F = C + a * (D-C);

        int G = E + b * (F-E);

        return G;
    }
}
