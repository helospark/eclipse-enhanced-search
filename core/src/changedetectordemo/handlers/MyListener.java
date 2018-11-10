package changedetectordemo.handlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.internal.ui.packageview.LibraryContainer;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.helospark.lightdi.annotation.Component;

import changedetectordemo.indexing.JarFileIndexer;
import changedetectordemo.indexing.UniqueNameCalculator;

@Component
@SuppressWarnings("restriction")
public class MyListener implements IElementChangedListener {
    private JarFileIndexer indexer;
    private LuceneSearchAdaptor luceneSearchAdaptor;
    private UniqueNameCalculator uniqueNameCalculator;

    public MyListener(JarFileIndexer indexer, LuceneSearchAdaptor luceneSearchAdaptor, UniqueNameCalculator uniqueNameCalculator) {
        this.indexer = indexer;
        this.luceneSearchAdaptor = luceneSearchAdaptor;
        this.uniqueNameCalculator = uniqueNameCalculator;
    }

    @Override
    public void elementChanged(ElementChangedEvent event) {
        visit(event.getDelta());
    }

    private void visit(IJavaElementDelta delta) {
        IJavaElement el = delta.getElement();
        switch (el.getElementType()) {
        case IJavaElement.JAVA_MODEL:
            visitChildren(delta);
            break;
        case IJavaElement.JAVA_PROJECT:
            if (isClasspathChanged(delta.getFlags())) {
                notifyClasspathChanged((IJavaProject) el);
            }
            break;
        default:
            break;
        }
    }

    public static class LuceneRequest {
        public String uniqueId;
        public File file;

        public LuceneRequest(String uniqueId, File file) {
            this.uniqueId = uniqueId;
            this.file = file;
        }

    }

    private void notifyClasspathChanged(IJavaProject el) {
        IClasspathEntry[] rawClasspath;
        List<LuceneRequest> path = new ArrayList<>();
        Map<String, List<JarPackageFragmentRoot>> map = new HashMap<>();
        try {
            rawClasspath = el.getRawClasspath();
            for (IClasspathEntry entry : rawClasspath) {
                int entryKind = entry.getEntryKind();
                if (entryKind == IClasspathEntry.CPE_CONTAINER) {
                    IAdaptable[] children = new ClassPathContainer(el, entry).getChildren();
                    System.out.println(children);
                    for (Object child : children) {
                        if (child instanceof JarPackageFragmentRoot) {
                            JarPackageFragmentRoot jpf = (JarPackageFragmentRoot) child;
                            Optional.ofNullable(jpf.getSourceAttachmentPath()).map(a -> a.toFile())
                                    .ifPresent(a -> {
                                        String uniqueId = uniqueNameCalculator.calculateUniqueId(a);
                                        path.add(new LuceneRequest(uniqueId, a));
                                        map.compute(uniqueId, (k, v) -> {
                                            if (v == null) {
                                                v = new ArrayList<>();
                                            }
                                            v.add(jpf);
                                            return v;
                                        });
                                    });
                        }

                    }
                } else if ((entryKind == IClasspathEntry.CPE_LIBRARY || entryKind == IClasspathEntry.CPE_VARIABLE)) {
                    LibraryContainer libraryContainer = new LibraryContainer(el);
                    System.out.println(libraryContainer);
                }
            }
        } catch (JavaModelException e) {
            e.printStackTrace();
        }

        System.out.println("Path: " + path);

        for (LuceneRequest file : path) {
            try {
                indexer.indexFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        luceneSearchAdaptor.setClasspathInfo(path, map);
    }

    class FileTreeContentProvider implements IStructuredContentProvider {

        @Override
        public Object[] getElements(Object inputElement) {
            return null;
        }

    }

    private boolean isClasspathChanged(int flags) {
        return 0 != (flags & (IJavaElementDelta.F_CLASSPATH_CHANGED | IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED));
    }

    public void visitChildren(IJavaElementDelta delta) {
        for (IJavaElementDelta c : delta.getAffectedChildren()) {
            visit(c);
        }
    }

    static class JarFileInformation {
        public String name;
        public Object file;

        public JarFileInformation(String name, Object file) {
            this.name = name;
            this.file = file;
        }

        @Override
        public String toString() {
            return "JarFileInformation [name=" + name + ", file=" + file + "]";
        }

    }

    static class FileTreeLabelProvider implements ITableLabelProvider {

        @Override
        public void addListener(ILabelProviderListener listener) {

        }

        @Override
        public void dispose() {

        }

        @Override
        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        @Override
        public void removeListener(ILabelProviderListener listener) {

        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            return ((JarFileInformation) element).name;
        }

    }
}