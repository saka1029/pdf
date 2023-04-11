package saka1029.pdf.itext;

import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.Test;

import com.google.gson.Gson;

import saka1029.pdf.IText;

/**
 * 平成30年、令和1年、令和2年、令和4年の
 * PDFBoxによるPDF抽出テキストと、
 * iTextによる PDF抽出テキストを比較するためのテストケースです。
 */
public class TestTensuhyo {

	static final String TENSUHYO_DIR = "../tensuhyo/";
	static final String TENSUHYO_DATA_DIR = TENSUHYO_DIR + "data/in/";
	static final String[] PARAMS = {
		"h3004.json",
		"r0110.json",
		"r0204.json",
		"r0404.json"
	};

	static PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

	static class Param {
		String 元号, 年度;
		String[] 医科告示PDF, 医科通知PDF;
		String[] 歯科告示PDF, 歯科通知PDF;
		String[] 調剤告示PDF, 調剤通知PDF;
		String[] 施設基準告示PDF, 施設基準通知PDF;
	}

	static Param param(String jsonFile) throws IOException {
		try (Reader reader = new FileReader(jsonFile)) {
			return new Gson().fromJson(reader, Param.class);
		}
	}

	static CopyOption OW = StandardCopyOption.REPLACE_EXISTING;

	static void copyOld() throws IOException {
		for (String paramFile : PARAMS) {
			Param param = param(TENSUHYO_DIR + paramFile);
			Path src = Path.of(TENSUHYO_DATA_DIR, param.年度);
			Path dst = Path.of("data", "comp");
			Files.copy(src.resolve("i/txt/kokuji.txt"), dst.resolve(param.年度 + "-i-kokuji-old.txt"), OW);
			Files.copy(src.resolve("i/txt/tuti.txt"), dst.resolve(param.年度 + "-i-tuti-old.txt"), OW);
			Files.copy(src.resolve("s/txt/kokuji.txt"), dst.resolve(param.年度 + "-s-kokuji-old.txt"), OW);
			Files.copy(src.resolve("s/txt/tuti.txt"), dst.resolve(param.年度 + "-s-tuti-old.txt"), OW);
			Files.copy(src.resolve("t/txt/kokuji.txt"), dst.resolve(param.年度 + "-t-kokuji-old.txt"), OW);
			Files.copy(src.resolve("t/txt/tuti.txt"), dst.resolve(param.年度 + "-t-tuti-old.txt"), OW);
			Files.copy(src.resolve("k/txt/kokuji.txt"), dst.resolve(param.年度 + "-k-kokuji-old.txt"), OW);
			Files.copy(src.resolve("k/txt/tuti.txt"), dst.resolve(param.年度 + "-k-tuti-old.txt"), OW);
		}
	}
	
	static String[] path(String nendo, String tensuhyo, String[] names) {
		int length = names.length;
		String[] t = new String[length];
		for (int i = 0; i < length; ++i)
			t[i] = TENSUHYO_DATA_DIR + nendo + "/" + tensuhyo + "/pdf/" + names[i];
		return t;
	}

	static void copyNew() throws IOException {
		for (String paramFile : PARAMS) {
			Param param = param(TENSUHYO_DIR + paramFile);
			String n = param.年度;
			String dst = "data/comp/";
			IText.テキスト変換(path(n, "i", param.医科告示PDF), dst + param.年度 + "-i-kokuji-new.txt", true);
			IText.テキスト変換(path(n, "i", param.医科通知PDF), dst + param.年度 + "-i-tuti-new.txt", true);
			IText.テキスト変換(path(n, "s", param.歯科告示PDF), dst + param.年度 + "-s-kokuji-new.txt", true);
			IText.テキスト変換(path(n, "s", param.歯科通知PDF), dst + param.年度 + "-s-tuti-new.txt", true);
			IText.テキスト変換(path(n, "t", param.調剤告示PDF), dst + param.年度 + "-t-kokuji-new.txt", true);
			IText.テキスト変換(path(n, "t", param.調剤通知PDF), dst + param.年度 + "-t-tuti-new.txt", true);
			IText.テキスト変換(path(n, "k", param.施設基準告示PDF), dst + param.年度 + "-k-kokuji-new.txt", false);
			IText.テキスト変換(path(n, "k", param.施設基準通知PDF), dst + param.年度 + "-k-tuti-new.txt", true);
		}
	}

	@Test
	public void test() throws IOException {
		copyOld();
		copyNew();
	}

}
