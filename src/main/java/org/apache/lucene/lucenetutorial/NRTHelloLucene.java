package org.apache.lucene.lucenetutorial;

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TrackingIndexWriter;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

/**
 * Lucene NRT (NearRealtimeSearch)
 * 
 * http://www.lucenetutorial.com/lucene-nrt-hello-world.html
 * 
 * @author NAVER
 *
 */
public class NRTHelloLucene {

	/**
	 * 색인 작업과 검색 작업이 동시에 수행된다.
	 * 
	 * IndexWriter의 작업이 지속적으로 일어나고 있는 상황에서 
	 * IndexSearcher의 검색 작업이 동시에 수행된다.
	 * 최근에 색인된 데이터는 검색 결과에 근실시간으로 반영된다.
	 *  
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		
		/**
		 * ControlledRealTimeReopenThread 초기화
		 */
		
		// 분석기 생성
		StandardAnalyzer analyzer = new StandardAnalyzer();
		
		// 분석기를 이용하여 index 객체 생성
		RAMDirectory index = new RAMDirectory();
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, analyzer);
		
		final IndexWriter indexWriter = new IndexWriter(index, config);
		
		
		// ControlledRealTimeReopenThread 스레드를 BackGround로 실행한다.
		// 다른 스레드에서는 searcherManager를 이용하여 통신한다.
		TrackingIndexWriter trackingIndexWriter = new TrackingIndexWriter(indexWriter);
		final ReferenceManager<IndexSearcher> searcherManager = new SearcherManager(indexWriter, true, null);
				
		ControlledRealTimeReopenThread<IndexSearcher> nrtReopenThread = new ControlledRealTimeReopenThread<>(trackingIndexWriter, searcherManager, 1.0, 0.1);
		nrtReopenThread.setName("NRT Reopen Thread");
		nrtReopenThread.setPriority(Math.min(Thread.currentThread().getPriority()+2, Thread.MAX_PRIORITY));
		nrtReopenThread.setDaemon(true);
		nrtReopenThread.start();
		
		
		/**
		 * 색인을 위한 스레드를 생성한다.
		 */
		Thread writerThread = new Thread() {
			@Override
			public void run() {
				try {
					// 지속적으로 색인 작업을 수행한다.
					for (int i=0; i<100; i++) {
						Document doc = new Document();
						doc.add(new LongField("time", System.currentTimeMillis(), Field.Store.YES));
						doc.add(new StringField("counter", ""+i, Field.Store.YES));
						
						indexWriter.addDocument(doc);
						
						// 문서가 추가되면 searcherManager를 리프레시한다.
						searcherManager.maybeRefresh();
						
						if (i%10 == 0) {
							System.out.println("[Index Thread] Indexing " + i + " ...");
						}
						Thread.sleep(100);
					}
					indexWriter.commit();
					
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		};		
		writerThread.start();
		
		
		/**
		 * 색인된 결과가 근실시간으로 검색되는지 검사한다.
		 */
		IndexSearcher searcher = null;
		Query q = null;
		TopDocs docs = null;
		String querystr = "";
		
		// 1번이 존재하는 검사
		Thread.sleep(2000);
		querystr = "1";

		searcher = searcherManager.acquire();
		q = new TermQuery(new Term("counter", querystr));
		docs = searcher.search(q, 10);
		
		System.out.println("[Search Thread] Found " + docs.totalHits + " docs for counter=" + querystr);
		
		// 58번이 존재하는 검사
		Thread.sleep(1000);
		querystr = "58";

		searcher = searcherManager.acquire();
		q = new TermQuery(new Term("counter", querystr));
		docs = searcher.search(q, 10);
		
		System.out.println("[Search Thread] Found " + docs.totalHits + " docs for counter=" + querystr);

		// 58번이 존재하는 검사
		Thread.sleep(4000);
		querystr = "58";

		searcher = searcherManager.acquire();
		q = new TermQuery(new Term("counter", querystr));
		docs = searcher.search(q, 10);
		
		System.out.println("[Search Thread] Found " + docs.totalHits + " docs for counter=" + querystr);

		
		
		searcherManager.release(searcher);
	}
	
	
}
