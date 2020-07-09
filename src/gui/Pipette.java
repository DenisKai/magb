package gui;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

import main.Picsi;

/**
 * Pipette for input view
 * 
 * @author Christoph Stamm
 *
 */
public class Pipette {
	static Cursor s_pipette = null;
	View m_view = Picsi.getTwinView().getView(true);
	MouseListener m_mouseListener;
	KeyListener m_keyListener;
	boolean m_finished = false;
	int m_x = -1, m_y;
	
	static {
		try {
			ImageData image = new ImageData(Picsi.s_shell.getClass().getClassLoader().getResource("images/pipetteHS.png").openStream());
			s_pipette = new Cursor(Picsi.s_shell.getDisplay(), image, 0, image.height - 1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Pipette() {
		m_view.setFocus();
		Picsi.getTwinView().m_mainWnd.setEnabledMenu(false);
	}
	
	/**
	 * Starts a pipette for picking a color
	 * @return RGB or null
	 */
	public RGB start() {
		Display display = m_view.getDisplay();
		
		m_keyListener = new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {}
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.ESC) m_finished = true;
			}
		};
		m_view.addKeyListener(m_keyListener);
		
		m_mouseListener = new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent event) {}
			@Override
			public void mouseDown(MouseEvent event) {}  
			@Override
			public void mouseUp(MouseEvent event) {
				if (event.button == 1) {
					// button 1 (left mouse button) has been released
					m_x = event.x;
					m_y = event.y;
					m_finished = true;
				}
			}
		};
		m_view.addMouseListener(m_mouseListener);
				
		try {
			Cursor cursor = m_view.getCursor();
			m_view.setCursor(s_pipette); 
			while (!m_finished && !m_view.isDisposed()) 
				if (!display.readAndDispatch()) display.sleep();
			m_view.setCursor(cursor); 

			Object[] values = null;
			
			if (m_x >= 0) {
				// read color at saved position
				values = m_view.getPixelInfoAt(m_x, m_y); 
			}
			if (values != null) {
				int pixel = (int)values[2];
				RGB rgb = m_view.getImageData().palette.getRGB(pixel);
				return (m_view.isDisposed()) ? null : rgb;
			} else {
				return null;
			}
			
		} finally {
			stop();
		}
	}
	
	private void stop() {
		if (!m_view.isDisposed()) {
			m_view.removeMouseListener(m_mouseListener);
			m_view.removeKeyListener(m_keyListener);
			Picsi.getTwinView().m_mainWnd.setEnabledMenu(true);
		}
	}
}
