package edu.umd.cs.daveho.ba;

import java.util.*;
import java.io.*;

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * A special version of ClassParser that automatically enters
 * parsed classes into the Repository.  This allows us to
 * use the Repository to inspect the class hierarchy, based on
 * the current class path.
 */
public class RepositoryClassParser {
	private ClassParser classParser;

	/**
	 * Constructor.
	 * @param inputStream the input stream from which to read the class file
	 * @param fileName filename of the class file
	 */
	public RepositoryClassParser(InputStream inputStream, String fileName) {
		classParser = new ClassParser(inputStream, fileName);
	}

	/**
	 * Constructor.
	 * @param fileName name of the class file
	 * @throws IOException if the file cannot be read
	 */
	public RepositoryClassParser(String fileName) throws IOException {
		classParser = new ClassParser(fileName);
	}

	/**
	 * Constructor.
	 * @param zipFile name of a zip file containing the class
	 * @param fileName name of the zip entry within the class
	 * @throws IOException if the zip entry cannot be read
	 */
	public RepositoryClassParser(String zipFile, String fileName) throws IOException {
		classParser = new ClassParser(zipFile, fileName);
	}

	/**
	 * Parse the class file into a JavaClass object.
	 * If succesful, the new JavaClass is entered into the Repository.
	 * @return the parsed JavaClass
	 * @throws IOException if the class cannot be parsed
	 */
	public JavaClass parse() throws IOException {
		JavaClass jclass = classParser.parse();
		Repository.addClass(jclass);
		return jclass;
	}
}

// vim:ts=4
