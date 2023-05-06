package saka1029.pdf;

import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.itextpdf.awt.geom.Rectangle2D;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

public class IText {

//    static {
//        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
//        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
//    }
	public static final PrintWriter OUT = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
	public static final Logger logger = Logger.getLogger(IText.class.getName());

	/**
	 * A4のポイントサイズ 横約8.27 × 縦約11.69 インチ 595.44 x 841.68 ポイント
	 */
	public static final float PAGE_WIDTH = 596F, PAGE_HEIGHT = 842F;

	public record Element(float x, float y, float w, float h, String text) {
		static String i(float f) {
		    return ("" + f).replaceFirst("\\.0$", "");
		}

		@Override
		public String toString() {
			return "%sx%s@%sx%s:%s".formatted(i(x), i(y), i(w), i(h), text);
		}
	}
	
	public interface DebugElement {
		void element(String path, int pageNo, int lineNo, 文書属性 attr, TreeSet<Element> elements);
	}

	// オプションパラメータ
	public String 改行文字 = "\n";
	public Charset 出力文字セット = StandardCharsets.UTF_8;
	public float 行併合範囲割合 = 0.6F;
	public float ルビ割合 = 0.6F;
	public float 行高さ規定値 = 10F;
	public float 行間隔規定値 = 14F;
	public float ゼロ幅左シフト = 8F;
	public Pattern ルビパターン = Pattern.compile("\\p{IsHiragana}*");
	public Pattern ページ番号パターン = Pattern.compile("^\\s*\\S*\\s*-\\s*\\d+\\s*-\\s*$");
	public DebugElement debugElement = null;

	// ローカルフィールド
	public final boolean horizontal;

	public IText(boolean horizontal) {
		this.horizontal = horizontal;
	}

	static float round(float f) {
		return Math.round(f);
	}

	List<Element> parse(String path, PdfReaderContentParser parser, int pageNo) throws IOException {
		List<Element> page = new ArrayList<>();
		parser.processContent(pageNo, new RenderListener() {
			public void renderText(TextRenderInfo info) {
				String text = info.getText();
				if (text.isBlank())
					return;
				Rectangle2D.Float baseBox = info.getBaseline().getBoundingRectange();
				float ascent = info.getAscentLine().getBoundingRectange().y;
				float descent = info.getDescentLine().getBoundingRectange().y;
				float shiftLeft = baseBox.width <= 0.9F ? ゼロ幅左シフト : 0F;
				Element element = new Element(
						round((horizontal ? baseBox.x : PAGE_HEIGHT - baseBox.y) - shiftLeft),
						round(horizontal ? PAGE_HEIGHT - baseBox.y : PAGE_WIDTH - baseBox.x),
						round(baseBox.width),
						round(ascent - descent), text);
				page.add(element);
			}

			@Override
			public void beginTextBlock() {
			}

			@Override
			public void endTextBlock() {
			}

			@Override
			public void renderImage(ImageRenderInfo renderInfo) {
			}
		});
		return page;
	}

	static final Comparator<Element> 行内ソート = Comparator.comparing(Element::x)
			.thenComparing(Comparator.comparing(Element::y).reversed());

	List<TreeMap<Float, List<Element>>> 行分割(List<List<Element>> pages) {
	    List<TreeMap<Float, List<Element>>> result = new ArrayList<>();
	    for (List<Element> page : pages) {
	    	TreeMap<Float, List<Element>> lines = new TreeMap<>();
	    	result.add(lines);
	    	for (Element e : page)
				lines.computeIfAbsent(e.y, k -> new ArrayList<>()).add(e);
	    }
	    return result;
	}
	
	public record 文書属性(boolean 横書き, float 左余白, float 行間隔, float 行高さ, float 行併合範囲, float ルビ高さ) {
	}
	
