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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing. Run
 * it with no command-line arguments for usage information.
 */
public class IndexFiles {


	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		
		String usage = "java org.apache.lucene.demo.IndexFiles"
				+ " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
				+ "This indexes the documents in DOCS_PATH, creating a Lucene index"
				+ "in INDEX_PATH that can be searched with SearchFiles";
		
		String indexPath = "index";
		String docsPath = null;
		boolean create = true;
		
		for (int i = 0; i < args.length; i++) {
			if ("-index".equals(args[i])) {
				indexPath = args[i + 1];
				i++;
			} else if ("-docs".equals(args[i])) {
				docsPath = args[i + 1];
				i++;
			} else if ("-update".equals(args[i])) {
				create = false;
			}
		}

		if (docsPath == null) {
			System.err.println("Usage: " + usage);
			System.exit(1);
		}

		final File docDir = new File(docsPath);
		if (!docDir.exists() || !docDir.canRead()) {
			System.out.println("Document directory '"
							+ docDir.getAbsolutePath()
							+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}

		Date start = new Date();
		try {
			System.out.println("Indexing to directory '" + indexPath + "'...");

			// 디렉토리 오픈
			Directory dir = FSDirectory.open(new File(indexPath));
			
			// 기본분석기 생성
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_10_0);
			
			// Writer 환경설정
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_4_10_0, analyzer);
			if (create) {
				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}

			// Writer 생성
			IndexWriter writer = new IndexWriter(dir, iwc);
			
			// 색인작업 실행
			indexDocs(writer, docDir);

			// Writer 종료
			writer.close();

			Date end = new Date();
			System.out.println(end.getTime() - start.getTime() + " total milliseconds");

		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}
	}


	/**
	 * 색인작업을 수행한다.
	 * 
	 * @param writer
	 * @param file
	 * @throws IOException
	 */
	static void indexDocs(IndexWriter writer, File file) throws IOException {
		if (!file.canRead()) {
			return;
		}
		
		if (file.isDirectory()) {
			/**
			 * 디렉토리일 경우 파일이 나타날때까지 Recursive로 처리한다.
			 */
			String[] files = file.list();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					indexDocs(writer, new File(file, files[i]));
				}
			}
			
		} else {
			/**
			 * 파일을 읽어서 색인을 생성한다.
			 */
			FileInputStream fis;
			try {
				fis = new FileInputStream(file);
			} catch (FileNotFoundException fnfe) {
				return;
			}

			try {
				// Document 객체 생성
				Document doc = new Document();

				doc.add(new StringField("path", file.getPath(), Field.Store.YES));
				doc.add(new LongField("modified", file.lastModified(), Field.Store.NO));
				doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))));

				if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
					System.out.println("adding " + file);
					writer.addDocument(doc);
				} else {
					System.out.println("updating " + file);
					writer.updateDocument(new Term("path", file.getPath()), doc);
				}

			} finally {
				fis.close();
			}
		}
	}
	
	
	
	
	
}



