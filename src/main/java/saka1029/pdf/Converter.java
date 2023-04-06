package saka1029.pdf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Converter {

    /**
     * A4のポイントサイズ 横約8.27 × 縦約11.69 インチ 595.44 x 841.68 ポイント
     */
    public static final float PAGE_WIDTH = 596F, PAGE_HEIGHT = 842F;
    
    static float round(float value) {
        return Math.round(value);
    }
    
    static Text round(Text text) {
        return new Text(round(text.x), round(text.y), round(text.w), round(text.h), text.text);
    }

    /**
     * Textをx座標の値によってソートするためのコンパレータです。
     * x座標の値が同一の場合はy座標の逆順にします。
     */
    static final Comparator<Text> LEFT2RIGHT = (a, b) -> {
        int result = Float.compare(a.x, b.x);
        if (result == 0)
            result = Float.compare(b.x, a.x);
        return result;
    };

    /**
     * 縦書き、横書きに応じて座標変換を行います。
     * また座標値を丸めます。
     * PDFの原点は左下隅、X座標は右方向、Y座標は上方向です。
     * 変換後の原点は左上隅、X座標は右方向、Y座標は下方向です。
     * @param texts 変換対象のText群を指定します。
     * @param horizontal 横書きの場合はtrue、縦書きの場合はfalseを指定します。
     * @return 変換後のTextの群を指定します。
     */
    public static List<List<Text>> 座標変換(List<List<Text>> texts, boolean horizontal) {
        return texts.stream()
            .map(page -> page.stream()
                .filter(text -> !text.text.isBlank())   // 空白テキストを除去します。
                .map(text -> new Text(
                    round(horizontal ? text.x : PAGE_HEIGHT - text.y),                  // X座標変換
                    round(horizontal ? PAGE_HEIGHT - text.y : PAGE_WIDTH - text.x),     // Y座標変換
                    round(text.w),
                    round(text.h),
                    text.text))
                .toList())
            .toList();
    }

    /**
     * 頁ごとのTextを列(Y座標)および行(X座標)で単純に並び替えます。
     * @param pages Text群を指定します。
     * @return 並び替えたText群を返します。
     */
    public static List<NavigableMap<Float, NavigableSet<Text>>> 整列(List<List<Text>> pages) {
        List<NavigableMap<Float, NavigableSet<Text>>> result = new ArrayList<>();
        int numberOfPages = pages.size();
        for (int i = 0; i < numberOfPages; ++i) {
            List<Text> page = pages.get(i);
            NavigableMap<Float, NavigableSet<Text>> newPage = new TreeMap<>();
            result.add(newPage);
            for (Text text : page)
                newPage.computeIfAbsent(text.y, k -> new TreeSet<>(LEFT2RIGHT)).add(text);
        }
        return result;
    }
    
    public static String 行文字列(Collection<Text> line, float leftMargin, float charWidth) {
        StringBuilder sb = new StringBuilder();
        float halfWidth = charWidth / 2;
        float start = leftMargin;
        for (Text text : line) {
            int spaces = Math.round((text.x - start) / halfWidth);
            for (int i = 0; i < spaces; ++i)
                sb.append(" ");
            sb.append(text.text);
            start = text.x + text.w;
        }
        return sb.toString();
    }
    
    public static float 最頻文字サイズ(Stream<Text> in) {
    	return in
    	    .collect(Collectors.groupingBy(text -> text.h,
    	        Collectors.summingInt(text -> text.text.length()))) // 文字列の長さをテキストの高さで集計する。
			.entrySet().stream()
    		.max(Entry.comparingByValue())
    		.orElseGet(() -> Map.entry(10F, 0))
    		.getKey();
    }

    public static List<NavigableMap<Float, NavigableSet<Text>>> 行マージ(List<NavigableMap<Float, NavigableSet<Text>>> pages) {
        List<NavigableMap<Float, NavigableSet<Text>>> result = new ArrayList<>();
        for (NavigableMap<Float, NavigableSet<Text>> page : pages) {
        	NavigableMap<Float, NavigableSet<Text>> newPage = new TreeMap<>();
        	float 標準文字サイズ = 最頻文字サイズ(page.values().stream().flatMap(line -> line.stream()));
        }
        return result;
    }
}
