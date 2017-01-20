package org.apache.lucene.lucenetutorial;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

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
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * Text File 색인
 * 
 * http://www.lucenetutorial.com/sample-apps/textfileindexer-java.html
 * 
 * @author NAVER
 *
 */
public class TextFileIndexer {

	private static StandardAnalyzer analyzer = new StandardAnalyzer();
	
	private IndexWriter writer;
	private ArrayList<File> queue = new ArrayList<>();
	
	
	
	public static void main(String[] args) throws IOException, ParseException {
		
		// Indexer 객체를 초기화한다.
		String indexLocation = "C:\\tmp_index";
		TextFileIndexer indexer = new TextFileIndexer(indexLocation);
		
		// 색인할 파일을 지정한다.
		indexer.addFile("C:\\tmp\\spring.log");
		
		// 색인 작업을 시작한다.
		indexer.indexFile();
		indexer.closeIndex();
		
		// 검색
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexLocation)));
		IndexSearcher searcher = new IndexSearcher(reader);
		
		TopScoreDocCollector collector = TopScoreDocCollector.create(5, true);
		
		Query q = new QueryParser("contents", analyzer).parse("context");
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		
		// 결과 출력
		System.out.println("Found " + hits.length + " hits.");
		for (int i=0; i<hits.length; i++) {
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);
			System.out.println((i+1) + ". " + d.get("path") + " score=" + hits[i].score);
		}
	}
	
	
	/**
	 * 생성자에서 입력받은 파일을 오픈하여 IndexWriter를 생성한다.
	 * 
	 * @param indexDir
	 * @throws IOException
	 */
	public TextFileIndexer(String indexDir) throws IOException {
		FSDirectory dir = FSDirectory.open(new File(indexDir));
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, analyzer);
		
		writer = new IndexWriter(dir, config);
	}
	
	
	/**
	 * 색인 대상 파일을 추가한다.
	 * @param fileName
	 */
	public void addFile(String fileName) {
		queue.add(new File(fileName));
	}
	
	
	/**
	 * 큐에 등록된 모든 파일을 색인한다.
	 * @throws IOException
	 */
	public void indexFile() throws IOException {
		for (File f : queue) {
			FileReader fr = new FileReader(f);
			
			Document doc = new Document();
			doc.add(new TextField("contents", fr));
			doc.add(new StringField("path", f.getPath(), Field.Store.YES));
			doc.add(new StringField("filename", f.getName(), Field.Store.YES));
			
			writer.addDocument(doc);
			System.out.println("Added : " + f);
			
			fr.close();
		}
	}
	
	
	public void closeIndex() throws IOException {
		writer.close();
	}
	
	
}











