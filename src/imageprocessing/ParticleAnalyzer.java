package imageprocessing;

import gui.OptionPane;
import imageprocessing.colors.Inverter;
import main.Picsi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import utils.Parallel;

import java.awt.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * Particle analyzer that analyzes gray-value pictures for particles.
 * Labels each individual particle and calculates different moments.
 *
 * Calculated values/moments: Centroid, Boundingbox, Area, Eccentricity,
 *                            Contour (normal and corrected), Orientation, Compactness/Roundness.
 *
 * Additionally draws the contour in a darker gray around the particles, draws the centroid pixel (rounded)
 *     and the bounding box.
 *
 *
 * @author Denis Kai Wilhelm
 */
public class ParticleAnalyzer implements IImageProcessor {
    boolean smallValuesAreForeground = false;   // Please assign true or false accordingly
    final int bb_color = Color.BLUE.getRGB();    //Bounding box color
    final int contour_color = 120;  // gray value contour colour

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        Object[] op = {"True", "False"};
        int option = OptionPane.showOptionDialog("Small values (darker pixels) are foreground?", SWT.ICON_INFORMATION, op, 1);
        if (option == 0) {
            smallValuesAreForeground = true;
        } else {
            smallValuesAreForeground = false;
        }

        int threshold = Binarization.otsuThreshold(inData);
        ImageData binarized = Binarization.binarize(inData, threshold, smallValuesAreForeground, false);
        ImageData labeled_image = MorphologicFilter.closing(binarized, MorphologicFilter.s_diamond5, 2, 2, 1);
        FloodFilling.fillHoles(labeled_image); // fill leftover holes from closing

        int n_labels = FloodFilling.floodFillLabeling(labeled_image);

        // Flächen (Nulltes Moment M00)
        int[] areas = new int[n_labels];
        Point[] centers = new Point[n_labels];
        double[] eccentricities = new double[n_labels];
        double[] contours = new double[n_labels];
        double[] compactness = new double[n_labels];
        double[] compactness_corr = new double[n_labels];
        double[] orientations = new double[n_labels];
        for (int i = 0; i < n_labels; i++) {
            int[] geometric_moments = calculateMoments(labeled_image, i + 2);
            areas[i] = geometric_moments[0];

            // Schwerpunkte (Erstes Moment M10 und M01)
            double u_center = geometric_moments[1] / geometric_moments[0];
            double v_center = geometric_moments[2] / geometric_moments[0];
            centers[i] = new Point((int) u_center, (int) v_center); // rounded center point

            // Exzentrizität
            double[] centralMoments = calculateCentralMoments(labeled_image, i + 2, u_center, v_center);
            eccentricities[i] = calculateEccentricity(centralMoments);

            // Kontur
            contours[i] = calculateContour(labeled_image, i + 2);

            // Kompaktheit / Rundheit mit korrigiertem Umfang
            compactness[i] = (4 * Math.PI * areas[i]) / (contours[i] * contours[i]);
            compactness_corr[i] = (4 * Math.PI * areas[i]) / ((contours[i] * 0.95) * (contours[i] * 0.95));

            // Orientation
            orientations[i] = calculateOrientation(centralMoments[0], centralMoments[1], centralMoments[2]);
        }

        // Bounding box
        ImageData out = convertGrayToRGB(labeled_image);
        Point[][] bounds = new Point[n_labels][2];
        for (int i = 0; i < n_labels; i++) {
            bounds[i] = drawBoundingBox(out, i + 2);
            Point centerPoint = centers[i];
            out.setPixel(centerPoint.x, centerPoint.y, Color.RED.getRGB());
        }

        String output_header =
                """
                        | Label | Schwerpunkt (u, v) | Bounding Box                  | Flaeche (px) | Exzentrizitaet | Kontur (px)           | Orientierung | Rundheit | Rundheit     |
                        |       |                    | (u_min, v_min):(u_max, v_max) |              |                | (normal):(korrigiert) | (in Grad)    |          | (korrigiert) |
                        |-------|--------------------|-------------------------------|--------------|----------------|-----------------------|--------------|----------|--------------|""";
        System.out.println(output_header);

