package saka1029.pdf.itext;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;

import saka1029.pdf.IText;

public class TestIText {

	static final PrintWriter OUT = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

	/**
	 * デバッグ出力するファイル名、ページ番号、行番号を指定します。
	 */
	static final Map<String, Map<Integer, Set<Integer>>> DEBUG_MAP = new HashMap<>();

	static {
//	    DEBUG_MAP.put(ファイル名, Map.of(ページ番号, Set.of(行番号...)));
//	    DEBUG_MAP.put("data/kokuji.pdf", Map.of(22, Set.of(8, 9, 10, 11)));
	    DEBUG_MAP.put("data/tuti.pdf", Map.of(1, Set.of(1, 2, 3, 4, 5, 6, 7, 8)));
	}
	
	/**
	 * DEBUG_MAPで指定した条件に合致する行(Elementの集合)を出力します。
	 */
	static final IText.DebugElement DEBUG_ELEMENT = (path, pageNo, lineSpace, lineHeight, lineNo, elements) -> {
	    Map<Integer, Set<Integer>> pageLine = DEBUG_MAP.get(path);
	    if (pageLine != null) {
            Set<Integer> lines = pageLine.get(pageNo);
            if (lines != null && lines.contains(lineNo))
                OUT.printf("%s:%d:lineSpace=%s:lineHeight=%s:%d:%s%n", path, pageNo, lineSpace, lineHeight, lineNo, elements);
	    }
	};
	
	static void read(boolean horizontal, String out, String... ins) throws IOException {
	    IText itext = new IText(horizontal);
	    itext.debugElement = DEBUG_ELEMENT;
	    itext.テキスト変換(out, ins);
	}

	@Test
	public void testRead() throws IOException {
		read(true, "data/tuti-itext.txt", "data/tuti.pdf");
		read(false, "data/kokuji-itext.txt", "data/kokuji.pdf");
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
