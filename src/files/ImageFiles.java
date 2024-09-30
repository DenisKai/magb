package files;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;

import main.Picsi;

/**
 * Management of image files
 * 
 * @author Christoph Stamm
 *
 */
public class ImageFiles {
	private static final int FirstUserfileType = 100; 
	// extension of SWT.IMAGE_XXX
	public static int IMAGE_PBM = SWT.IMAGE_UNDEFINED; 
	public static int IMAGE_PGM = SWT.IMAGE_UNDEFINED;
	public static int IMAGE_PPM = SWT.IMAGE_UNDEFINED;
	public static int IMAGE_RAW = SWT.IMAGE_UNDEFINED;
	public static int IMAGE_HUF = SWT.IMAGE_UNDEFINED;

	private static final String[] OPEN_FILTER_EXTENSIONS = new String[] {
			"*.bmp;*.gif;*.ico;*.jfif;*.jpeg;*.jpg;*.png;*.tif;*.tiff",
			"*.bmp", "*.gif", "*.ico", "*.jpg;*.jpeg;*.jfif", "*.png", "*.tif;*.tiff" };
	private static final String[] OPEN_FILTER_NAMES = new String[] {
		"All images",
		"BMP (*.bmp)", "GIF (*.gif)", "ICO (*.ico)", "JPEG (*.jpg, *.jpeg, *.jfif)",
		"PNG (*.png)", "TIFF (*.tif, *.tiff)" };
	private static final String[] SAVE_FILTER_EXTENSIONS = new String[] {
		"*.bmp", "*.gif", "*.ico", "*.jpg", "*.png", "*.tif" };
	private static final String[] SAVE_FILTER_NAMES = new String[] {
		"BMP (*.bmp)", "GIF (*.gif)", "ICO (*.ico)", "JPEG (*.jpg)", "PNG (*.png)", "TIFF (*.tif)" };

	private static class ImageFile {
		private String m_fileTypeString;
		private String m_extension;					// extension without dot
		private Class<? extends IImageFile> m_cls;
		private boolean m_read;						// allows reading
		private int m_writeTypes;					// allowed image types for writing: IMAGE_TYPE_xyz | IMAGE_TYPE_abc
		
		public ImageFile(String fileTypeString, String ext, Class<? extends IImageFile> cls, boolean read, int writeTypes) {
			assert ext.charAt(0) != '.' : "wrong extension";
			m_fileTypeString = fileTypeString;
			m_extension = ext;
			m_cls = cls;
			m_read = read;
			m_writeTypes = writeTypes;
		}
		
		public IImageFile createImageFile() throws Exception {
			return m_cls.getDeclaredConstructor().newInstance();
		}
		
	}
	
	private static ArrayList<ImageFile> s_imageFiles = new ArrayList<>();
	
	/**
	 * Register user specific image files
	 */
	public static void registerUserImageFiles() {
		IMAGE_PBM = registerImageFile("PBM", "pbm", PNM.class, true, Picsi.IMAGE_TYPE_BINARY);
		IMAGE_PGM = registerImageFile("PGM", "pgm", PNM.class, true, Picsi.IMAGE_TYPE_GRAY);
		IMAGE_PPM = registerImageFile("PPM", "ppm", PNM.class, true, Picsi.IMAGE_TYPE_RGB);
		// TODO call registerImageFile(...) for each user specific image file once
		IMAGE_RAW = registerImageFile("RAW", "raw", Raw.class, true, 0);
		IMAGE_HUF = registerImageFile("HUF", "huf", Huffman.class, true, Picsi.IMAGE_TYPE_GRAY);
	}
	
	/**
	 * Registers new image file
	 * @param fileTypeString short readable string (e.g. "TIFF")
	 * @param ext file extension (e.g. "tif")
	 * @param cls class of image file
	 * @param read true if the image file is used as input file format
	 * @param writeTypes allowed image types for writing (0 = writing not allowed)
	 * @return file type of new registered image file
	 */
	private static int registerImageFile(String fileTypeString, String ext, Class<? extends IImageFile> cls, boolean read, int writeTypes) {
		if (ext.charAt(0) == '.') ext = ext.substring(1, ext.length());
		s_imageFiles.add(new ImageFile(fileTypeString, ext, cls, read, writeTypes));
		return FirstUserfileType + s_imageFiles.size() - 1;
	}

	/**
	 * Creates new image file based on file type
	 * @param fileType
	 * @return new image file
	 * @throws Exception 
	 */
	public static IImageFile createImageFile(int fileType) throws Exception {
		if (fileType < FirstUserfileType) {
			return new BMP();
		} else if (fileType < FirstUserfileType + s_imageFiles.size()) {
			ImageFile imgFile = s_imageFiles.get(fileType - FirstUserfileType);
			return imgFile.createImageFile();
		} else {
			throw new Exception("invalid fileType");
		}
	}
	
