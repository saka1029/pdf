package saka1029.pdf.itext;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.gson.Gson;

import saka1029.pdf.IText;

/**
 * 平成30年、令和1年、令和2年、令和4年の
 * PDFBoxによるPDF分割と
 * iTextによる PDF分割を比較するためのテストケースです。
 */
public class TestYoshiki {

	static final String TENSUHYO_DIR = "../tensuhyo/";
	static final String TENSUHYO_DATA_DIR = TENSUHYO_DIR + "data/in/";
	static final String[] PARAMS = {
//		"h3004.json",   平成30年度と令和元年度はPDF自体は同一なので除外する。
		"r0110.json",
		"r0204.json",
		"r0404.json"
	};

//	static PrintStream OUT = System.out;
	static PrintStream OUT = new PrintStream(System.out, true, StandardCharsets.UTF_8);

	static class Param {
		String 元号, 年度;
		String[] 医科様式PDF, 歯科様式PDF, 調剤様式PDF;
		String[] 施設基準基本様式PDF, 施設基準特掲様式PDF;
	}

	static final CopyOption OW = StandardCopyOption.REPLACE_EXISTING;

	static Param param(String jsonFile) throws IOException {
		try (Reader reader = new FileReader(jsonFile)) {
			return new Gson().fromJson(reader, Param.class);
		}
	}
	
	static String[] path(String nendo, String tensuhyo, String[] names) {
		int length = names.length;
		String[] t = new String[length];
		for (int i = 0; i < length; ++i)
			t[i] = TENSUHYO_DATA_DIR + nendo + "/" + tensuhyo + "/pdf/" + names[i];
		return t;
	}

	static void copy(String[] srcs, Path dst) throws IOException {
		for (String src : srcs) {
			Path srcPath = Path.of(src);
			Files.copy(srcPath, dst.resolve(srcPath.getFileName()), OW);
		}
	}

	static void copyOldPdf() throws IOException {
		for (String paramFile : PARAMS) {
			Param param = param(TENSUHYO_DIR + paramFile);
			String n = param.年度;
			Path dst = Path.of("data", "yoshiki");
			if (!Files.exists(dst))
                Files.createDirectory(dst);
			copy(path(n, "i", param.医科様式PDF), dst);
			copy(path(n, "s", param.歯科様式PDF), dst);
			copy(path(n, "t", param.調剤様式PDF), dst);
			copy(path(n, "k", param.施設基準基本様式PDF), dst);
			copy(path(n, "k", param.施設基準特掲様式PDF), dst);
		}
	}

	/**
	 * デバッグ出力するファイル名、ページ番号、行番号を指定します。
	 */
	static final Map<String, Map<Integer, Set<Integer>>> DEBUG_MAP = new HashMap<>();
	static {
//	    DEBUG_MAP.put(ファイル名, Map.of(ページ番号, Set.of(行番号...)));
	    DEBUG_MAP.put("0000196314.pdf", Map.of(5, Set.of(9, 10, 11, 12, 13, 14, 15),
	                                           8, Set.of(15, 16, 17)));
	}
	
//	/**
//	 * DEBUG_MAPで指定した条件に合致する行(Elementの集合)を出力します。
//	 */
//	static final IText.DebugElement DEBUG_ELEMENT = (path, pageNo, lineNo, 文書属性, elements) -> {
//	    String fileName = Path.of(path).getFileName().toString();
//	    Map<Integer, Set<Integer>> pageLine = DEBUG_MAP.get(fileName);
//	    if (pageLine != null) {
//            Set<Integer> lines = pageLine.get(pageNo);
//            if (lines != null && lines.contains(lineNo))
//                OUT.printf("%s:%d:%d:%s%n", path, pageNo, lineNo, elements);
//	    }
//	};

	static String normalize(String s) {
		return Normalizer.normalize(s, Form.NFKD);
	}

	static void copyNew() throws IOException {
		for (String paramFile : PARAMS) {
			Param param = param(TENSUHYO_DIR + paramFile);
			String n = param.年度;
			String dst = "data/yoshiki/";
			new IText(true).様式一覧変換(dst + param.年度 + "-i-yoshiki-new.txt", path(n, "i", param.医科様式PDF));
			new IText(true).様式一覧変換(dst + param.年度 + "-s-yoshiki-new.txt", path(n, "s", param.歯科様式PDF));
			new IText(true).様式一覧変換(dst + param.年度 + "-t-yoshiki-new.txt", path(n, "t", param.調剤様式PDF));
			new IText(true).様式一覧変換(dst + param.年度 + "-k-kihon-new.txt", path(n, "k", param.施設基準基本様式PDF));
			new IText(true).様式一覧変換(dst + param.年度 + "-k-tokkei-new.txt", path(n, "k", param.施設基準特掲様式PDF));
		}
	}

	@Test
	public void test() throws IOException {
//		copyOldPdf();
		copyNew();
	}
	
//	@Test
	public void testHankaku() {
		String z = "別紙様式６の３あいうアイウｱｲｳ①②⑴ ()（）";
		String nfc = Normalizer.normalize(z, Form.NFC);
		String nfd = Normalizer.normalize(z, Form.NFD);
		String nfkc = Normalizer.normalize(z, Form.NFKC);
		String nfkd = Normalizer.normalize(z, Form.NFKD);
		OUT.println("NFC:" + nfc);
		OUT.println("NFD:" + nfd);
		OUT.println("NFKC:" + nfkc);
		OUT.println("NFKD:" + nfkd);
	}
}