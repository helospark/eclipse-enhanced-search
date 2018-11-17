package changedetectordemo.indexing;

public class IndexEntity {
    String data;
    String path;
    String indexPath;
    String jarPath;

    public IndexEntity(String data, String path, String indexPath, String jarName) {
        this.data = data;
        this.path = path;
        this.indexPath = indexPath;
        this.jarPath = jarName;
    }

}
