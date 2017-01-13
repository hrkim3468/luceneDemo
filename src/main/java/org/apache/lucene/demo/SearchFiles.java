package org.apache.lucene.demo;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/** 
 * Simple command-line based search demo. 
 */
public class SearchFiles {

	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
		
		String usage = "Usage:\tjava org.apache.lucene.demo.SearchFiles "
				+ "[-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\n"
				+ "See http://lucene.apache.org/core/4_1_0/demo/ for details.";
		
		if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
			System.out.println(usage);
			System.exit(0);
		}

		// 검색을 위한 쿼리 정보를 구한다.
		String index = "index";
		String field = "contents";
		String queries = null;
		String queryString = null;

		int repeat = 0;
		boolean raw = false;
		int hitsPerPage = 10;

		for (int i = 0; i < args.length; i++) {
			if ("-index".equals(args[i])) {
				index = args[i + 1];
				i++;
			} else if ("-field".equals(args[i])) {
				field = args[i + 1];
				i++;
			} else if ("-queries".equals(args[i])) {
				queries = args[i + 1];
				i++;
			} else if ("-query".equals(args[i])) {
				queryString = args[i + 1];
				i++;
			} else if ("-repeat".equals(args[i])) {
				repeat = Integer.parseInt(args[i + 1]);
				i++;
			} else if ("-raw".equals(args[i])) {
				raw = true;
			} else if ("-paging".equals(args[i])) {
				hitsPerPage = Integer.parseInt(args[i + 1]);
				if (hitsPerPage <= 0) {
					System.err.println("There must be at least 1 hit per page.");
					System.exit(1);
				}
				i++;
			}
		}

		// 디렉토리 오픈
		FSDirectory dir = FSDirectory.open(new File(index));
		
		// 기본분석기 생성
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_10_0);

		// Searcher 생성
		IndexReader reader = DirectoryReader.open(dir);
		IndexSearcher searcher = new IndexSearcher(reader);
		

		// 쿼리 문자열을 조회 할 InputStream을 생성한다.
		BufferedReader in = null;
		if (queries != null) {
			in = new BufferedReader(
					new InputStreamReader(new FileInputStream(queries), StandardCharsets.UTF_8));
		} else {
			in = new BufferedReader(
					new InputStreamReader(System.in, StandardCharsets.UTF_8));
		}
		
		// 쿼리파서를 생성한다.
		QueryParser parser = new QueryParser(Version.LUCENE_4_10_0, field, analyzer);
		
		while (true) {
			if (queries == null && queryString == null) { // prompt the user
				System.out.println("Enter query: ");
			}

			// 쿼리 문자열을 조회한다.
			String line = queryString != null ? queryString : in.readLine();

			if (line == null || line.length() == -1) {
				break;
			}

			line = line.trim();
			if (line.length() == 0) {
				break;
			}

			// 쿼리 문자열을 파싱하여 쿼리를 생성한다.
			Query query = parser.parse(line);
			System.out.println("Searching for: " + query.toString(field));

			// 검색작업 실행 (반복 실행 옵션)
			if (repeat > 0) {
				Date start = new Date();
				for (int i = 0; i < repeat; i++) {
					searcher.search(query, null, 100);
				}
				Date end = new Date();
				System.out.println("Time: " + (end.getTime() - start.getTime()) + "ms");
			}

			// 검색작업 실행 (페이징)
			doPagingSearch(in, searcher, query, hitsPerPage, raw, queries == null && queryString == null);

			if (queryString != null) {
				break;
			}
		}
		reader.close();
	}


	/**
	 * 검색작업을 수행한다.
	 * 
	 * @param in
	 * @param searcher
	 * @param query
	 * @param hitsPerPage
	 * @param raw
	 * @param interactive
	 * @throws IOException
	 */
	public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, 
			Query query, int hitsPerPage, boolean raw, boolean interactive) throws IOException {

		// 5페이지에 해당하는 정보만 조회한다.
		TopDocs results = searcher.search(query, 5 * hitsPerPage);
		
		// 결과 Score 정보
		ScoreDoc[] hits = results.scoreDocs;

		// 결과 건수
		int numTotalHits = results.totalHits;
		System.out.println(numTotalHits + " total matching documents");

		int start = 0;
		int end = Math.min(numTotalHits, hitsPerPage);

		while (true) {
			if (end > hits.length) {
				System.out.println("Only results 1 - " + hits.length + " of " + numTotalHits
								+ " total matching documents collected.");
				System.out.println("Collect more (y/n) ?");
				String line = in.readLine();
				if (line.length() == 0 || line.charAt(0) == 'n') {
					break;
				}

				hits = searcher.search(query, numTotalHits).scoreDocs;
			}

			end = Math.min(hits.length, start + hitsPerPage);

			// 결과 문서를 출력한다.
			for (int i = start; i < end; i++) {
				if (raw) {
					System.out.println("doc=" + hits[i].doc + " score=" + hits[i].score);
					continue;
				}

				// Document 객체 조회
				Document doc = searcher.doc(hits[i].doc);
				String path = doc.get("path");
				if (path != null) {
					System.out.println((i + 1) + ". " + path);
					String title = doc.get("title");
					if (title != null) {
						System.out.println("   Title: " + doc.get("title"));
					}
				} else {
					System.out.println((i + 1) + ". " + "No path for this document");
				}
			}

			if (!interactive || end == 0) {
				break;
			}

			if (numTotalHits >= end) {
				boolean quit = false;
				while (true) {
					System.out.print("Press ");
					if (start - hitsPerPage >= 0) {
						System.out.print("(p)revious page, ");
					}
					if (start + hitsPerPage < numTotalHits) {
						System.out.print("(n)ext page, ");
					}
					System.out.println("(q)uit or enter number to jump to a page.");

					String line = in.readLine();
					if (line.length() == 0 || line.charAt(0) == 'q') {
						quit = true;
						break;
					}
					if (line.charAt(0) == 'p') {
						start = Math.max(0, start - hitsPerPage);
						break;
					} else if (line.charAt(0) == 'n') {
						if (start + hitsPerPage < numTotalHits) {
							start += hitsPerPage;
						}
						break;
					} else {
						int page = Integer.parseInt(line);
						if ((page - 1) * hitsPerPage < numTotalHits) {
							start = (page - 1) * hitsPerPage;
							break;
						} else {
							System.out.println("No such page");
						}
					}
				}
				if (quit) {
					break;
				}
				end = Math.min(numTotalHits, start + hitsPerPage);
			}
		}
	}
	

	
	
	
}



