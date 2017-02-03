package org.apache.lucene.javacafe;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Test;

public class TestStandardAnalyzer {

	/**
	 * 테스트 성공
	 * @throws IOException
	 */
	@Test
	public void test() throws IOException {
		Analyzer a = new StandardAnalyzer();
		
		assertAnalyzesTo(a, "안녕하세요 한글입니다", new String[]{"안녕하세요", "한글입니다"});
	}

	
	/**
	 * 테스트 실패 (내부적으로 LowercaseFilter를 타기때문에)
	 * @throws Exception
	 */
	@Test
	public void testArmenian() throws Exception {
		Analyzer a = new StandardAnalyzer();

		assertAnalyzesTo(a,
				"Վիքիպեդիայի 13 միլիոն հոդվածները (4,600` հայերեն վիքիպեդիայում) գրվել են կամավորների կողմից ու համարյա բոլոր հոդվածները կարող է խմբագրել ցանկաց մարդ ով կարող է բացել Վիքիպեդիայի կայքը։",
				new String[] { "Վիքիպեդիայի", "13", "միլիոն", "հոդվածները", "4,600", "հայերեն", "վիքիպեդիայում",
						"գրվել", "են", "կամավորների", "կողմից", "ու", "համարյա", "բոլոր", "հոդվածները", "կարող", "է",
						"խմբագրել", "ցանկաց", "մարդ", "ով", "կարող", "է", "բացել", "Վիքիպեդիայի", "կայքը" });
	}

	  
	
	private void assertAnalyzesTo(Analyzer a, String src, String[] dest) throws IOException {
		List<String> terms = new ArrayList<>();
		
		TokenStream ts = a.tokenStream("dummy", src);
		CharTermAttribute termAtt = ts.getAttribute(CharTermAttribute.class);
		
		ts.reset();
		while (ts.incrementToken()) {
			terms.add(termAtt.toString());
		}
		ts.end();
		ts.close();
		
		if (dest.length != terms.size()) {
			assertEquals("", "FAIL");
		}
		
		int i=0;
		for (String term : terms) {
			if (!term.equals(dest[i])) {
				assertEquals("", "FAIL");
			}
			i++;
		}
		
		assertEquals("OK", "OK");
	}
	
}
