package org.bpel4chor.model.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import junit.framework.Assert;

import org.bpel4chor.utils.ZipUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ZipUtilTest {

	static String outputDirPath = null;

	static String destDir = null;

	static String destZipName = null;

	static String zipFileAbsPath = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testZip() throws IOException {
		File file = new File(".");
		outputDirPath = file.getAbsolutePath() + File.separator + "files" + File.separator
				+ "ziptest";
		destDir = "c:\\tmp\\bpel4chor";
		destZipName = "ZipTest" + Calendar.getInstance().getTimeInMillis() + ".zip";
		String zipfilePath = ZipUtil.zip(outputDirPath, destDir, destZipName);

		File zipFile = new File(zipfilePath);
		assertZipFileValid(zipFile);
	}

	private void assertZipFileValid(File zipFile) {
		Assert.assertTrue(isValid(zipFile));
	}

	private boolean isValid(final File file) {
		ZipFile zipfile = null;
		try {
			zipfile = new ZipFile(file);
			return true;
		} catch (ZipException e) {
			return false;
		} catch (IOException e) {
			return false;
		} finally {
			try {
				if (zipfile != null) {
					zipfile.close();
					zipfile = null;
				}
			} catch (IOException e) {
			}
		}
	}

	@Test
	public void testUnzip() throws ZipException, IOException {

		File file = new File(".");
		zipFileAbsPath = file.getAbsolutePath() + File.separator + "files" + File.separator
				+ "OrderingProcessSimple1Choreography.zip";
		destDir = "c:\\tmp\\bpel4chor\\UnZipTest"
				+ Calendar.getInstance().getTimeInMillis();
		ZipUtil.unizp(destDir, zipFileAbsPath);

		File unzipDir = new File(destDir);
		String[] files = unzipDir.list();

		List<String> expectedList = Arrays.asList(new String[] { "grounding.xml", "topology.xml",
				"participant1.pbd", "participant1.wsdl", "participant2.pbd", "participant2.wsdl" });

		for (String fileName : files) {
			Assert.assertTrue(expectedList.contains(fileName));
			File unzipedfile = new File(destDir, fileName);
			Assert.assertTrue(unzipedfile.exists());
			Assert.assertTrue(unzipedfile.length() > 100);
		}
	}
}
