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
import java.util.regex.Pattern;

import com.itextpdf.awt.geom.Rectangle2D;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

public class IText {

	public static final PrintWriter OUT = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

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
		void element(String path, int pageNo, float lineSpace, float lineHeight, int lineNo, TreeSet<Element> elements);
	}

	// オプションパラメータ
	public String 改行文字 = "\n";
	public Charset 出力文字セット = StandardCharsets.UTF_8;
	public float 行高さ範囲割合 = 0.5F;
	public float ルビ割合 = 0.6F;
	public float 行高さ規定値 = 10F;
	public float 行間隔規定値 = 14F;
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
				Element element = new Element(
						round(horizontal ? baseBox.x : PAGE_HEIGHT - baseBox.y),
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
	
	record 文書属性(boolean 横書き, float 左余白, float 行間隔, float 行高さ, float 行高さ範囲, float ルビ高さ) {
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
        float 行高さ範囲 = 行高さ * 行高さ範囲割合;
        float ルビ高 = 行高さ * ルビ割合;
        return new 文書属性(horizontal, 左余白, 行間隔, 行高さ, 行高さ範囲, ルビ高);
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
	
//	/**
//	 * 1行を表すElementのリストを文字列に変換します。
//	 * ページ番号行のパターン(pagePattern)に一致した場合は
//	 * 先頭に"#"を付与します。
//	 * @param line Elementのリストを指定します。
//	 * @param leftMargin 行先頭の無視するx座標値を指定します。
//	 * @param charWidth 平均的な1文字の幅を指定します。
//	 * @return Elementを連結した文字列を返します。
//	 */
//	String string(TreeSet<Element> line, float leftMargin, float charWidth) {
//		return ページ番号パターン
//			.matcher(stringPrimitive(line, leftMargin, charWidth))
//			.replaceFirst("#$0");
//	}

//	/**
//	 * 行の高さの最大値を求めます。
//	 */
//	float freqLineSpace(List<Element> page) {
//		TreeSet<Float> yValues = page.stream()
//			.map(Element::y)
//			.collect(Collectors.toCollection(TreeSet::new));
//		float max = -1000F, prev = -1000F;
//		for (float y : yValues) {
//		    if (prev != -1000F)
//		        max = Math.max(max, y - prev);
//		    prev = y;
//		}
//		return max == -1000F ? 行間隔規定値 : max;
//		TreeSet<Float> yValues = page.stream()
//			.map(Element::y)
//			.collect(Collectors.toCollection(TreeSet::new));
//		Map<Float, Integer> histogram = new HashMap<>();
//		float prev = -1000F;
//		for (float y : yValues) {
//			if (prev != -1000F)
//				histogram.compute(y - prev, (k, v) -> v == null ? 1 : v + 1);
//            prev = y;
//		}
//		return histogram.entrySet().stream()
//			.max(Entry.comparingByValue())
//			.map(Entry::getKey)
//			.orElse(defaultFreqLineSpace);
//	}
	
//	float freqHeight(List<Element> page) {
//		return page.stream()
//			.collect(Collectors.groupingBy(Element::h,
//				Collectors.summingInt(e -> e.text.length())))
//			.entrySet().stream()
//			.max(Entry.comparingByValue())
//			.map(Entry::getKey)
//			.orElse(行高さ規定値);
//	}

//	static String lineDirective(String path, int pageNo) {
//		return "#file: %s page: %d".formatted(Path.of(path).getFileName(), pageNo);
//	}

//	/**
//	 * Elementをフォーマットして文字列に変換します。
//	 */
//	public void format(String path, List<List<Element>> pages, float leftMargin, List<List<String>> result) {
//		for (int pageNo = 1, pageSize = pages.size(); pageNo <= pageSize; ++pageNo) {
//			List<Element> page = pages.get(pageNo - 1);
//			List<String> pageString = new ArrayList<>();
//			result.add(pageString);
//			pageString.add(lineDirective(path, pageNo));
//			Collections.sort(page, ページ内ソート);
//			// 同一行の範囲を求めます。
//			float lineSpace = freqLineSpace(page);
//			float lineRange = lineSpace * 行高さ範囲割合;
//			// 文字高さの最頻値を求めます。
//			float lineHeight = freqHeight(page);
//			// ビ文字の最大の高さを求めます。
//			float rubyHeight = lineHeight * ルビ割合;
//			float yStart = -100;
//			int lineNo = 0;
//			TreeSet<Element> line = new TreeSet<>(行内ソート);
//			// lineHeight内に収まるテキストを1行にマージします。
//			for (Iterator<Element> it = page.iterator(); it.hasNext();) {
//				Element element = it.next();
//				if (element.h <= rubyHeight && element.text.matches("\\p{IsHiragana}*"))
//					continue;
//				if (element.y > yStart + lineRange) {
//					if (!line.isEmpty()) {
//						pageString.add(string(line, leftMargin, lineHeight));
//						if (debugElement != null)
//							debugElement.element(path, pageNo, lineSpace, lineHeight, ++lineNo, line);
//					}
//					line.clear();
//				}
//				line.add(element);
//				yStart = element.y;
//			}
//			if (!line.isEmpty()) {
//				pageString.add(string(line, leftMargin, lineHeight));
//				if (debugElement != null)
//					debugElement.element(path, pageNo, lineSpace, lineHeight, ++lineNo, line);
//			}
//		}
//	}
	    
//	public List<List<String>> read(String... paths) throws IOException {
//		int pathCount = paths.length;
//		List<List<List<Element>>> files = new ArrayList<>();
//		for (String path : paths) {
//			List<List<Element>> file = new ArrayList<>();
//			files.add(file);
//			PdfReader reader = new PdfReader(path);
//			try (Closeable c = () -> reader.close()) {
//				int pageSize = reader.getNumberOfPages();
//				PdfReaderContentParser parser = new PdfReaderContentParser(reader);
//				for (int pageNo = 1; pageNo <= pageSize; ++pageNo)
//					file.add(parse(path, parser, pageNo));
//			}
//		}
//		// ファイル内の最もインデントの小さい行をレフトマージンとします。
//		float leftMargin = (float) files.stream()
//			.flatMap(List::stream)
//			.flatMap(List::stream)
//			.mapToDouble(e -> e.x)
//			.min().orElse(0);
//		List<List<String>> result = new ArrayList<>();
//		// 行に分割します。
//		for (int pathNo = 0; pathNo < pathCount; ++pathNo) {
//			String path = paths[pathNo];
//			List<List<Element>> pages = files.get(pathNo);
//            format(path, pages, leftMargin, result);
//			文書属性 dc = 文書属性(行分割(pages));
//			OUT.printf("%s: %s%n", path, dc);
//		}
//		return result;
//	}
	
	void addLine(List<String> list, TreeSet<Element> sortedLine, String path, int pageNo, int lineNo, 文書属性 文書属性) {
        if (!sortedLine.isEmpty())
            list.add(toString(sortedLine, 文書属性.左余白, 文書属性.行高さ));
        if (debugElement != null)
            debugElement.element(path, pageNo, 文書属性.行間隔, 文書属性.行高さ, ++lineNo, sortedLine);
        sortedLine.clear();
	}

	public List<List<String>> readString(String path) throws IOException {
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
		List<String> list = new ArrayList<>();
		result.add(list);
		int pageNo = 0;
		for (TreeMap<Float, List<Element>> lines : pageLines) {
//			list.add("# file: %s page: %d%s".formatted(Path.of(path).getFileName(), ++pageNo, 改行文字));
			float y = Float.MIN_VALUE;
			TreeSet<Element> sortedLine = new TreeSet<>(行内ソート);
			int lineNo = 0;
			for (Entry<Float, List<Element>> line : lines.entrySet()) {
				List<Element> lineElements = line.getValue();
				if (lineElements.stream().allMatch(e -> e.h <= 文書属性.ルビ高さ && e.text.matches("\\p{IsHiragana}*")))
					continue;
				if (y != Float.MIN_VALUE && line.getKey() > y + 文書属性.行高さ範囲)
					addLine(list, sortedLine, path, pageNo, ++lineNo, 文書属性);
				sortedLine.addAll(lineElements);
				y = line.getKey();
			}
			addLine(list, sortedLine, path, pageNo, ++lineNo, 文書属性);
		}
		return result;
	}

	public void テキスト変換(String outFile, String... inFiles) throws IOException {
		try (PrintWriter writer = new PrintWriter(new FileWriter(outFile, 出力文字セット))) {
			for (String path : inFiles) {
				List<List<String>> pages = readString(path);
				for (int i = 0, pageSize = pages.size(); i < pageSize; ++i) {
					writer.printf("# file: %s page: %d%s", Path.of(path).getFileName(), i + 1, 改行文字);
					for (String line : pages.get(i))
						writer.printf("%s%s", ページ番号パターン.matcher(line).replaceFirst("#$0"), 改行文字);
				}
			}
		}
	}

}
