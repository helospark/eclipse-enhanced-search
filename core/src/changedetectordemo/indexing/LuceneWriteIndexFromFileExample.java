package changedetectordemo.indexing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.helospark.lightdi.annotation.Component;

import changedetectordemo.handlers.MyListener.LuceneRequest;

@Component
public class LuceneWriteIndexFromFileExample {
    private static final String PATH = "/tmp/lucene/";

    private static final String CONTENTS_FIELD = "contents";
    private UniqueNameCalculator uniqueNameCalculator;

    public LuceneWriteIndexFromFileExample(UniqueNameCalculator uniqueNameCalculator) {
        this.uniqueNameCalculator = uniqueNameCalculator;
    }

    public IndexWriter indexContentTo(String indexLocation) {
        try {
            // org.apache.lucene.store.Directory instance
            Directory dir = FSDirectory.open(Paths.get(PATH + indexLocation));

            // analyzer with the default stop words
            Analyzer analyzer = new StandardAnalyzer();

            // IndexWriter Configuration
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);

            // IndexWriter writes new index files to the directory
            return new IndexWriter(dir, iwc);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addToIndex(IndexWriter writer, IndexEntity entity) throws IOException {
        Document doc = new Document();

        String fullPath = entity.path;
        int slashPosition = fullPath.lastIndexOf("/");
        String fileName = slashPosition == -1 ? fullPath : fullPath.substring(slashPosition + 1);

        doc.add(new StringField("path", fullPath, Field.Store.YES));
        doc.add(new StringField("filename", fileName, Field.Store.YES));
        doc.add(new TextField(CONTENTS_FIELD, entity.data, Store.YES));
        doc.add(new TextField("jarName", entity.jarName, Store.YES));

        writer.updateDocument(new Term("path", fullPath), doc);
    }

    public boolean hasIndex(String uniqueId) {
        return new File(PATH + uniqueId).exists();
    }

    private IndexSearcher searcher = null;
    private IndexReader reader;

    public void search(List<LuceneRequest> files, String content) throws IOException, InvalidTokenOffsetsException, ParseException {
        // analyzer with the default stop words
        Analyzer analyzer = new StandardAnalyzer();

        if (searcher == null) {
            searcher = createSearcer(files);
        }

        QueryParser qp = new QueryParser(CONTENTS_FIELD, analyzer);
        Query query = qp.parse(content);
        TopDocs hits = searcher.search(query, 10);
        Formatter formatter = new SimpleHTMLFormatter();
        QueryScorer scorer = new QueryScorer(query);
        Highlighter highlighter = new Highlighter(formatter, scorer);
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 60);
        highlighter.setTextFragmenter(fragmenter);

        for (int i = 0; i < hits.scoreDocs.length; i++) {
            int docid = hits.scoreDocs[i].doc;
            Document doc = searcher.doc(docid);
            String title = doc.get("path");

            System.out.println("Path " + " : " + title);

            String text = doc.get("contents");

            TokenStream stream = TokenSources.getAnyTokenStream(reader, docid, "contents", analyzer);

            String[] frags = highlighter.getBestFragments(stream, text, 10);
            for (String frag : frags) {
                System.out.println("=======================");
                System.out.println(frag);
            }
        }
    }

    private IndexSearcher createSearcer(List<LuceneRequest> files) throws IOException {
        List<IndexReader> indicesToUse = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (LuceneRequest file : files) {
            indicesToUse.add(DirectoryReader.open(FSDirectory.open(Paths.get(PATH + file.uniqueId))));
        }
        System.out.println("Name generator " + (System.currentTimeMillis() - start));

        // Index reader - an interface for accessing a point-in-time view of a lucene
        // index
        reader = new MultiReader(indicesToUse.toArray(new IndexReader[indicesToUse.size()]));

        // Create lucene searcher. It search over a single IndexReader.
        return new IndexSearcher(reader);
    }

    public List<LuceneSearchResult> findFile(List<LuceneRequest> request, String content) {
        try {
            Analyzer analyzer = new WhitespaceAnalyzer();

            searcher = createSearcer(request);

            QueryParser qp = new QueryParser("filename", analyzer);
            qp.setAllowLeadingWildcard(true);
            Query query = qp.parse(content);
            TopDocs hits = searcher.search(query, 100);

            List<LuceneSearchResult> fullyQualifiedNames = new ArrayList<>();

            for (int i = 0; i < hits.scoreDocs.length; i++) {
                int docid = hits.scoreDocs[i].doc;
                Document doc = searcher.doc(docid);
                String title = doc.get("path");
                String jarName = doc.get("jarName");

                System.out.println(title);
                fullyQualifiedNames.add(new LuceneSearchResult(jarName, title));
            }
            return fullyQualifiedNames;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class LuceneSearchResult {
        public String jarName;
        public String fullyQualifiedName;

        public LuceneSearchResult(String jarName, String fullyQualifiedName) {
            this.jarName = jarName;
            this.fullyQualifiedName = fullyQualifiedName;
        }

    }
}
