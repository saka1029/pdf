package saka1029.pdf.itext;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import saka1029.pdf.IText;

public class TestIText {

	static final PrintWriter OUT = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

	static void testRead(String filename, boolean horizontal, String outFile) throws IOException {
		List<List<String>> texts = IText.読み込み(filename, horizontal);
		try (PrintWriter wirter = new PrintWriter(new FileWriter(outFile, StandardCharsets.UTF_8))) {
			for (int i = 0, pageSize = texts.size(); i < pageSize; ++i) {
				wirter.println("#file: " + filename + " page:" + (i + 1));
				for (String line : texts.get(i))
					wirter.println(line);
			}
		}
	}

	@Test
	public void testRead() throws IOException {
		testRead("data/tuti.pdf", true, "data/tuti-itext.txt");
//        testRead("data/kokuji.pdf", false, "data/kokuji-itext.txt");
	}
}
