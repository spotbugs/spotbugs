package edu.umd.cs.findbugs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class DebugPrinter {
	private static PrintStream ps = null;
	private static final String outFileName="SpotbugsDebugCore.txt";
	
	private static void initMyPrinter() {
		try {
			FileOutputStream fs = new FileOutputStream(new File(outFileName));
			ps = new PrintStream(fs, false, "UTF-8");
			ps.println("Printer begion work.");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public static void printStrToFile(Object obj) {
		if (ps == null) {
			initMyPrinter();
		}
		String str = "null";
		if (obj != null) {
			str = obj.toString();
		}
		ps.append(str);
		ps.append(System.getProperty("line.separator"));
	}
}
