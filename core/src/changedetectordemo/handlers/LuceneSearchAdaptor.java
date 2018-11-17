package changedetectordemo.handlers;

import changedetectordemo.indexing.JarFileIndexer;
import changedetectordemo.indexing.LuceneWriteIndexFromFileExample;

@SuppressWarnings("restriction")
public class LuceneSearchAdaptor {
    private JarFileIndexer indexer;
    private LuceneWriteIndexFromFileExample luceneAdaptor;
//
//    private List<LuceneRequest> path = new ArrayList<>();
//    private Map<String, List<JarPackageFragmentRoot>> map = new HashMap<>();
//
//    public LuceneSearchAdaptor(JarFileIndexer indexer, LuceneWriteIndexFromFileExample luceneAdaptor) {
//        this.indexer = indexer;
//        this.luceneAdaptor = luceneAdaptor;
//    }
//
//    public void setClasspathInfo(List<LuceneRequest> path, Map<String, List<JarPackageFragmentRoot>> map) {
//        this.path = path;
//        this.map = map;
//    }
//
//    public List<JarFileInformation> findFile(String name) {
//        indexer.waitUntilIndexingFinishes();
//        List<LuceneSearchResult> names = luceneAdaptor.findFile(path, name);
//        return names.stream()
//                .map(a -> new JarFileInformation(a.fullyQualifiedName, createPath(map, a)))
//                .collect(Collectors.toList());
//    }

}
