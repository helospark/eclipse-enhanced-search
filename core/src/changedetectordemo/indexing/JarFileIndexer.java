package changedetectordemo.indexing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.lucene.index.IndexWriter;

import com.helospark.lightdi.annotation.Component;

import changedetectordemo.handlers.MyListener.LuceneRequest;

@Component
public class JarFileIndexer {
    private ExecutorService executorService = Executors.newFixedThreadPool(1);
    private ConcurrentHashMap<String, CompletableFuture<Void>> inprogressIndexing = new ConcurrentHashMap<>();
    private LuceneWriteIndexFromFileExample luceneWriteIndexFromFileExample;
    private UniqueNameCalculator uniqueNameCalculator;

    public JarFileIndexer(LuceneWriteIndexFromFileExample luceneWriteIndexFromFileExample, UniqueNameCalculator uniqueNameCalculator) {
        this.luceneWriteIndexFromFileExample = luceneWriteIndexFromFileExample;
        this.uniqueNameCalculator = uniqueNameCalculator;
    }

    public void indexFile(LuceneRequest request) throws IOException {
        inprogressIndexing.putIfAbsent(request.uniqueId,
                CompletableFuture.runAsync(() -> indexSingleFile(request.file, request.uniqueId), executorService));
    }

    private void indexSingleFile(File file, String uniqueId) {
        if (luceneWriteIndexFromFileExample.hasIndex(uniqueId)) {
            return;
        }
        System.out.println("Actually indexing " + file.getName());
        IndexWriter writer = luceneWriteIndexFromFileExample.indexContentTo(uniqueId);
        if (file.getName().endsWith(".jar") || file.getName().endsWith(".zip")) {
            try (ZipFile zipFile = new ZipFile(file)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        try (InputStream inputStream = zipFile.getInputStream(entry)) {
                            String data = readString(inputStream);
                            String path = entry.getName();

                            luceneWriteIndexFromFileExample.addToIndex(writer, new IndexEntity(data, path, uniqueId));
                        } catch (IOException e) {
                            throw new RuntimeException("Error unzipping file ", e);
                        }
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

    public void waitUntilIndexingFinishes() {
        Collection<CompletableFuture<Void>> values = inprogressIndexing.values();
        CompletableFuture.allOf(values.toArray(new CompletableFuture[values.size()])).join();
    }

}
