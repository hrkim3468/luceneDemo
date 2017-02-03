package org.apache.lucene.javacafe;

import static org.junit.Assert.assertEquals;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.UpperCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;
/**
 * Analyzer 테스트
 * 
 * @author NAVER
 *
 */
public class TestAnalyzers {

	@Test
	public void testSimple() throws Exception {
		Analyzer a = new SimpleAnalyzer();
		
		TokenStream ts = a.tokenStream("dummy", "foo bar FOO BAR");
		
		CharTermAttribute termAtt = ts.getAttribute(CharTermAttribute.class);
		OffsetAttribute offsetAtt = ts.getAttribute(OffsetAttribute.class);
		TypeAttribute typeAtt = ts.getAttribute(TypeAttribute.class);
		PositionIncrementAttribute posIncrAtt = ts.getAttribute(PositionIncrementAttribute.class);
		PositionLengthAttribute posLengthAtt = ts.getAttribute(PositionLengthAttribute.class);

		ts.reset();
		while (ts.incrementToken()) {
			System.out.println("----------------------------------------");
			System.out.println(termAtt.toString());
			System.out.println(offsetAtt.startOffset() + " - " + offsetAtt.endOffset());
			System.out.println(typeAtt.type());
			System.out.println(posIncrAtt.getPositionIncrement());
			System.out.println(posLengthAtt.getPositionLength());
		}
		ts.end();
		ts.close();
				
		assertEquals("OK", "OK");
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
	
	
	
	
	
	
	@Test
	public void test() throws IOException {
		Analyzer a = new SimpleAnalyzer();
		assertAnalyzesTo(a, "foo bar FOO BAR", 				new String[] { "foo", "bar", "foo", "bar" });
		assertAnalyzesTo(a, "foo      bar .  FOO <> BAR", 	new String[] { "foo", "bar", "foo", "bar" });
		assertAnalyzesTo(a, "foo.bar.FOO.BAR", 				new String[] { "foo", "bar", "foo", "bar" });
		assertAnalyzesTo(a, "U.S.A.",						new String[] { "u", "s", "a" });
		assertAnalyzesTo(a, "C++", 							new String[] { "c" });
		assertAnalyzesTo(a, "B2B", 							new String[] { "b", "b" });
		assertAnalyzesTo(a, "2B", 							new String[] { "b" });
		assertAnalyzesTo(a, "\"QUOTED\" word", 				new String[] { "quoted", "word" });
	}	
	
	
	@Test
	public void testNull() throws Exception {
		Analyzer a = new WhitespaceAnalyzer();
		assertAnalyzesTo(a, "foo bar FOO BAR", 				new String[] { "foo", "bar", "FOO", "BAR" });
		assertAnalyzesTo(a, "foo      bar .  FOO <> BAR", 	new String[] { "foo", "bar", ".", "FOO", "<>", "BAR" });
		assertAnalyzesTo(a, "foo.bar.FOO.BAR", 				new String[] { "foo.bar.FOO.BAR" });
		assertAnalyzesTo(a, "U.S.A.", 						new String[] { "U.S.A." });
		assertAnalyzesTo(a, "C++", 							new String[] { "C++" });
		assertAnalyzesTo(a, "B2B", 							new String[] { "B2B" });
		assertAnalyzesTo(a, "2B", 							new String[] { "2B" });
		assertAnalyzesTo(a, "\"QUOTED\" word", 				new String[] { "\"QUOTED\"", "word" });
	}
	
	
	@Test
	public void testStop() throws Exception {
		Analyzer a = new StopAnalyzer();
		assertAnalyzesTo(a, "foo bar FOO BAR", 				new String[] { "foo", "bar", "foo", "bar" });
		assertAnalyzesTo(a, "foo a bar such FOO THESE BAR", new String[] { "foo", "bar", "foo", "bar" });
	}
	


	
	
	private void verifyPayload(TokenStream ts) throws IOException {
		PayloadAttribute payloadAtt = ts.getAttribute(PayloadAttribute.class);
		ts.reset();
		for (byte b = 1;; b++) {
			boolean hasNext = ts.incrementToken();
			if (!hasNext)
				break;
			// System.out.println("id="+System.identityHashCode(nextToken) + " "
			// + t);
			// System.out.println("payload=" +
			// (int)nextToken.getPayload().toByteArray()[0]);
			assertEquals(b, payloadAtt.getPayload().bytes[0]);
		}
	}

	// Make sure old style next() calls result in a new copy of payloads
	@Test
	public void testPayloadCopy() throws IOException {
		String s = "how now brown cow";
		TokenStream ts;
		ts = new WhitespaceTokenizer(new StringReader(s));
		ts = new PayloadSetter(ts);
		verifyPayload(ts);

		ts = new WhitespaceTokenizer(new StringReader(s));
		ts = new PayloadSetter(ts);
		verifyPayload(ts);
	}
	
	
	

	// LUCENE-1150: Just a compile time test, to ensure the
	// StandardAnalyzer constants remain publicly accessible
	@SuppressWarnings("unused")
	public void _testStandardConstants() {
		int x = StandardTokenizer.ALPHANUM;
		x = StandardTokenizer.APOSTROPHE;
		x = StandardTokenizer.ACRONYM;
		x = StandardTokenizer.COMPANY;
		x = StandardTokenizer.EMAIL;
		x = StandardTokenizer.HOST;
		x = StandardTokenizer.NUM;
		x = StandardTokenizer.CJ;
		String[] y = StandardTokenizer.TOKEN_TYPES;
	}

	private static class LowerCaseWhitespaceAnalyzer extends Analyzer {

		@Override
		public TokenStreamComponents createComponents(String fieldName, Reader reader) {
			Tokenizer tokenizer = new WhitespaceTokenizer(reader);
			return new TokenStreamComponents(tokenizer, new LowerCaseFilter(tokenizer));
		}

	}

	private static class UpperCaseWhitespaceAnalyzer extends Analyzer {

		@Override
		public TokenStreamComponents createComponents(String fieldName, Reader reader) {
			Tokenizer tokenizer = new WhitespaceTokenizer(reader);
			return new TokenStreamComponents(tokenizer, new UpperCaseFilter(tokenizer));
		}

	}

	/**
	 * Test that LowercaseFilter handles entire unicode range correctly
	 */
	@Test
	public void testLowerCaseFilter() throws IOException {
		Analyzer a = new LowerCaseWhitespaceAnalyzer();
		
		// BMP
		assertAnalyzesTo(a, "AbaCaDabA", new String[] { "abacadaba" });
		
		// supplementary
		assertAnalyzesTo(a, "\ud801\udc16\ud801\udc16\ud801\udc16\ud801\udc16",
				new String[] { "\ud801\udc3e\ud801\udc3e\ud801\udc3e\ud801\udc3e" });
		
		assertAnalyzesTo(a, "AbaCa\ud801\udc16DabA", new String[] { "abaca\ud801\udc3edaba" });
		
		// unpaired lead surrogate
		assertAnalyzesTo(a, "AbaC\uD801AdaBa", new String[] { "abac\uD801adaba" });
		
		// unpaired trail surrogate
		assertAnalyzesTo(a, "AbaC\uDC16AdaBa", new String[] { "abac\uDC16adaba" });
	}

	/**
	 * Test that LowercaseFilter handles entire unicode range correctly
	 */
	@Test
	public void testUpperCaseFilter() throws IOException {
		Analyzer a = new UpperCaseWhitespaceAnalyzer();
		
		// BMP
		assertAnalyzesTo(a, "AbaCaDabA", new String[] { "ABACADABA" });
		
		// supplementary
		assertAnalyzesTo(a, "\ud801\udc3e\ud801\udc3e\ud801\udc3e\ud801\udc3e",
				new String[] { "\ud801\udc16\ud801\udc16\ud801\udc16\ud801\udc16" });
		
		assertAnalyzesTo(a, "AbaCa\ud801\udc3eDabA", new String[] { "ABACA\ud801\udc16DABA" });
		
		// unpaired lead surrogate
		assertAnalyzesTo(a, "AbaC\uD801AdaBa", new String[] { "ABAC\uD801ADABA" });
		
		// unpaired trail surrogate
		assertAnalyzesTo(a, "AbaC\uDC16AdaBa", new String[] { "ABAC\uDC16ADABA" });
	}

}



final class PayloadSetter extends TokenFilter {
	PayloadAttribute payloadAtt;

	public PayloadSetter(TokenStream input) {
		super(input);
		payloadAtt = addAttribute(PayloadAttribute.class);
	}

	byte[] data = new byte[1];
	BytesRef p = new BytesRef(data, 0, 1);

	@Override
	public boolean incrementToken() throws IOException {
		boolean hasNext = input.incrementToken();
		if (!hasNext)
			return false;
		payloadAtt.setPayload(p); // reuse the payload / byte[]
		data[0]++;
		return true;
	}
	
}