package gui;

import imageprocessing.Binarization;
import imageprocessing.Cropping;
import imageprocessing.FloodFilling;
import imageprocessing.MorphologicFilter;
import imageprocessing.RotateAndZoom;
import imageprocessing.Rotation;
import imageprocessing.Scaling;
import imageprocessing.colors.ChannelRGBA;
import imageprocessing.colors.Inverter;
import imageprocessing.grayValueConverter.GrayValue;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Image processing menu
 *
 * @author Christoph Stamm
 */
public class ImageMenu extends UserMenu {
    /**
     * Registration of image operations
     *
     * @param item  menu item
     * @param views twin view
     * @param mru   MRU
     */
    public ImageMenu(MenuItem item, TwinView views, MRU mru) {
        super(item, views, mru);

        // add(menuText, shortcut, instanceOfIImageProcessor)
        add("C&ropping\tCtrl+R", SWT.CTRL | 'R', new Cropping());
        add("&Invert\tF1", SWT.F1, new Inverter());

        // TODO add here further image processing entries (they are inserted into the Image menu)
        add("&Grayscaler\tF2", SWT.F2, new GrayValue());
        add("&Binary\tF3", SWT.F3, new Binarization());
        add("&Rotation\tF4", SWT.F4, new Rotation());
        add("&Rotate and Zoom\tF5", SWT.F5, new RotateAndZoom());
        add("&Static Scaling\tF6", SWT.F6, new Scaling());
        add("&Floodfill\tF7", SWT.F7, new FloodFilling());
        add("&Morphological\tF8", SWT.F8, new MorphologicFilter());

        UserMenu channels = addMenu("Channel");
        channels.add("R\tCtrl+1", SWT.CTRL | '1', new ChannelRGBA(0));
        channels.add("G\tCtrl+2", SWT.CTRL | '2', new ChannelRGBA(1));
        channels.add("B\tCtrl+3", SWT.CTRL | '3', new ChannelRGBA(2));
        channels.add("A\tCtrl+4", SWT.CTRL | '4', new ChannelRGBA(3));

    }
}
