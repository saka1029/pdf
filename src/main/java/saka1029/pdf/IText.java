package saka1029.pdf;

import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.itextpdf.awt.geom.Rectangle2D;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

public class IText {

//	public static final PrintWriter OUT = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

	/**
	 * A4のポイントサイズ 横約8.27 × 縦約11.69 インチ 595.44 x 841.68 ポイント
	 */
	public static final float PAGE_WIDTH = 596F, PAGE_HEIGHT = 842F;

	record Element(float x, float y, float w, float h, String text) {
		public int length() {
			return text.length();
		}
		
		@Override
		public String toString() {
			return "%sx%s@%s%s:%s".formatted(x, y, w, h, text);
		}
	}
	
	interface DebugElement {
		void element(String path, int pageNo, int lineNo, TreeSet<Element> element);
	}

	// オプションパラメータ
	public String newLine = "\n";
	public Charset outCharset = StandardCharsets.UTF_8;
	public float lineHeightRate = 1.2F;
	public float rubyRate = 0.6F;
	public float defaultHeight = 10F;
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

	static final Comparator<Element> IN_PAGE_SORT = Comparator.comparing(Element::y)
			.thenComparing(Comparator.comparing(Element::x));

	static final Comparator<Element> IN_LINE_SORT = Comparator.comparing(Element::x)
			.thenComparing(Comparator.comparing(Element::y).reversed());

	String string(TreeSet<Element> line, float leftMargin, float charWidth) {
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

	static String lineDirective(String path, int pageNo) {
		return "#file: %s page: %d".formatted(path, pageNo);
	}

	public List<List<String>> read(String... paths) throws IOException {
		int pathCount = paths.length;
		List<List<String>> result = new ArrayList<>();
		List<List<List<Element>>> files = new ArrayList<>();
		for (String path : paths) {
			List<List<Element>> file = new ArrayList<>();
			files.add(file);
			PdfReader reader = new PdfReader(path);
			try (Closeable c = () -> reader.close()) {
				int pageSize = reader.getNumberOfPages();
				PdfReaderContentParser parser = new PdfReaderContentParser(reader);
				for (int pageNo = 1; pageNo <= pageSize; ++pageNo)
					file.add(parse(path, parser, pageNo));
			}
		}
		// 最もインデントの小さい行をレフトマージンとします。
		float leftMargin = (float) files.stream()
			.flatMap(List::stream)
			.flatMap(List::stream)
			.mapToDouble(e -> e.x)
			.min().orElse(0);
		// 行に分割します。
		for (int pathNo = 0; pathNo < pathCount; ++pathNo) {
			String path = paths[pathNo];
			List<List<Element>> pages = files.get(pathNo);
			int pageSize = pages.size();
			for (int pageNo = 1; pageNo <= pageSize; ++pageNo) {
				List<Element> page = pages.get(pageNo - 1);
				List<String> pageString = new ArrayList<>();
				result.add(pageString);
				pageString.add(lineDirective(path, pageNo));
				Collections.sort(page, IN_PAGE_SORT);
				// ページ内の最頻出文字高さを求めます。
				float freqHeight = page.stream()
					.collect(Collectors.groupingBy(Element::h,
						Collectors.summingInt(Element::length)))
					.entrySet().stream()
					.max(Entry.comparingByValue())
					.map(Entry::getKey)
					.orElse(defaultHeight);
				float lineHeight = freqHeight * lineHeightRate; // 1行の高さ
				float rubyHeight = freqHeight * rubyRate; // 最大ルビ文字高さ
				float yStart = -100;
				int lineNo = 0;
				TreeSet<Element> line = new TreeSet<>(IN_LINE_SORT);
				// lineHeight内に収まるテキストを1行にマージします。
				for (Iterator<Element> it = page.iterator(); it.hasNext();) {
					Element element = it.next();
					if (element.h <= rubyHeight && element.text.matches("\\p{IsHiragana}*"))
						continue;
					if (element.y > yStart + lineHeight) {
						if (!line.isEmpty()) {
							pageString.add(string(line, leftMargin, freqHeight));
							if (debugElement != null)
								debugElement.element(path, pageNo, ++lineNo, line);
						}
						line.clear();
						yStart = element.y;
					}
					line.add(element);
				}
				if (!line.isEmpty()) {
					pageString.add(string(line, leftMargin, freqHeight));
					if (debugElement != null)
						debugElement.element(path, pageNo, ++lineNo, line);
				}
			}
		}
		return result;
	}

	public void テキスト変換(String outFile, String... inFiles) throws IOException {
		List<List<String>> texts = read(inFiles);
		try (PrintWriter wirter = new PrintWriter(new FileWriter(outFile, outCharset))) {
			for (int i = 0, pageSize = texts.size(); i < pageSize; ++i)
				for (String line : texts.get(i))
					wirter.print(line + newLine);
		}
	}

}
