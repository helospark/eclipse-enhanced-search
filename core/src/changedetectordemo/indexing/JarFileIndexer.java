package changedetectordemo.indexing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;

import com.helospark.lightdi.annotation.Component;

import changedetectordemo.handlers.LuceneIndexRepository;

@Component
public class JarFileIndexer {
    private LuceneWriteIndexFromFileExample luceneWriteIndexFromFileExample;
    private UniqueNameCalculator uniqueNameCalculator;
    private LuceneIndexRepository repository;

    public JarFileIndexer(LuceneWriteIndexFromFileExample luceneWriteIndexFromFileExample, UniqueNameCalculator uniqueNameCalculator, LuceneIndexRepository repository) {
        this.luceneWriteIndexFromFileExample = luceneWriteIndexFromFileExample;
        this.uniqueNameCalculator = uniqueNameCalculator;
        this.repository = repository;
    }

    public IndexReader indexFile(File file) throws IOException {
        return indexSingleFile(file);
    }

    private IndexReader indexSingleFile(File file) {
        String indexName = uniqueNameCalculator.indexPathCalculator(file);

        if (!new File(indexName).exists()) {

            System.out.println("Actually indexing " + file.getName());
            if (file.getName().endsWith(".jar") || file.getName().endsWith(".zip")) {
                try (ZipFile zipFile = new ZipFile(file)) {
                    IndexWriter writer = luceneWriteIndexFromFileExample.indexContentTo(indexName);
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();

                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (!entry.isDirectory()) {
                            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                                String data = readString(inputStream);
                                String path = entry.getName();

                                luceneWriteIndexFromFileExample.addToIndex(writer, new IndexEntity(data, path, indexName, file.getAbsolutePath()));
                            } catch (IOException e) {
                                throw new RuntimeException("Error unzipping file ", e);
                            }
                        }
                    }
                    writer.close();
                    DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexName)));
                    repository.addIndexerForId(indexName, reader);
                    return reader;
                } catch (IOException e1) {
                    e1.printStackTrace();
                    throw new RuntimeException(e1);
                }
            }
        } else {
            DirectoryReader reader;
            try {
                reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexName)));
                repository.addIndexerForId(indexName, reader);
                return reader;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Not supported " + file.getAbsolutePath());
    }

    public static String readString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream into = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int n; 0 < (n = inputStream.read(buf));) {
            into.write(buf, 0, n);
        }
        into.close();
        return new String(into.toByteArray(), "UTF-8");
    }

}
