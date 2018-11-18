package changedetectordemo.indexing;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Generated;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.helospark.lightdi.annotation.Component;

@Component
public class LuceneWriteIndexFromFileExample {

    private static final String FILEPATH_FIELD = "path";
    private static final String FILENAME_FIELD = "filename";
    private static final String CONTENTS_FIELD = "contents";

    private static final String FILEPATH_FIELD_CASE_INSENSITIVE = "path_insensitive";
    private static final String FILENAME_FIELD_CASE_INSENSITIVE = "filename_insensitive";
    private static final String CONTENTS_FIELD_CASE_INSENSITIVE = "contents_insensitive";
    private UniqueNameCalculator uniqueNameCalculator;

    public LuceneWriteIndexFromFileExample(UniqueNameCalculator uniqueNameCalculator) {
        this.uniqueNameCalculator = uniqueNameCalculator;
    }

    public IndexWriter indexContentTo(String indexLocation) {
        try {
            // org.apache.lucene.store.Directory instance
            Directory dir = FSDirectory.open(Paths.get(indexLocation));

            // analyzer with the default stop words
            Analyzer analyzer = new WhitespaceAnalyzer();

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

        doc.add(new Field(FILEPATH_FIELD, fullPath, createStoredAndIndexedField()));
        doc.add(new Field(FILENAME_FIELD, fileName, createStoredAndIndexedField()));
        doc.add(new Field(CONTENTS_FIELD, entity.data, createStoredAndIndexedField()));

        doc.add(new Field(FILEPATH_FIELD_CASE_INSENSITIVE, fullPath.toLowerCase(), createStoredAndIndexedField()));
        doc.add(new Field(FILENAME_FIELD_CASE_INSENSITIVE, fileName.toLowerCase(), createStoredAndIndexedField()));
        doc.add(new Field(CONTENTS_FIELD_CASE_INSENSITIVE, entity.data.toLowerCase(), createStoredAndIndexedField()));

        doc.add(new Field("indexPath", entity.indexPath, createStoredAndIndexedField()));
        doc.add(new Field("jarPath", entity.jarPath, createStoredAndIndexedField()));

        writer.updateDocument(new Term(FILEPATH_FIELD, fullPath), doc);

    }

    private FieldType createStoredAndIndexedField() {
        FieldType fieldType = new FieldType();
        fieldType.setTokenized(true);
        fieldType.setStored(true);
        fieldType.setStoreTermVectors(true);
        fieldType.setStoreTermVectorPositions(true);
        fieldType.setStoreTermVectorOffsets(true);
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        return fieldType;
    }

    private IndexSearcher createSearcer(IndexReader multiReader) throws IOException {
        return new IndexSearcher(multiReader);
    }

    public LuceneSearchResultRoot findFile(SearchRequest request) {
        try {
            Analyzer analyzer = new WhitespaceAnalyzer();
            IndexSearcher searcher = createSearcer(request.getMultiReader());

            String[] fields = createRequest(request);

            String searchString = request.getContent();
            if (request.caseSensitive) {
                searchString.toLowerCase();
            }

            if (fields.length == 0) {
                return new LuceneSearchResultRoot(Collections.emptyList());
            }

            MultiFieldQueryParser multiFieldQuers = new MultiFieldQueryParser(fields, analyzer);
            multiFieldQuers.setAllowLeadingWildcard(true);
            Query query = multiFieldQuers.parse(searchString);
            TopDocs hits = searcher.search(query, 10);

            Formatter formatter = new SimpleHTMLFormatter();
            QueryScorer scorer = new QueryScorer(query);
            Highlighter highlighter = new Highlighter(formatter, scorer);
            SimpleSpanFragmenter fragmenter = new SimpleSpanFragmenter(scorer, 30);
            highlighter.setTextFragmenter(fragmenter);

            List<LuceneSearchResult> fullyQualifiedNames = new ArrayList<>();

            for (int i = 0; i < hits.scoreDocs.length; i++) {
                int docid = hits.scoreDocs[i].doc;
                Document doc = searcher.doc(docid);
                String title = doc.get(FILEPATH_FIELD);
                String indexPath = doc.get("indexPath");
                String jarName = doc.get("jarPath");

                LuceneSearchResult result = new LuceneSearchResult(indexPath, title, jarName);

                if (request.isIncludeContent()) {
                    String content = doc.get(CONTENTS_FIELD);

                    Fields termVector = request.getMultiReader().getTermVectors(docid);
                    TokenStream stream = TokenSources.getTermVectorTokenStreamOrNull(CONTENTS_FIELD, termVector, -1);

                    if (!request.caseSensitive) {
                        stream = new LowerCaseFilter(stream);
                    }

                    String[] frags = highlighter.getBestFragments(stream, content, 10);
                    for (String frag : frags) {
                        String line = frag.replace("\n", " ").trim();
                        result.addTextSearchResultFragment(new TextSearchResult(line, result));
                    }

                }

                fullyQualifiedNames.add(result);
            }
            return new LuceneSearchResultRoot(fullyQualifiedNames);
        } catch (

        Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String[] createRequest(SearchRequest request) {
        List<String> result = new ArrayList<>();
        if (request.isIncludeContent()) {
            if (request.caseSensitive) {
                result.add(CONTENTS_FIELD);
            } else {
                result.add(CONTENTS_FIELD_CASE_INSENSITIVE);
            }
        }
        if (request.isIncludeFileName()) {
            if (request.caseSensitive) {
                result.add(FILENAME_FIELD);
            } else {
                result.add(FILENAME_FIELD_CASE_INSENSITIVE);
            }
        }
        if (request.isIncludeFilePath()) {
            if (request.caseSensitive) {
                result.add(FILEPATH_FIELD);
            } else {
                result.add(FILEPATH_FIELD_CASE_INSENSITIVE);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public static class LuceneSearchResultRoot {
        public List<LuceneSearchResult> results;

        public LuceneSearchResultRoot(List<LuceneSearchResult> results) {
            this.results = results;
        }

    }

    public static class SearchRequest {
        IndexReader multiReader;
        String content;
        boolean includeFileName;
        boolean includeFilePath;
        boolean includeContent;
        boolean caseSensitive;

        @Generated("SparkTools")
        private SearchRequest(Builder builder) {
            this.multiReader = builder.multiReader;
            this.content = builder.content;
            this.includeFileName = builder.includeFileName;
            this.includeFilePath = builder.includeFilePath;
            this.includeContent = builder.includeContent;
            this.caseSensitive = builder.caseSensitive;
        }

        public IndexReader getMultiReader() {
            return multiReader;
        }

        public String getContent() {
            return content;
        }

        public boolean isIncludeFileName() {
            return includeFileName;
        }

        public boolean isIncludeFilePath() {
            return includeFilePath;
        }

        public boolean isIncludeContent() {
            return includeContent;
        }

        public boolean isCaseSensitive() {
            return caseSensitive;
        }

        @Generated("SparkTools")
        public static Builder builder() {
            return new Builder();
        }

        @Generated("SparkTools")
        public static final class Builder {
            private IndexReader multiReader;
            private String content;
            private boolean includeFileName;
            private boolean includeFilePath;
            private boolean includeContent;
            private boolean caseSensitive;

            private Builder() {
            }

            public Builder withMultiReader(IndexReader multiReader) {
                this.multiReader = multiReader;
                return this;
            }

            public Builder withContent(String content) {
                this.content = content;
                return this;
            }

            public Builder withIncludeFileName(boolean includeFileName) {
                this.includeFileName = includeFileName;
                return this;
            }

            public Builder withIncludeFilePath(boolean includeFilePath) {
                this.includeFilePath = includeFilePath;
                return this;
            }

            public Builder withIncludeContent(boolean includeContent) {
                this.includeContent = includeContent;
                return this;
            }

            public Builder withCaseSensitive(boolean caseSensitive) {
                this.caseSensitive = caseSensitive;
                return this;
            }

            public SearchRequest build() {
                return new SearchRequest(this);
            }
        }

    }

    public static class LuceneSearchResult {
        public String indexPath;
        public String fullyQualifiedName;
        public String jarPath;
        public List<TextSearchResult> textSearchResults;

        public LuceneSearchResult(String indexPath, String fullyQualifiedName, String jarName2) {
            this.indexPath = indexPath;
            this.fullyQualifiedName = fullyQualifiedName;
            this.jarPath = jarName2;
            textSearchResults = new ArrayList<>();
        }

        public void addTextSearchResultFragment(TextSearchResult textSearchResult) {
            textSearchResults.add(textSearchResult);
        }

    }

    public static class TextSearchResult {
        public String foundLine;
        public int documentPositionStart;
        public int documentPositionEnd;
        public int lineStart;
        public int lineEnd;

        public LuceneSearchResult parent;

        public TextSearchResult(String foundLine, LuceneSearchResult parent) {
            this.foundLine = foundLine;
            this.parent = parent;
        }

    }
}