        for (int i = 0; i < n_labels; i++) {
            int l_center = String.format(" (%d,%d)", centers[i].x, centers[i].y).length();
            int l_bb = String.format("(%d,%d):(%d,%d)", bounds[i][0].x, bounds[i][0].y, bounds[i][1].x, bounds[i][1].y).length();
            int l_con = String.format(" (%.2f):(%.2f) ", contours[i], (float) contours[i] * 0.95).length();
            int l_or = String.format("%.4f", orientations[i]).length();

            System.out.printf("| %-5d | (%d,%d)%-" + (19 - l_center) + "s | (%d,%d):(%d,%d)%-" + (29 - l_bb) + "s | %-12d | %.5f%-7s | (%.2f):(%.2f)%-" + (23 - l_con) + "s | %.4f%-" + (12 - l_or) + "s | %.4f%-2s | %.4f%-6s |\n",
                    i + 2,
                    centers[i].x, centers[i].y, "",
                    bounds[i][0].x, bounds[i][0].y, bounds[i][1].x, bounds[i][1].y, "",
                    areas[i],
                    eccentricities[i], "",
                    contours[i], (float) contours[i] * 0.95, "",
                    orientations[i], "",
                    compactness[i], "",
                    compactness_corr[i], "");
        }

        return out;
    }

    /**
     * Calculate the moments of a particle
     * <p>
     * Geometrische Momente M_pq: ∑v ∑u u^p * v^q * I(u,v)
     * <p>
     * Intensität wird vernachlässigt da diese als 1 genommen wird (pseudo-binärbild).
     * (Eigentlich ist diese gleich dem Label)
     *
     * @param inData      labeled grayscale image
     * @param label_value value of which label the area should be calculated
     * @return array of moments m00, m10, m01
     */
    private int[] calculateMoments(ImageData inData, int label_value) {
        int m00 = 0;
        int m10 = 0;
        int m01 = 0;
        int m11 = 0;
        int m20 = 0;
        int m02 = 0;

        for (int v = 0; v < inData.height; v++) {
            for (int u = 0; u < inData.width; u++) {
                if (inData.getPixel(u, v) == label_value) {
                    m00++;
                    m10 += u;
                    m01 += v;
                    m11 += u * v;
                    m20 += u * u;
                    m02 += v * v;
                }
            }
        }

        return new int[]{
                m00, m10, m01, m11, m20, m02
        };
    }

    /**
     * Zentrale Momente μ_pq: ∑v ∑u (u-u_center)^p*(v-v_center)^q * I(u,v)
     * Auch hier wird die Intensität vernachlässigt.
     *
     * @param u_center x-coordinate of center from particle
     * @param v_center y-coordinate of center from particle
     * @return Array containing μ_pq
     */
    private double[] calculateCentralMoments(ImageData inData, int label_value, double u_center, double v_center) {
        double mu11 = 0;
        double mu20 = 0;
        double mu02 = 0;

        for (int v = 0; v < inData.height; v++) {
            for (int u = 0; u < inData.width; u++) {
                if (inData.getPixel(u, v) == label_value) {
                    mu11 += (u - u_center) * (v - v_center);
                    mu20 += (u - u_center) * (u - u_center);
                    mu02 += (v - v_center) * (v - v_center);
                }
            }
        }

        return new double[]{mu11, mu20, mu02};
    }

    /**
     * Exzentrizität e = [mu20 - mu02]^2 + 4[mu11]^2 / (mu20 + mu02)
     * Zeigt an wie 'Ellipsisch' das Partikel ist. 0 = Rund, 1 = langgezogen.
     *
     * @param central_moments Array containing the central moments: mu11, mu20 and mu02
     * @return
     */
    private double calculateEccentricity(double[] central_moments) {
        double mu11 = central_moments[0];
        double mu20 = central_moments[1];
        double mu02 = central_moments[2];

        double e_upper_part = ((mu20 - mu02) * (mu20 - mu02)) + (4 * (mu11 * mu11));
        double e_lower_part = (mu20 + mu02) * (mu20 + mu02);

        return e_upper_part / e_lower_part;
    }

    /**
     * Determine bounding box by lowest point (upper left) and highest point (lower right)
     *
     * @param inData   labeled image
     * @param label_no number of label which bounding box should be determined.
     * @return size 2 array, containing lowest (x,y) and highest (x,y) value/point
     */
    private Point[] drawBoundingBox(ImageData inData, int label_no) {
        RGB rgb_value_label = new RGB(label_no, label_no, label_no);
        int min_x = inData.width;
        int min_y = inData.height;
        int max_x = 0;
        int max_y = 0;

        // Find the bounding box of the labeled region
        for (int u = 0; u < inData.width; u++) {
            for (int v = 0; v < inData.height; v++) {
                if (inData.getPixel(u, v) != inData.palette.getPixel(rgb_value_label)) {
                    continue;
                }

                if (u < min_x) min_x = u;
                if (u > max_x) max_x = u;
                if (v < min_y) min_y = v;
                if (v > max_y) max_y = v;
            }
        }

        // Da das Kontur einfärben die Pixel verändert, muss bei den Koordinaten:
        // minimum je -1 und
        // maximum je +1 gerechnet werden
        min_x--;
        min_y--;
        max_x++;
        max_y++;

        // Draw the bounding box
        for (int u = min_x; u <= max_x; u++) {
            inData.setPixel(u, min_y, bb_color); // Top
            inData.setPixel(u, max_y, bb_color); // Bottom
        }
        for (int v = min_y; v <= max_y; v++) {
            inData.setPixel(min_x, v, bb_color); // Left
            inData.setPixel(max_x, v, bb_color); // Right
        }

        return new Point[]{
                new Point(min_x, min_y),
                new Point(max_x, max_y)
        };
    }

    /**
     * Calculate contour of particle with given label.
     *
     * @param inData   labeled image (grayvalue)
     * @param label_no label of particle
     * @return length of contour. -1 if no particle with given label has been found.
     */
    private double calculateContour(ImageData inData, int label_no) {
        int start_u = -1, start_v = -1;

        for (int v = 0; v < inData.height; v++) {
            if (start_u != -1 && start_v != 1) break;

            for (int u = 0; u < inData.width; u++) {
                if (inData.getPixel(u, v) == label_no) {
                    start_u = u;
                    start_v = v;
                    break;
                }
            }
        }

        if (start_u == -1 || start_v == -1) {
            return -1;
        }

        double contour_length = 0;

        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(start_u, start_v));
        inData.setPixel(start_u, start_v, contour_color);

        // directions
        // 1 2 3
        // 0 X 4
        // 7 6 5
        final int[][] d_0 = {{0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}};
        final int[][] d_1 = {{-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}};
        final int[][] d_2 = {{-1, 0}, {-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}};
        final int[][] d_3 = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
        final int[][] d_4 = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
        final int[][] d_5 = {{1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}};
        final int[][] d_6 = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
        final int[][] d_7 = {{1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}, {1, 0}};
        int direction_prev = 4; // initial for-loops ran row to row, left to right

        while (!queue.isEmpty()) {
            Point curr = queue.poll();

            int[][] rel_direction = {};

            switch (direction_prev) {
                case 0:
                    rel_direction = d_0;
                    break;
                case 1:
                    rel_direction = d_1;
                    break;
                case 2:
                    rel_direction = d_2;
                    break;
                case 3:
                    rel_direction = d_3;
                    break;
                case 4:
                    rel_direction = d_4;
                    break;
                case 5:
                    rel_direction = d_5;
                    break;
                case 6:
                    rel_direction = d_6;
                    break;
                case 7:
                    rel_direction = d_7;
                    break;
                default:
                    System.out.println("no direction matrix...");
                    break;
            }

            int initial_direction = direction_prev - 2;
            if (initial_direction < 0) {
                initial_direction = (initial_direction + 8) % 8;
            }
            int current_direction = initial_direction;


            // if previous direction is even -> 0,2,4,6 are vertical/horizontal
            // if previous direction is odd -> 1,3,5,7 are vertical/horizontal
            boolean even = (direction_prev % 2 == 0);

            for (int i = 0; i < rel_direction.length; i++) {
                int rel_u = curr.x + rel_direction[i][0];
                int rel_v = curr.y + rel_direction[i][1];
                if (start_u == rel_u && start_v == rel_v) {
                    break; // Umrundung abgeschlossen
                }

                if (inData.getPixel(rel_u, rel_v) == label_no) {
                    queue.add(new Point(rel_u, rel_v));
                    inData.setPixel(rel_u, rel_v, contour_color);

                    if (even) {
                        // add 1 to contour if 0,2,4,6
                        if (current_direction % 2 == 0) {
                            contour_length++;
                        } else {
                            contour_length += Math.sqrt(2);
                        }
                    } else {
                        // add 1 to contour if 1,3,5,7
                        if (current_direction % 2 == 1) {
                            contour_length++;
                        } else {
                            contour_length += Math.sqrt(2);
                        }
                    }

                    direction_prev = current_direction;
                    break;
                } else {
                    current_direction = ((current_direction + 1) % 8);
                }
            }
        }

        return contour_length;
    }

    /**
     * Berechnet die Orientierung (Winkel zu Hauptachse, G.u.s)
     * Theta 0 = 0.5 * atan((2*mu11) / (mu20 - mu02))
     *
     * @param mu11 zentrales Moment mu11
     * @param mu20 zentrales Moment mu20
     * @param mu02 zentrales Moment mu02
     * @return Winkel zwischen Hauptachse (x bzw. u) und Vektor der grössten Ausdehnung des Parikels.
     */
    private double calculateOrientation(double mu11, double mu20, double mu02) {
        double input_atan = (2 * mu11) / (mu20 - mu02);
        return Math.toDegrees(0.5 * Math.atan(input_atan));
    }

    /**
     * Convert a grayvalue image to a rgb image
     *
     * @param inData (labeled) gray value image
     * @return rgb image that has all channels set to the gray image values.
     */
    private ImageData convertGrayToRGB(ImageData inData) {
        PaletteData palette = new PaletteData(0xFF0000, 0x00FF00, 0x0000FF);
        ImageData out = new ImageData(
                inData.width,
                inData.height,
                24,
                palette
        );

        for (int v = 0; v < inData.height; v++) {
            for (int u = 0; u < inData.width; u++) {
                int grayPixel = inData.getPixel(u, v);

                // Convert the gray-value to a color (R=G=B=grayPixel)
                int colorPixel = palette.getPixel(new RGB(grayPixel, grayPixel, grayPixel));
                out.setPixel(u, v, colorPixel);
            }
        }

        return out;
    }

    // ---------------- Otsu + Binarize, Floodfill, Morphilogical-filter classes from other tasks --------

    /**
     * Image segmentation (binarization) using Otsu's method
     * Image foreground = black
     * Palette: background, foreground
     *
     * @author Christoph Stamm
     * @author Denis Wilhelm (Otsu threshold)
     */
    private static class Binarization {
        public static int s_background = 0; // white
        public static int s_foreground = 1; // black

        /**
         * Binarization of grayscale image
         *
         * @param inData                   grayscale image
         * @param threshold
         * @param smallValuesAreForeground true: Image foreground <= threshold, false: Image foreground > threshold
         * @param binary                   true: output is binary image, false: output is grayscale image
         * @return binarized image
         */
        public static ImageData binarize(ImageData inData, int threshold, boolean smallValuesAreForeground, boolean binary) {
            ImageData outData = ImageProcessing.createImage(inData.width, inData.height, (binary) ? Picsi.IMAGE_TYPE_BINARY : Picsi.IMAGE_TYPE_GRAY);
            final int fg = (smallValuesAreForeground) ? s_foreground : s_background;
            final int bg = (smallValuesAreForeground) ? s_background : s_foreground;

            Parallel.For(0, inData.height, v -> {
                for (int u = 0; u < inData.width; u++) {
                    outData.setPixel(u, v, (inData.getPixel(u, v) <= threshold) ? fg : bg);
                }
            });
            return outData;
        }

        /**
         * Computes a global threshold for binarization using Otsu's method
         *
         * @param inData grayscale image
         * @return threshold
         */
        public static int otsuThreshold(ImageData inData) {
            int[] histogram = ImageProcessing.histogram(inData, (int) Math.pow(2, inData.depth));

            int n = inData.width * inData.height; // Anzahl Pixel

            int threshold = 0; // Schwellwert
            double max_varianz = 0;


            float total_intensity = 0;
            for (int t = 0; t < histogram.length; t++) {
                total_intensity += t * histogram[t];
            }

            float intensity_b = 0; // Kumultative Hintergrund-Intensität
            float weight_p0 = 0; // Kumultative Wahrscheinlichkeit p_i des Hintergrundes
            float weight_p1 = 0; // Kumultative Wahrscheinlichkeit p_i des Vordergrunds (1 - p0)

            for (int t = 0; t < histogram.length; t++) {
                weight_p0 += histogram[t];
                if (weight_p0 == 0) continue;

                weight_p1 = n - weight_p0;
                if (weight_p1 == 0) break;

                intensity_b += (float) (t * histogram[t]);

                float mu_0 = intensity_b / weight_p0;
                float mu_1 = (total_intensity - intensity_b) / weight_p1;

                float o_inter = weight_p0 * weight_p1 * (mu_0 - mu_1) * (mu_0 - mu_1);

                // Interklassenvarianz maximiert == Intraklassenvarianz minimiert
                if (o_inter > max_varianz) {
                    max_varianz = o_inter;
                    threshold = t;
                }
            }

            return threshold;
        }
    }

    /**
     * Flood Filling
     *
     * @author Christoph Stamm
     * @author Denis Wilhelm (extension/implementation of floodfilling
     */
    private static class FloodFilling {
        public static int s_background = 0; // white
        public static int s_foreground = 1; // black

        /**
         * Labeling of a binarized grayscale image
         *
         * @param imageData input: grayscale image with intensities 0 and 1 only, output: labeled foreground regions
         * @return number of regions
         */
        public static int floodFillLabeling(ImageData imageData) {
            assert ImageProcessing.determineImageType(imageData) == Picsi.IMAGE_TYPE_GRAY;

            int m = 2; // label number/value of pixel

            boolean[][] visited = new boolean[imageData.width][imageData.height];
            Queue<Point> queue = new LinkedList<>();

            int[] neighbour_x = {1, -1, 0, 0, 1, 1, -1, -1};
            int[] neighbour_y = {0, 0, 1, -1, 1, -1, 1, -1};

            for (int v = 0; v < imageData.height; v++) {
                for (int u = 0; u < imageData.width; u++) {
                    if (imageData.getPixel(u, v) == s_foreground && !visited[u][v]) {    //add Vordergrund to queue
                        queue.add(new Point(u, v));
                        visited[u][v] = true;

                        while (!queue.isEmpty()) {
                            Point point = queue.poll();
                            int x = point.x;
                            int y = point.y;

                            imageData.setPixel(x, y, m);

                            // add neighbours
                            for (int i = 0; i < 4; i++) {
                                int x_n = x + neighbour_x[i];
                                int y_n = y + neighbour_y[i];

                                if (x_n >= 0 &&
                                        x_n < imageData.width &&
                                        y_n >= 0 &&
                                        y_n < imageData.height &&
                                        !visited[x_n][y_n] &&
                                        imageData.getPixel(x_n, y_n) == s_foreground) {
                                    queue.add(new Point(x_n, y_n));
                                    visited[x_n][y_n] = true;
                                }
                            }
                        }

                        m++;
                    }
                }
            }

            // Background has to be white -> 255 in grayscale
            for (int v = 0; v < imageData.height; v++) {
                for (int u = 0; u < imageData.width; u++) {
                    if (!visited[u][v]) {
                        imageData.setPixel(u, v, 255);
                    }
                }
            }

            return m - 2;   // -2 not counting background and 'default' foreground
        }

        /**
         * Fill any holes inside a particle. Set hole to s_foreground
         *
         * @param imageData input: binarized image (with closing happening beforehand) with 0 and 1 values.
         */
        public static void fillHoles(ImageData imageData) {
            boolean[][] visited = new boolean[imageData.width][imageData.height];
            Queue<Point> queue = new LinkedList<>();

            int[] neighbour_x = {1, -1, 0, 0};
            int[] neighbour_y = {0, 0, 1, -1};

            // Flood fill from the borders to mark all background-connected regions
            for (int v = 0; v < imageData.height; v++) {
                for (int u = 0; u < imageData.width; u++) {
                    if ((v == 0 || v == imageData.height - 1 || u == 0 || u == imageData.width - 1) && imageData.getPixel(u, v) == s_background && !visited[u][v]) {
                        queue.add(new Point(u, v));
                        visited[u][v] = true;

                        while (!queue.isEmpty()) {
                            Point point = queue.poll();
                            int x = point.x;
                            int y = point.y;

                            imageData.setPixel(x, y, 255);

                            for (int i = 0; i < 4; i++) {
                                int x_n = x + neighbour_x[i];
                                int y_n = y + neighbour_y[i];

                                if (x_n >= 0 && x_n < imageData.width && y_n >= 0 && y_n < imageData.height && !visited[x_n][y_n] && imageData.getPixel(x_n, y_n) == s_background) {
                                    queue.add(new Point(x_n, y_n));
                                    visited[x_n][y_n] = true;
                                }
                            }
                        }
                    }
                }
            }

            // Any unvisited background pixel inside the foreground is a hole and should be filled
            for (int v = 0; v < imageData.height; v++) {
                for (int u = 0; u < imageData.width; u++) {
                    if (!visited[u][v] && imageData.getPixel(u, v) == s_background) {
                        imageData.setPixel(u, v, s_foreground);
                    }
                }
            }
        }

        /**
         * False color presentation of labeled grayscale image
         *
         * @param inData labeled grayscale image
         * @param n      number of different false colors (<= 256)
         * @return indexed color image
         */
        public static ImageData falseColor(ImageData inData, int n) {
            assert ImageProcessing.determineImageType(inData) == Picsi.IMAGE_TYPE_GRAY;
            assert 0 < n && n <= 256;

            ImageData outData = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_RGB);

            Random random = new Random();
            Color[] colors = new Color[n];
            colors[0] = Color.WHITE;    // background
            colors[1] = Color.BLACK;    // foreground (shouldn't exist -> all labeled

            for (int i = 2; i < n; i++) {
                colors[i] = new Color(
                        random.nextInt(256),
                        random.nextInt(256),
                        random.nextInt(256));
            }


            for (int v = 0; v < inData.height; v++) {
                for (int u = 0; u < inData.width; u++) {
                    int value = inData.getPixel(u, v);
                    if (value != 255) {  // all non-Background pixels will be colored
                        outData.setPixel(u, v, colors[value].getRGB());
                    } else {
                        outData.setPixel(u, v, Color.WHITE.getRGB());
                    }
                }
            }

            return outData;
        }
    }

    /**
     * Morphologic filter and demo
     *
     * @author Christoph Stamm
     * @author Denis Wilhelm (Dilation, Opening, Closing, Contour)
     */
    private static class MorphologicFilter {
        public static int s_background = 0; // white
        public static int s_foreground = 1; // black
        public static boolean[][] s_circle3 = new boolean[][]{
                {false, true, false},
                {true, true, true},
                {false, true, false}};
        public static boolean[][] s_circle5 = new boolean[][]{
                {false, true, true, true, false},
                {true, true, true, true, true},
                {true, true, true, true, true},
                {true, true, true, true, true},
                {false, true, true, true, false}};
        public static boolean[][] s_circle7 = new boolean[][]{
                {false, false, true, true, true, false, false},
                {false, true, true, true, true, true, false},
                {true, true, true, true, true, true, true},
                {true, true, true, true, true, true, true},
                {true, true, true, true, true, true, true},
                {false, true, true, true, true, true, false},
                {false, false, true, true, true, false, false}};
        public static boolean[][] s_diamond5 = new boolean[][]{
                {false, false, true, false, false},
                {false, true, true, true, false},
                {true, true, true, true, true},
                {false, true, true, true, false},
                {false, false, true, false, false}};
        public static boolean[][] s_diamond7 = new boolean[][]{
                {false, false, false, true, false, false, false},
                {false, false, true, true, true, false, false},
                {false, true, true, true, true, true, false},
                {true, true, true, true, true, true, true},
                {false, true, true, true, true, true, false},
                {false, false, true, true, true, false, false},
                {false, false, false, true, false, false, false}};
        public static boolean[][] s_square2 = new boolean[][]{
                {true, true},
                {true, true}};
        public static boolean[][] s_square3 = new boolean[][]{
                {true, true, true},
                {true, true, true},
                {true, true, true}};
        public static boolean[][] s_square4 = new boolean[][]{{true, true, true, true}, {true, true, true, true}, {true, true, true, true}, {true, true, true, true}};
        public static boolean[][] s_square5 = new boolean[][]{{true, true, true, true, true}, {true, true, true, true, true}, {true, true, true, true, true}, {true, true, true, true, true}, {true, true, true, true, true}};

        /**
         * Erosion: if the structure element is empty, then the eroded image only contains foreground pixels
         *
         * @param inData binary image or binarized grayscale image
         * @param struct all true elements belong to the structure
         * @param cx     origin of the structure (hotspot)
         * @param cy     origin of the structure (hotspot)
         * @return new eroded binary image
         */
        public static ImageData erosion(ImageData inData, boolean[][] struct, int cx, int cy) {
            assert inData.type == Picsi.IMAGE_TYPE_BINARY || inData.type == Picsi.IMAGE_TYPE_GRAY;

            ImageData outData = (ImageData) inData.clone();

            Parallel.For(0, outData.height, v -> {
                for (int u = 0; u < outData.width; u++) {
                    boolean set = true;

                    for (int j = 0; set && j < struct.length; j++) {
                        final int v0 = v + j - cy;

                        for (int i = 0; set && i < struct[j].length; i++) {
                            final int u0 = u + i - cx;

                            if (struct[j][i] && (v0 < 0 || v0 >= inData.height || u0 < 0 || u0 >= inData.width || inData.getPixel(u0, v0) != s_foreground)) {
                                set = false;
                            }
                        }
                    }
                    if (set) outData.setPixel(u, v, s_foreground); // foreground
                    else outData.setPixel(u, v, s_background); // background
                }
            });
            return outData;
        }

        /**
         * Dilation: if the structure element is empty, then the dilated image is empty, too
         *
         * @param inData binary image or binarized grayscale image
         * @param struct all true elements belong to the structure
         * @param cx     origin of the structure (hotspot)
         * @param cy     origin of the structure (hotspot)
         * @return new dilated binary image
         */
        public static ImageData dilation(ImageData inData, boolean[][] struct, int cx, int cy) {
            assert inData.type == Picsi.IMAGE_TYPE_BINARY || inData.type == Picsi.IMAGE_TYPE_GRAY;

            ImageData outData = new ImageData(inData.width, inData.height, inData.depth, inData.palette); // outData is initialized with 0
            Parallel.For(0, outData.height, v -> {
                for (int u = 0; u < outData.width; u++) {
                    if (inData.getPixel(u, v) == s_foreground) {
                        for (int i = 0; i < struct.length; i++) {
                            final int u0 = u + i - cx;

                            for (int j = 0; j < struct[i].length; j++) {
                                final int v0 = v + j - cy;

                                if (struct[i][j]) {
                                    outData.setPixel(u0, v0, s_foreground);
                                }
                            }
                        }
                    }
                }
            });

            return outData;
        }

        /**
         * Opening
         *
         * @param inData       not an indexed-color image
         * @param struct       all true elements belong to the structure
         * @param cx           origin of the structure (hotspot)
         * @param cy           origin of the structure (hotspot)
         * @param multiplicity
         * @return new opened binary image
         */
        public static ImageData opening(ImageData inData, boolean[][] struct, int cx, int cy, int multiplicity) {
            ImageData out = (ImageData) inData.clone();

            for (int i = 0; i < multiplicity; i++) {
                out = erosion(out, struct, cx, cy);
            }

            for (int i = 0; i < multiplicity; i++) {
                out = dilation(out, struct, cx, cy);
            }

            return out;
        }

        /**
         * Closing
         *
         * @param inData       not an indexed-color image
         * @param struct       all true elements belong to the structure
         * @param cx           origin of the structure (hotspot)
         * @param cy           origin of the structure (hotspot)
         * @param multiplicity
         * @return new closed binary image
         */
        public static ImageData closing(ImageData inData, boolean[][] struct, int cx, int cy, int multiplicity) {
            ImageData out = (ImageData) inData.clone();

            for (int i = 0; i < multiplicity; i++) {
                out = dilation(out, struct, cx, cy);
            }

            for (int i = 0; i < multiplicity; i++) {
                out = erosion(out, struct, cx, cy);
            }

            return out;
        }

        /**
         * Contour
         *
         * @param inData not an indexed-color image
         * @param struct all true elements belong to the structure
         * @param cx     origin of the structure (hotspot)
         * @param cy     origin of the structure (hotspot)
         * @param inner: true = inner contour, false = outer contour
         * @return new closed binary image
         */
        public static ImageData contour(ImageData inData, boolean[][] struct, int cx, int cy, boolean inner) {
            ImageData out = new ImageData(inData.width, inData.height, inData.depth, inData.palette);

            if (inner) {
                ImageData I_dash = erosion(inData, struct, cx, cy);
                ImageData I_dash_complement = (ImageData) I_dash.clone();
                Inverter.invert(I_dash_complement, out.type);

                Parallel.For(0, inData.height, v -> {
                    for (int u = 0; u < inData.width; u++) {
                        if (inData.getPixel(u, v) == I_dash_complement.getPixel(u, v)) {
                            out.setPixel(u, v, s_foreground);
                        }
                    }
                });
            } else {
                ImageData I_dash = dilation(inData, struct, cx, cy);
                ImageData I_complement = (ImageData) inData.clone();
                Inverter.invert(I_complement, I_complement.type);

                Parallel.For(0, I_complement.height, v -> {
                    for (int u = 0; u < I_complement.width; u++) {
                        if (I_complement.getPixel(u, v) == I_dash.getPixel(u, v)) {
                            out.setPixel(u, v, s_foreground);
                        }
                    }
                });
            }

            return out;
        }

    }
}