	文書属性 文書属性(List<TreeMap<Float, List<Element>>> pages) {
        float 左余白 = Float.MAX_VALUE;
        Map<Float, Integer> 行間隔度数分布 = new HashMap<>();
        Map<Float, Integer> 行高さ度数分布 = new HashMap<>();
        for (TreeMap<Float, List<Element>> page : pages) {
			float prevY = Float.MIN_VALUE;
			for (Entry<Float, List<Element>> line : page.entrySet()) {
				左余白 = Math.min(左余白, line.getValue().get(0).x);
				float y = line.getKey();
				if (prevY != Float.MIN_VALUE)
					行間隔度数分布.compute(y - prevY, (k , v) -> v == null ? 1 : v + 1);
				prevY = y;
				for (Element e : line.getValue())
					行高さ度数分布.compute(e.h, (k, v) -> (v == null ? 0 : v) + e.text.length());
			}
        }
        if (左余白 == Float.MAX_VALUE)
        	左余白 = 0;
        float 行間隔 = 行間隔度数分布.entrySet().stream()
			.max(Entry.comparingByValue())
			.map(Entry::getKey)
			.orElse(行間隔規定値);
        float 行高さ = 行高さ度数分布.entrySet().stream()
			.max(Entry.comparingByValue())
			.map(Entry::getKey)
			.orElse(行高さ規定値);
        float 行併合範囲 = 行高さ * 行併合範囲割合;
        float ルビ高 = 行高さ * ルビ割合;
        return new 文書属性(horizontal, 左余白, 行間隔, 行高さ, 行併合範囲, ルビ高);
	}

	/**
	 * 1行を表すElementのリストを文字列に変換します。
	 * @param line Elementのリストを指定します。
	 * @param leftMargin 行先頭の無視するx座標値を指定します。
	 * @param charWidth 平均的な1文字の幅を指定します。
	 * @return Elementを連結した文字列を返します。
	 */
	String toString(TreeSet<Element> line, float leftMargin, float charWidth) {
		StringBuilder sb = new StringBuilder();
		float halfWidth = charWidth / 2;
		float start = leftMargin;
		for (Element e : line) {
			int spaces = Math.round((e.x - start) / halfWidth);
			for (int i = 0; i < spaces; ++i)
				sb.append(" ");
			sb.append(e.text);
			start = e.x + e.w;
		}
		return sb.toString();
	}
	
	void addLine(List<String> list, TreeSet<Element> sortedLine, String path, int pageNo, int lineNo, 文書属性 文書属性) {
        if (!sortedLine.isEmpty())
            list.add(toString(sortedLine, 文書属性.左余白, 文書属性.行高さ));
        if (debugElement != null)
            debugElement.element(path, pageNo, lineNo, 文書属性, sortedLine);
        sortedLine.clear();
	}

	public List<List<String>> read(String path) throws IOException {
		List<List<String>> result = new ArrayList<>();
		List<List<Element>> elements = new ArrayList<>();
		PdfReader reader = new PdfReader(path);
		try (Closeable c = () -> reader.close()) {
			int pageSize = reader.getNumberOfPages();
			PdfReaderContentParser parser = new PdfReaderContentParser(reader);
			for (int pageNo = 1; pageNo <= pageSize; ++pageNo)
				elements.add(parse(path, parser, pageNo));
		}
		List<TreeMap<Float, List<Element>>> pageLines = 行分割(elements);
		文書属性 文書属性 = 文書属性(pageLines);
		OUT.printf("%s: %s%n", path, 文書属性);
//		logger.info("%s: %s%n".formatted(path, 文書属性));
		int pageNo = 0;
		for (TreeMap<Float, List<Element>> lines : pageLines) {
		    ++pageNo;
            List<String> linesString = new ArrayList<>();
            result.add(linesString);
			float y = Float.MIN_VALUE;
			TreeSet<Element> sortedLine = new TreeSet<>(行内ソート);
			int lineNo = 0;
			for (Entry<Float, List<Element>> line : lines.entrySet()) {
				List<Element> lineElements = line.getValue();
				if (lineElements.stream().allMatch(e -> e.h <= 文書属性.ルビ高さ && ルビパターン.matcher(e.text).matches()))
					continue;
				if (y != Float.MIN_VALUE && line.getKey() > y + 文書属性.行併合範囲)
					addLine(linesString, sortedLine, path, pageNo, ++lineNo, 文書属性);
				sortedLine.addAll(lineElements);
				y = line.getKey();
			}
			addLine(linesString, sortedLine, path, pageNo, ++lineNo, 文書属性);
		}
		return result;
	}

	public void テキスト変換(String outFile, String... inFiles) throws IOException {
		try (PrintWriter writer = new PrintWriter(new FileWriter(outFile, 出力文字セット))) {
			for (String path : inFiles) {
				List<List<String>> pages = read(path);
				for (int i = 0, pageSize = pages.size(); i < pageSize; ++i) {
					writer.printf("# file: %s page: %d%s", Path.of(path).getFileName(), i + 1, 改行文字);
					for (String line : pages.get(i))
						writer.printf("%s%s", ページ番号パターン.matcher(line).replaceFirst("#$0"), 改行文字);
				}
			}
		}
	}

}
