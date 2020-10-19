package com.compress.text;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.Test;

public class TextFileCompressorTest {
	
	@Test
	public void testCompressingFileEqualsCase() throws IOException, URISyntaxException {
		Path source = Paths.get(getClass().getResource("test_equals_in.txt").toURI()); 
		//NOTE: actual file when running the test files are in the bin folder  <workspace>\Compress\bin\com\compress\text
		Path destination = Paths.get(getClass().getResource("out.txt").toURI());
		TextFileCompressor c = new TextFileCompressor(source, destination);
		c.compress();
		List<String> outLines = Files.readAllLines(destination);
		List<String> expected = new ArrayList<String>();
		expected.addAll(Arrays.asList("aa", "b", "bb", "c", "m"));
		assertTrue(outLines.size() == expected.size() && outLines.containsAll(expected) && expected.containsAll(outLines));
	}
	
	@Test
	public void testCompressingFileCustomSimilarityAndComparator() throws URISyntaxException, IOException {
		Path source = Paths.get(getClass().getResource("test_json_in.txt").toURI()); 
		Path destination = Paths.get(getClass().getResource("out2.txt").toURI());
		BiFunction<String, String, Boolean> similar = (String o1, String o2) -> {
			int startIndex1 = o1.indexOf("id");
			int startIndex2 = o2.indexOf("id");
			return o1.substring(startIndex1).equals(o2.substring(startIndex2));
		};
		Comparator<String> comp = (String o1, String o2)->{
			int startIndex1 = o1.indexOf("id");
			int startIndex2 = o2.indexOf("id");
			return o1.substring(startIndex1).compareTo(o2.substring(startIndex2));
		};
		TextFileCompressor c = new TextFileCompressor(source, destination, similar, comp);
		c.compress();
		List<String> outLines = Files.readAllLines(destination);
		List<String> expected = new ArrayList<String>();
		expected.addAll(Arrays.asList("{creationTime: 11233, id: 1, prop: \"some prop\"}", 
				"{creationTime: 11235, id: 2, prop: \"some prop\"}",
				"{creationTime: 11236, id: 3, prop: \"some prop\"}"));
		assertTrue(outLines.size() == expected.size() && outLines.containsAll(expected) && expected.containsAll(outLines));
	}
}
