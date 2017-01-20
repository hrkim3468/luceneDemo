package org.apache.lucene.lucenetutorial;

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

/**
 * Lucene Sample 코드
 * 
 * http://www.lucenetutorial.com/lucene-in-5-minutes.html
 * 
 * @author NAVER
 *
 */
public class HelloLucene {

	/**
	 * 색인 작업이 모두 완료된 후 검색 작업을 수행한다.
	 * 
	 * IndexWriter의 작업이 모두 완료되고 Close 상태가 되어야 
	 * 비로소 IndexSearcher로 검색이 가능해진다.
	 *  
	 * @param args
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws IOException, ParseException {
		
		/**
		 * 색인 작업
		 */
		
		// 1. 분석기 생성
		StandardAnalyzer analyzer = new StandardAnalyzer();
		
		// 2. 분석기를 이용하여 index 객체 생성
		Directory index = new RAMDirectory();
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, analyzer);
		
		// 3. 색인 시작 (완료시 IndexWriter를 Close해야 한다.)
		IndexWriter w = new IndexWriter(index, config);
		addDoc(w, "Lucene in Action", "1111");
		addDoc(w, "Lucene for Dummies", "2222");
		addDoc(w, "Managing Gigabytes", "3333");
		addDoc(w, "The Art of Computer Science", "4444");
		
		w.close();
		
		
		
		/**
		 * 검색 작업
		 */
		
		// 1. 제목을 검색하는 Query 객체 생성
		String querystr = args.length>0 ? args[0] : "lucene";
		Query q = new QueryParser("title", analyzer).parse(querystr);
		
		// 2. Query 객체를 이용하여 검색
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		
		int hitsPerPage = 10;
		TopDocs docs = searcher.search(q, hitsPerPage);
		ScoreDoc[] hits = docs.scoreDocs;
		
		// 3. 결과 출력
		System.out.println("Found " + hits.length + " hits.");
		for (int i=0; i<hits.length; ++i) {
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);
			System.out.println((i+1) + ". " + d.get("isbn") + "\t" + d.get("title"));
		}
		
		reader.close();
	}
	
	
	private static void addDoc(IndexWriter w, String title, String isbn) throws IOException {
		
		// 문서 객체 생성
		Document doc = new Document();
		// 필드 정의 : 분석 필드 (색인, 원문저장)
		doc.add(new TextField("title", title, Field.Store.YES));
		// 필드 정의 : 문자열 필드 (미색인, 원문저장)
		doc.add(new StringField("isbn", isbn, Field.Store.YES));
		
		// 문서 저장
		w.addDocument(doc);
	}
	
	
}
