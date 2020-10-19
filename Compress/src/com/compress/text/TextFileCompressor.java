package com.compress.text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TextFileCompressor {
	private Path source;
	private Path destination;
	private int maxLines;
	private BiFunction<String, String, Boolean> similarityFunction;
	private Comparator<String> comparator;
	private static Logger logger = Logger.getLogger(TextFileCompressor.class.getName());

	public TextFileCompressor(Path source, Path destination) {
		this.source = source;
		this.destination = destination;
		this.maxLines = 1000000;
		similarityFunction = (String str1, String str2) -> str1.equals(str2);
		comparator = (String str1, String str2) -> str1.compareTo(str2);
	}

	public TextFileCompressor(Path source, 
							  Path destination,
							  BiFunction<String, String, Boolean> similarityFunction,
							  Comparator<String> comparator) {
		this.source = source;
		this.destination = destination;
		this.similarityFunction = similarityFunction;
		this.comparator = comparator;
	}
	
	public void setMaxLines(int maxLines) {
		this.maxLines = maxLines;
	}
	
	public int getMaxLines() {
		return this.maxLines;
	}

	public void compress() {
		List<Path> tempFiles = splitSourceToSortedFiles(source, destination, comparator);
		mergeFiles(tempFiles, destination, similarityFunction, comparator);
		deleteTempFiles(tempFiles);
	}

	/**
	 * Performs k-way merge of sorted temp files using PriorityQueue. 
	 * Removes similar copies of lines.
	 */
	private void mergeFiles(List<Path> paths, 
							Path destination, 
							BiFunction<String, String, Boolean> similarityFunction,
							Comparator<String> comparator) {
		PriorityQueue<Pair<String, BufferedReader>> queue = new PriorityQueue<>((p1, p2) -> comparator.compare(p1.getFirst(), p2.getFirst()));
		HashSet<BufferedReader> readers = new HashSet<>();
		try {
			for (Path p : paths)
				readers.add(Files.newBufferedReader(p));

			for (BufferedReader r : readers) {
				String line = r.readLine();
				if (line != null)
					queue.add(new Pair<String, BufferedReader>(line, r));
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed reading temp file.");
			throw new RuntimeException(e);
		}
		logger.log(Level.INFO, "Merging temp files.");

		int BUFFER_SIZE = 8 * 1024;
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(destination.toFile()), BUFFER_SIZE)){
			String prev = null;
			while (!queue.isEmpty()) {
				Pair<String, BufferedReader> el = queue.poll();
				if (!(prev != null && similarityFunction.apply(prev, el.getFirst()))) {
					bufferedWriter.write(el.getFirst());
					bufferedWriter.newLine();
				}
				BufferedReader reader = el.getSecond();
				String newLine = reader.readLine();
				if (newLine != null)
					queue.add(new Pair<String, BufferedReader>(newLine, reader));
				prev = el.getFirst();
			}
			bufferedWriter.flush();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed prepairing output file.");
			throw new RuntimeException(e);
		}
	}

	/**
	 * Splits the source file in smaller sorted files with MAX_LINES lines.
	 * If file contains less than MAX_LINES it is sorted in one new temp file.
	 */
	private List<Path> splitSourceToSortedFiles(Path source, Path destination, Comparator<String> comparator) {
		int tempFileCount = 0;
		List<String> readLines = new ArrayList<>(maxLines);
		List<Path> tempFiles = new ArrayList<>();
		try (BufferedReader br = Files.newBufferedReader(source)) {
			String line = br.readLine();
			while (line != null) {
				readLines.add(line);
				line = br.readLine();
				if (readLines.size() == maxLines) { 
					tempFiles.add(createSortedTempFile(readLines, destination, tempFileCount, comparator));
					readLines = new ArrayList<>(maxLines);
					tempFileCount++;
				}
			}
			if(tempFileCount == 0) {
				tempFiles.add(createSortedTempFile(readLines, destination, tempFileCount, comparator));
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed reading source file" + source.toString());
			throw new RuntimeException(e);
		}
		return tempFiles;

	}

	private Path createSortedTempFile(List<String> list, Path destination, int tempFileCount, Comparator<String> c) {
		Collections.sort(list, c);
		Path newFile = Paths.get(destination.getParent().toString() + "/temp" + tempFileCount);
		try {
			Files.write(newFile, list);
			logger.log(Level.INFO, "Created temp file " + newFile.toString());
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed writing temp file " + newFile.toString());
			throw new RuntimeException(e);
		}
		return newFile;
	}

	private void deleteTempFiles(List<Path> tempFiles) {
		logger.log(Level.INFO, "Cleanup temp files.");
		for (Path p : tempFiles) {
			File f = p.toFile();
			f.delete();
		}
	}
}
