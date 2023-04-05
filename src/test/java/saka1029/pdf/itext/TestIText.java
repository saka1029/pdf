package saka1029.pdf.itext;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import saka1029.pdf.Converter;
import saka1029.pdf.IText;
import saka1029.pdf.Text;

public class TestIText {

    static final PrintWriter OUT = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

    static void testRead(String filename, boolean horizontal) throws IOException {
        List<List<Text>> 読み込み済 = IText.読み込み(filename);
        List<List<Text>> 座標変換済 = Converter.座標変換(読み込み済, horizontal);
        List<NavigableMap<Float, NavigableSet<Text>>> 整列済 = Converter.整列(座標変換済);
        int numberOfPages = 整列済.size();
        for (int i = 0; i < numberOfPages; ++i) {
            NavigableMap<Float, NavigableSet<Text>> page = 整列済.get(i);
            OUT.printf("#file: %s page: %d%n", filename, i + 1);
            for (NavigableSet<Text> line : page.values())
                for (Text text : line)
                    OUT.println(text);
        }
    }

    @Test
    public void testRead() throws IOException {
        testRead("data/tuti.pdf", true);
//        testRead("data/kokuji.pdf", false);
    }
}
