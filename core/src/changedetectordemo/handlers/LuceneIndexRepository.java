package changedetectordemo.handlers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.lucene.index.IndexReader;

import changedetectordemo.indexing.UniqueNameCalculator;

public class LuceneIndexRepository {
    private Map<String, IndexReader> jarPathToIndex = new HashMap<>();
    private UniqueNameCalculator uniqueNameCalculator;

    public LuceneIndexRepository(UniqueNameCalculator uniqueNameCalculator) {
        this.uniqueNameCalculator = uniqueNameCalculator;
    }

    public void addIndexerForId(String id, IndexReader entry) {
        jarPathToIndex.put(id, entry);
    }

    public Optional<IndexReader> getReaderForJar(File file) {
        String path = uniqueNameCalculator.indexPathCalculator(file);
        return Optional.ofNullable(jarPathToIndex.get(path));
    }
}
