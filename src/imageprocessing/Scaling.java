package imageprocessing;

import gui.OptionPane;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import utils.Matrix;
import utils.Parallel;

public class Scaling implements IImageProcessor {
    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        Object[] methods = {"Nearest Neighbor", "Bilinear", "Bikubisch"};
        int m = OptionPane.showOptionDialog("Options", SWT.ICON_INFORMATION, methods, 0);
        float z = OptionPane.showFloatDialog("Skalierung?", 1.0f);

        return scale(inData, z, m);
    }

    public static ImageData scale(ImageData in, float scale, int option) {
        ImageData out = ImageProcessing.createImage((int) Math.floor(in.width * scale), (int) Math.floor(in.height * scale), ImageProcessing.determineImageType(in));

        double[][] scaling = {
                {(double) scale, 0, 0},
                {0, (double) scale, 0},
                {0, 0, 1}
        };
        Matrix scalingM = new Matrix(scaling);
        Matrix inverseScaling = scalingM.inverse();

        Parallel.For(0, out.height, v -> {
            for (int u = 0; u < out.width; u++) {
                double[][] input = {
                        {u},
                        {v},
                        {1}
                };
                Matrix inputM = new Matrix(input);
                Matrix source = inverseScaling.multiply(inputM);

                int intensity = 255;
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
        int src_u = (int) ((Math.round(source.el(0, 0)) < in.width) ? Math.round(source.el(0, 0)) : Math.floor(source.el(0, 0)));
        int src_v = (int) ((Math.round(source.el(1, 0)) < in.width) ? Math.round(source.el(1, 0)) : Math.floor(source.el(1, 0)));

        return in.getPixel(src_u, src_v);
    }

    private static int bilinear(ImageData in, Matrix source) {
        int A, B, C, D = 0;
        int u0 = (int) Math.floor(source.el(0, 0));
        int v0 = (int) Math.floor(source.el(1, 0));

        // B and D get IllegalArgumentException
        // because getting corner pixels in original picture can lead rounding to OutOfBounds
        int u0_1 = (u0 + 1 < in.width) ? u0 + 1 : u0;
        int v0_1 = (v0 + 1 < in.width) ? v0 + 1 : v0;

        A = in.getPixel(u0, v0);
        B = in.getPixel(u0_1, v0);
        C = in.getPixel(u0, v0_1);
        D = in.getPixel(u0_1, v0_1);

        int a, b = 0;
        a = (int) (source.el(0, 0) - Math.floor(source.el(0, 0)));
        b = (int) (source.el(1, 0) - Math.floor(source.el(1, 0)));
        int E = A + a * (B-A);
        int F = C + a * (D-C);

        int G = E + b * (F-E);

        return G;
    }
}
