package changedetectordemo.indexing;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;

public class ClassDecompiler {

    public String decompile(byte[] data, String name) {
        Map<String, String> options = new HashMap<>();
        options.put("comments", "false");

        MyOutputStreamFactory sink = new MyOutputStreamFactory();
        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(options)
                .withClassFileSource(new DataSource(data, name))
                .withOutputSink(sink)
                .build();
        driver.analyse(Arrays.asList(name));
        return sink.getGeneratedSource();
    }

}

class MyOutputStreamFactory implements OutputSinkFactory {
    private String generatedSource;

    @Override
    public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
        return Collections.singletonList(SinkClass.STRING);
    }

    @Override
    public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
        return a -> {
            generatedSource = (String) a;
        };
    }

    public String getGeneratedSource() {
        return generatedSource;
    }

};

class DataSource implements ClassFileSource {
    byte[] data;
    private String name;

    public DataSource(byte[] data, String name) {
        this.data = data;
        this.name = name;
    }

    @Override
    public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
    }

    @Override
    public Collection<String> addJar(String jarPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPossiblyRenamedPath(String s) {
        return s;
    }

    @Override
    public Pair<byte[], String> getClassFileContent(String s) throws IOException {
        if (!s.equals(name)) {
            throw new FileNotFoundException("Not reading " + s);
        }
        return Pair.make(data, name);
    }
}