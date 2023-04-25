package saka1029.pdf.itext;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.junit.Test;

import saka1029.pdf.IText;

public class TestIText {

	static final PrintWriter OUT = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

	@Test
	public void testRead() throws IOException {
		new IText(true).テキスト変換("data/tuti-itext.txt", "data/tuti.pdf");
		new IText(false).テキスト変換("data/kokuji-itext.txt", "data/kokuji.pdf");
	}
	
	@Test
	public void testMatcher() {
		Pattern pat = Pattern.compile("^\\s*\\S*\\s*-\\s*\\d+\\s*-\\s*$");
		OUT.println(pat.matcher("  - 3 -").replaceFirst("#$0"));
		OUT.println(pat.matcher("  -3-").replaceFirst("#$0"));
		OUT.println(pat.matcher("  - 231 -").replaceFirst("#$0"));
		OUT.println(pat.matcher(" 加算  - 3 -").replaceFirst("#$0"));
		OUT.println(pat.matcher(" - -").replaceFirst("#$0"));
	}
}
