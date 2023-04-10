package saka1029.pdf.itext;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import saka1029.pdf.IText;

public class TestIText {

	static final PrintWriter OUT = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

	@Test
	public void testRead() throws IOException {
		IText.テキスト変換("data/tuti.pdf", true, "data/tuti-itext.txt");
        IText.テキスト変換("data/kokuji.pdf", false, "data/kokuji-itext.txt");
	}
}
