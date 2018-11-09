package changedetectordemo.handlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JarEntryDirectory;
import org.eclipse.jdt.internal.core.JarEntryFile;
import org.eclipse.jdt.internal.core.JarEntryResource;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.internal.ui.packageview.LibraryContainer;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.helospark.lightdi.annotation.Component;

import changedetectordemo.indexing.JarFileIndexer;
import changedetectordemo.indexing.LuceneWriteIndexFromFileExample;
import changedetectordemo.indexing.LuceneWriteIndexFromFileExample.LuceneSearchResult;
import changedetectordemo.indexing.UniqueNameCalculator;

@Component
public class MyListener implements IElementChangedListener {
    private JarFileIndexer indexer;
    private LuceneWriteIndexFromFileExample luceneAdaptor;
    private UniqueNameCalculator uniqueNameCalculator;

    public MyListener(JarFileIndexer indexer, LuceneWriteIndexFromFileExample luceneAdaptor, UniqueNameCalculator uniqueNameCalculator) {
        this.indexer = indexer;
        this.luceneAdaptor = luceneAdaptor;
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
        Map<String, JarPackageFragmentRoot> map = new HashMap<>();
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
                                        map.put(uniqueId, jpf);
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
        indexer.waitUntilIndexingFinishes();
        List<LuceneSearchResult> names = luceneAdaptor.findFile(path, "pom*");
        List<JarFileInformation> result = names.stream()
                .map(a -> new JarFileInformation(a.fullyQualifiedName, createPath(map, a)))
                .collect(Collectors.toList());

        System.out.println("Result: " + result);

        Display.getDefault().asyncExec(() -> {
//      try {
            new FileTreeWindow(result);
//        EditorUtility.openInEditor(asd);
//      } catch (PartInitException e) {
            // TODO Auto-generated catch block
//        e.printStackTrace();
//      }
        });
    }

    private JarEntryFile createPath(Map<String, JarPackageFragmentRoot> map, LuceneSearchResult a) {
        JarPackageFragmentRoot root = map.get(a.jarName);
        String fqn = a.fullyQualifiedName;
        if (fqn.endsWith(".java")) {
            fqn = fqn.replaceAll("\\.java", "\\.class");
        }
        String[] parts = fqn.split("/");

        Object parent = root;
        for (int i = 0; i < parts.length - 1; ++i) {
            JarEntryDirectory dir = new JarEntryDirectory(parts[i]);
            dir.setParent(parent);
            parent = dir;
        }

        JarEntryFile resource = new JarEntryFile(parts[parts.length - 1]);
        resource.setParent(parent);
        return resource;
    }

    private void addResource(List<JarFileInformation> result, Object child, Object object) throws JavaModelException {
        if (object instanceof JarEntryDirectory) {
            for (Object asd : ((JarEntryDirectory) object).getChildren())
                addResource(result, child, asd);
        }
        if (object instanceof JarEntryFile) {
            JarEntryResource oj = (JarEntryResource) object;
            result.add(new JarFileInformation(((JarPackageFragmentRoot) child).getSourceAttachmentPath() + oj.getFullPath().toString(), oj));
        }
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
        public JarEntryResource file;

        public JarFileInformation(String name, JarEntryResource file) {
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