	/**
	 * Determine file type by file name extension
	 * @param filename
	 * @return file type
	 */
	public static int determinefileType(String filename) {
		String name = filename.toLowerCase();
	
		// check system file types
		if (name.endsWith("bmp"))
			return SWT.IMAGE_BMP;
		if (name.endsWith("gif"))
			return SWT.IMAGE_GIF;
		if (name.endsWith("ico"))
			return SWT.IMAGE_ICO;
		if (name.endsWith("jpg") || name.endsWith("jpeg") || name.endsWith("jfif"))
			return SWT.IMAGE_JPEG;
		if (name.endsWith("png"))
			return SWT.IMAGE_PNG;
		if (name.endsWith("tif") || name.endsWith("tiff"))
			return SWT.IMAGE_TIFF;
		
		// check user file types
		for(int i = 0; i < s_imageFiles.size(); i++) {
			if (name.endsWith(s_imageFiles.get(i).m_extension)) return FirstUserfileType + i;
		}
		
		return SWT.IMAGE_UNDEFINED;
	}
	
	/**
	 * Determine save filter index by file name extension
	 * @param saveFilterExtensions
	 * @param filename
	 * @return index to save filter extensions and save filter names
	 */
	public static int determineSaveFilterIndex(String[] saveFilterExtensions, String filename) {
		String name = filename.toLowerCase();
		
		for(int i=0; i < saveFilterExtensions.length; i++) {
			String ext = saveFilterExtensions[i].substring(2);
			if (name.endsWith(ext)) return i;
		}
		return 0;
	}
	
	/**
	 * Return file type specific short name
	 * @param fileType
	 * @return
	 */
	public static String fileTypeString(int fileType) {
		// check system file types
		switch(fileType) {
		case SWT.IMAGE_BMP: return "BMP";
		case SWT.IMAGE_GIF: return "GIF";
		case SWT.IMAGE_ICO: return "ICO";
		case SWT.IMAGE_JPEG: return "JPEG";
		case SWT.IMAGE_PNG: return "PNG";
		case SWT.IMAGE_TIFF: return "TIFF";
		}

		// check user file types
		if (fileType >= FirstUserfileType && fileType < FirstUserfileType + s_imageFiles.size()) {
			ImageFile imgFile = s_imageFiles.get(fileType - FirstUserfileType);
			return imgFile.m_fileTypeString;
		}
		
		return "";
	}

	/**
	 * Returns open filter extensions
	 * @return
	 */
	public static String[] openFilterExtensions() {
		int cnt = 0;
		for(ImageFile imgFile: s_imageFiles) {
			if (imgFile.m_read) cnt++;
		}
		if (cnt == 0) {
			return OPEN_FILTER_EXTENSIONS;
		} else {
			String[] exts = Arrays.copyOf(OPEN_FILTER_EXTENSIONS, OPEN_FILTER_EXTENSIONS.length + cnt);
			cnt = OPEN_FILTER_EXTENSIONS.length;
			for(ImageFile imgFile: s_imageFiles) {
				if (imgFile.m_read) {
					// update All images
					exts[0] += ";*." + imgFile.m_extension;
					// add new extension
					exts[cnt++] = "*." + imgFile.m_extension;
				}
			}
			return exts;
		}
	}
	
	/**
	 * Returns open filter names
	 * @return
	 */
	public static String[] openFilterNames() {
		int cnt = 0;
		for(ImageFile imgFile: s_imageFiles) {
			if (imgFile.m_read) cnt++;
		}
		if (cnt == 0) {
			return OPEN_FILTER_NAMES;
		} else {
			String[] exts = Arrays.copyOf(OPEN_FILTER_NAMES, OPEN_FILTER_NAMES.length + cnt);
			cnt = OPEN_FILTER_NAMES.length;
			for(ImageFile imgFile: s_imageFiles) {
				if (imgFile.m_read) {
					exts[cnt++] = imgFile.m_fileTypeString + " (*." + imgFile.m_extension + ")";
				}
			}
			return exts;
		}
	}

	/**
	 * Returns save filter extensions
	 * @param imageType IMAGE_TYPE_xyz
	 * @return
	 */
	public static String[] saveFilterExtensions(int imageType) {
		int cnt = 0;
		for(ImageFile imgFile: s_imageFiles) {
			if ((imgFile.m_writeTypes & imageType) == imageType) cnt++;
		}
		if (cnt == 0) {
			return SAVE_FILTER_EXTENSIONS;
		} else {
			String[] exts = Arrays.copyOf(SAVE_FILTER_EXTENSIONS, SAVE_FILTER_EXTENSIONS.length + cnt);
			cnt = SAVE_FILTER_EXTENSIONS.length;
			for(ImageFile imgFile: s_imageFiles) {
				if ((imgFile.m_writeTypes & imageType) == imageType) {
					exts[cnt++] = "*." + imgFile.m_extension;
				}
			}
			return exts;
		}
	}
	
	/**
	 * Returns open filter names
	 * @param imageType IMAGE_TYPE_xyz
	 * @return
	 */
	public static String[] saveFilterNames(int imageType) {
		int cnt = 0;
		for(ImageFile imgFile: s_imageFiles) {
			if ((imgFile.m_writeTypes & imageType) == imageType) cnt++;
		}
		if (cnt == 0) {
			return SAVE_FILTER_NAMES;
		} else {
			String[] exts = Arrays.copyOf(SAVE_FILTER_NAMES, SAVE_FILTER_NAMES.length + cnt);
			cnt = SAVE_FILTER_NAMES.length;
			for(ImageFile imgFile: s_imageFiles) {
				if ((imgFile.m_writeTypes & imageType) == imageType) {
					exts[cnt++] = imgFile.m_fileTypeString + " (*." + imgFile.m_extension + ")";
				}
			}
			return exts;
		}
	}

}
