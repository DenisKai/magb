package imageprocessing;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Region of Interest (ROI)
 *
 * @author Christoph Stamm
 */
public class ROI {
    public ImageData m_imageData;
    public Rectangle m_rect;

    /**
     * Create new ROI
     *
     * @param imageData image data
     * @param roi       region of interest
     */
    public ROI(ImageData imageData, Rectangle roi) {
        this.m_imageData = ImageProcessing.crop(imageData, roi.x, roi.y, roi.width, roi.height);
        this.m_rect = roi;
    }

    /**
     * @return width of ROI
     */
    public int getWidth() {
        return m_rect.width;
    }

    /**
     * @return height of ROI
     */
    public int getHeight() {
        return m_rect.height;
    }

    /**
     * Get pixel at position (x,y)
     *
     * @param x x-coordinate in ROI coordinate system
     * @param y y-coordinate in ROI coordinate system
     * @return
     */
    public int getPixel(int x, int y) {
        return m_imageData.getPixel(x, y);
    }

    /**
     * Set pixel at position (x,y)
     *
     * @param x   x-coordinate in ROI coordinate system
     * @param y   y-coordinate in ROI coordinate system
     * @param val
     */
    public void setPixel(int x, int y, int val) {
        m_imageData.setPixel(x, y, val);
    }

    /**
     * Returns true if this ROI overlaps with r
     *
     * @param r another ROI
     * @return
     */
    public boolean overlaps(ROI r) {
        if (r.m_rect.x + r.m_rect.width < this.m_rect.x || this.m_rect.x + this.m_rect.width < r.m_rect.x) {
            return false;
        }

        if (r.m_rect.y + r.m_rect.height < this.m_rect.y || this.m_rect.y + this.m_rect.height < r.m_rect.y) {
            return false;
        }

        return true;
    }
}
