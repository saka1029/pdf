package saka1029.pdf.itext;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.junit.Test;

import saka1029.pdf.IText;

public class TestIText {

	static final PrintWriter OUT = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

	@Test
	public void testRead() throws IOException {
		new IText(true).テキスト変換("data/tuti-itext.txt", "data/tuti.pdf");
		new IText(false).テキスト変換("data/kokuji-itext.txt", "data/kokuji.pdf");
	}
}
