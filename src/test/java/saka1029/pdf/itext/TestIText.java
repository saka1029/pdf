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
//		IText.テキスト変換("data/tuti.pdf", "data/tuti-itext.txt", true);
        IText.テキスト変換("data/kokuji.pdf", "data/kokuji-itext.txt", false);
	}
	
	@Test
	public void testTreeMap() {
	    TreeMap<Float, String> map = new TreeMap<>();
	    map.put(1F, "いち");
	    map.put(2F, "に");
	    map.put(3F, "さん");
	    Iterator<Entry<Float, String>> it = map.entrySet().iterator();
	    assert it.hasNext();
	    Entry<Float, String> 一番目 = it.next();
	    assert it.hasNext();
	    Entry<Float, String> 二番目 = it.next();
	    OUT.printf("二番目=%s%n", 二番目);
	    Entry<Float, String> 二番目コピー = Map.entry(二番目.getKey(), 二番目.getValue());
	    it.remove();
	    OUT.printf("二番目=%s%n", 二番目);
	    assert it.hasNext();
	    Entry<Float, String> 三番目 = it.next();
	    OUT.printf("一番目=%s 二番目=%s 二番目コピー=%s 三番目=%s%n", 一番目, 二番目, 二番目コピー, 三番目);
	}
}
