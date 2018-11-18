package changedetectordemo.indexing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Optional;
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
    private ClassDecompiler decompiler;

    public JarFileIndexer(LuceneWriteIndexFromFileExample luceneWriteIndexFromFileExample, UniqueNameCalculator uniqueNameCalculator, LuceneIndexRepository repository,
            ClassDecompiler decompiler) {
        this.luceneWriteIndexFromFileExample = luceneWriteIndexFromFileExample;
        this.uniqueNameCalculator = uniqueNameCalculator;
        this.repository = repository;
        this.decompiler = decompiler;
    }

    public IndexReader indexFile(File file) throws IOException {
        return indexSingleFile(file);
    }

    private IndexReader indexSingleFile(File file) {
        String indexName = uniqueNameCalculator.indexPathCalculator(file);

        Optional<DirectoryReader> optionalReader = exceptionSafeReaderGet(indexName);
        if (optionalReader.isPresent()) {
            return optionalReader.get();
        } else {
            System.out.println("Actually indexing " + file.getName());
            if (file.getName().endsWith(".jar") || file.getName().endsWith(".zip")) {
                try (ZipFile zipFile = new ZipFile(file)) {
                    IndexWriter writer = luceneWriteIndexFromFileExample.indexContentTo(indexName);
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();

                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (!entry.isDirectory()) {
                            String data = readFile(zipFile, entry);
                            String path = entry.getName();
                            luceneWriteIndexFromFileExample.addToIndex(writer, new IndexEntity(data, path, indexName, file.getAbsolutePath()));
                        }
                    }
                    writer.close();
                    DirectoryReader reader = exceptionSafeReaderGet(indexName).get();
                    repository.addIndexerForId(indexName, reader);
                    return reader;
                } catch (IOException e1) {
                    e1.printStackTrace();
                    throw new RuntimeException(e1);
                }
            }
        }
        throw new RuntimeException("Not supported " + file.getAbsolutePath());
    }

    private String readFile(ZipFile zipFile, ZipEntry entry) {
        try (InputStream inputStream = zipFile.getInputStream(entry)) {
            if (entry.getName().endsWith(".class")) {
                byte[] data = readBinary(inputStream).toByteArray();
                System.out.println("Decompiling " + entry.getName());
                return decompiler.decompile(data, entry.getName());
            } else { // TODO: other binary types
                return readString(inputStream);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error unzipping file ", e);
        }
    }

    private Optional<DirectoryReader> exceptionSafeReaderGet(String indexName) {
        try {
            return Optional.ofNullable(DirectoryReader.open(FSDirectory.open(Paths.get(indexName))));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static String readString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream into = readBinary(inputStream);
        return new String(into.toByteArray(), "UTF-8");
    }

    private static ByteArrayOutputStream readBinary(InputStream inputStream) throws IOException {
        ByteArrayOutputStream into = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int n; 0 < (n = inputStream.read(buf));) {
            into.write(buf, 0, n);
        }
        into.close();
        return into;
    }

}
