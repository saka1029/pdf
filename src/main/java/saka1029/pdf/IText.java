package saka1029.pdf;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.itextpdf.awt.geom.Rectangle2D;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

public class IText {

	public static final PrintWriter OUT = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8),
			true);

	/**
	 * A4のポイントサイズ 横約8.27 × 縦約11.69 インチ 595.44 x 841.68 ポイント
	 */
	public static final float PAGE_WIDTH = 596F, PAGE_HEIGHT = 842F;

	record Text(float x, float y, float w, float h, String text) {
		@Override
		public String toString() {
			return x + "x" + y + ":" + w + "x" + h + ":" + text;
		}
	}

	static float round(float f) {
		return Math.round(f);
	}

	/**
	 * 横書きの場合はx座標はそのまま、y座標は反転します。 縦書きの場合はx座標は反転、y座標も反転します。
	 * 幅(w)はbaseline.boundingRectanbleのwidthです。 高さ(h)はアセントライン-ディセントラインです。
	 * 
	 * @param parser
	 * @param horizontal
	 * @param pageNo
	 * @param page
	 * @throws IOException
	 */
	static void parse(PdfReaderContentParser parser, boolean horizontal, int pageNo, List<Text> page)
			throws IOException {
		parser.processContent(pageNo, new RenderListener() {
			public void renderText(TextRenderInfo info) {
				String text = info.getText();
				if (text.isBlank())
					return;
				Rectangle2D.Float baseBox = info.getBaseline().getBoundingRectange();
				float ascent = info.getAscentLine().getBoundingRectange().y;
				float descent = info.getDescentLine().getBoundingRectange().y;
				page.add(new Text(round(horizontal ? baseBox.x : PAGE_HEIGHT - baseBox.y),
						round(horizontal ? PAGE_HEIGHT - baseBox.y : PAGE_WIDTH - baseBox.x), round(baseBox.width),
						round(ascent - descent), text));
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
	}

	static final Comparator<Text> SORT_LINE = Comparator.comparing(Text::x)
			.thenComparing(Comparator.comparing(Text::y).reversed());

	/**
	 * 1行分のTextのストリームを文字列に変換します。
	 * 
	 * @param line       1行分のTextのストリームを指定します。
	 * @param leftMargin レフトマージンをポイントサイズで指定します。 このサイズ分の行頭のスペースを無視します。
	 * @param charWidth  半角スペースの幅をポイントサイズで指定します。
	 * @return 文字列に変換した結果を返します。
	 */
	public static String toString(Collection<Text> line, float leftMargin, float charWidth) {
		List<Text> sorted = line.stream().sorted(SORT_LINE).toList();
		StringBuilder sb = new StringBuilder();
		float halfWidth = charWidth / 2;
		float start = leftMargin;
		for (Text text : sorted) {
			int spaces = Math.round((text.x - start) / halfWidth);
			for (int i = 0; i < spaces; ++i)
				sb.append(" ");
			sb.append(text.text());
			start = text.x + text.w;
		}
		return sb.toString();
	}

	public static List<List<String>> 読み込み(String filename, boolean horizontal) throws IOException {
		List<List<Text>> pages = new ArrayList<>();
		int pageSize;
		List<List<String>> result = new ArrayList<>();
		PdfReader reader = new PdfReader(filename);
		try (Closeable c = () -> reader.close()) {
			pageSize = reader.getNumberOfPages();
			PdfReaderContentParser parser = new PdfReaderContentParser(reader);
			for (int pageNo = 1; pageNo <= pageSize; ++pageNo) {
				List<Text> page = new ArrayList<>();
				pages.add(page);
				parse(parser, horizontal, pageNo, page);
			}
		}
		float leftMargin = (float) pages.stream()
			.flatMap(List::stream)
			.mapToDouble(Text::x)
			.min().orElse(0);
		for (int pageNo = 1; pageNo < pageSize; ++pageNo) {
			List<Text> page = pages.get(pageNo - 1);
			List<String> pageString = new ArrayList<>();
			result.add(pageString);
			Collections.sort(page, Comparator.comparing(Text::y).thenComparing(Text::x));
			float freqHeight = page.stream()
					.collect(Collectors.groupingBy(Text::h,
						Collectors.summingInt(t -> t.text.length())))
					.entrySet().stream()
					.max(Entry.comparingByValue())
					.map(Entry::getKey).orElse(10F);
			float lineHeight = freqHeight * 1.2F;
			float yStart = -100;
			List<Text> line = new ArrayList<>();
			for (Iterator<Text> it = page.iterator(); it.hasNext();) {
				Text text = it.next();
				if (text.y > yStart + lineHeight) {
					if (!line.isEmpty())
						pageString.add(toString(line, leftMargin, freqHeight));
					line.clear();
					yStart = text.y;
				}
				line.add(text);
			}
			if (!line.isEmpty())
				pageString.add(toString(line, leftMargin, freqHeight));
		}
		return result;
	}

}
