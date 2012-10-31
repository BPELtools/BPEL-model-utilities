package org.bpel4chor.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

/**
 * ZipUtil compresses the files into destined zip file and decompresses the zip
 * file into directory.
 * 
 * @since Oct 30, 2011
 * @author Daojun Cui
 */
public class ZipUtil {

	static final int BUFFER = 2048;

	static final Logger logger = Logger.getLogger(ZipUtil.class);

	/**
	 * Compress the files under the output directory and create the zip file
	 * inside the given destine directory.
	 * 
	 * @param outputDirPath
	 *            The output directory of the files which we want to compress
	 *            into zip file
	 * @param destDir
	 *            The destine directory where we want to place the zip file.
	 * @param destZipName
	 *            The name of the destine zip file.
	 *            
	 * @return the resulted zip file path
	 * 
	 * @throws IOException
	 */
	public static String zip(String outputDirPath, String destDir, String destZipName)
			throws IOException {

		BufferedInputStream origin = null;

		File zipFile = getZipFile(destDir, destZipName);
		FileOutputStream dest = new FileOutputStream(zipFile);

		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
		// out.setMethod(ZipOutputStream.DEFLATED);
		byte data[] = new byte[BUFFER];
		// get a list of files from current directory
		File f = new File(outputDirPath);
		String files[] = f.list();

		for (int i = 0; i < files.length; i++) {

			logger.info("Adding: " + files[i]);
			FileInputStream fi = new FileInputStream(outputDirPath + File.separator + files[i]);
			origin = new BufferedInputStream(fi, BUFFER);
			ZipEntry entry = new ZipEntry(files[i]);
			out.putNextEntry(entry);
			int count;
			while ((count = origin.read(data, 0, BUFFER)) != -1) {
				out.write(data, 0, count);
			}
			origin.close();
		}
		out.close();
		
		String resultZipFile = destDir + File.separator + zipFile.getName();
		logger.info("Zip Result: " + resultZipFile);
		return resultZipFile;
	}

	/**
	 * Decompress the zip file into the given directory
	 * 
	 * @param destDir
	 * @param zipFileAbsPath
	 * @throws IOException
	 * @throws ZipException
	 */
	public static void unizp(String destDir, String zipFileAbsPath) throws ZipException, IOException {

		File dir = new File(destDir);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		BufferedOutputStream dest = null;
		BufferedInputStream is = null;
		ZipEntry entry;
		ZipFile zipfile = new ZipFile(new File(zipFileAbsPath));
		Enumeration e = zipfile.entries();
		while (e.hasMoreElements()) {
			entry = (ZipEntry) e.nextElement();
			logger.info("Extracting: " + entry);
			is = new BufferedInputStream(zipfile.getInputStream(entry));
			int count;
			byte data[] = new byte[BUFFER];
			FileOutputStream fos = new FileOutputStream(destDir + File.separator + entry.getName());
			dest = new BufferedOutputStream(fos, BUFFER);
			while ((count = is.read(data, 0, BUFFER)) != -1) {
				dest.write(data, 0, count);
			}
			dest.flush();
			dest.close();
			is.close();
		}
		logger.info("Unzip output directory: " + destDir);

	}

	/**
	 * Get the zip file with the given file name under the given destine
	 * directory
	 * 
	 * @param destDir
	 *            The destine directory
	 * @param destZipName
	 *            The destine zip file name
	 * @return
	 */
	private static File getZipFile(String destDir, String destZipName) {
		
		String zipFileAbsPath = destDir + File.separator + destZipName;
		File zipFile = new File(zipFileAbsPath);
		
		int count = 1;
		while (zipFile.exists()) {
			zipFile = new File(zipFileAbsPath + (count++));
		}

		return zipFile;
	}
	
}
