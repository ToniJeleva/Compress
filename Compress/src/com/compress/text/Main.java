package com.compress.text;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

	static Logger logger = Logger.getLogger(Main.class.getName());

	public static void main(String[] args) {

		Path source = Paths.get(args[0]);
		Path destination = Paths.get(args[1]);
		TextFileCompressor compressor = new TextFileCompressor(source, destination);
		logger.log(Level.INFO, "Started compressing.");
		compressor.compress();
		logger.log(Level.INFO, "Ended compressing.");
	}
}
