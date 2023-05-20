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

import org.junit.Test;

import com.google.gson.Gson;
import com.itextpdf.text.DocumentException;

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

	static void copyNew(String outFile, String baseName, String... inFiles) throws IOException, DocumentException {
        new IText(true).様式一覧変換(outFile, inFiles);
        String outDir = outFile.replaceFirst(".txt", "");
        Files.createDirectories(Path.of(outDir));
        IText.ページ分割(outFile, outDir, outDir + "/" + baseName);
	}

	static void copyNew() throws IOException, DocumentException {
		for (String paramFile : PARAMS) {
			Param param = param(TENSUHYO_DIR + paramFile);
			String n = param.年度;
			String dst = "data/yoshiki/";
			copyNew(dst + param.年度 + "-i-yoshiki-new.txt", "BESI", path(n, "i", param.医科様式PDF));
			copyNew(dst + param.年度 + "-s-yoshiki-new.txt", "BESI", path(n, "s", param.歯科様式PDF));
			copyNew(dst + param.年度 + "-t-yoshiki-new.txt", "BESI", path(n, "t", param.調剤様式PDF));
			copyNew(dst + param.年度 + "-k-kihon-new.txt", "KIHON-BETTEN7-BESI", path(n, "k", param.施設基準基本様式PDF));
			copyNew(dst + param.年度 + "-k-tokkei-new.txt", "TOKKEI-BETTEN2-BESI", path(n, "k", param.施設基準特掲様式PDF));
		}
	}

	@Test
	public void test() throws IOException, DocumentException {
//		copyOldPdf();
		copyNew();
	}
	
	@Test
	public void testYoshikiName() {
	    IText it = new IText(false);
	    OUT.println(it.様式IDパターン.matcher("様式 54 の７").matches());
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
