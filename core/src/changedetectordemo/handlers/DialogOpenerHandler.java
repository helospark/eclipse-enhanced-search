package changedetectordemo.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import changedetectordemo.Activator;
import changedetectordemo.indexing.JarFileIndexer;
import changedetectordemo.indexing.LuceneWriteIndexFromFileExample;

public class DialogOpenerHandler extends AbstractHandler {
    private LuceneIndexRepository repository;
    private JarFileIndexer indexer;
    private LuceneWriteIndexFromFileExample luceneWriteIndexFromFileExample;

    public DialogOpenerHandler() {
        this(Activator.luceneIndexRepository, Activator.jarFileIndexer, Activator.luceneWriteIndexFromFileExample);
    }

    public DialogOpenerHandler(LuceneIndexRepository repository, JarFileIndexer indexer, LuceneWriteIndexFromFileExample luceneWriteIndexFromFileExample) {
        this.repository = repository;
        this.indexer = indexer;
        this.luceneWriteIndexFromFileExample = luceneWriteIndexFromFileExample;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        List<IJavaProject> javaProjects = getJavaProjects();

        try {
            IndexReaderAndMappingDomain indexReader = getIndexReaderFor(javaProjects);
            Display.getDefault().asyncExec(() -> {
                FileTreeWindow dialog = new FileTreeWindow(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), indexReader, luceneWriteIndexFromFileExample);
                dialog.open();
            });
        } catch (JavaModelException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private IndexReaderAndMappingDomain getIndexReaderFor(List<IJavaProject> javaProjects) throws JavaModelException, IOException {
        List<IndexReader> readers = new ArrayList<>();
        Map<String, List<JarPackageFragmentRoot>> pathToJarFileConverter = new HashMap<>();
        for (IJavaProject project : javaProjects) {
            IClasspathEntry[] classpath = project.getRawClasspath();

            for (IClasspathEntry entry : classpath) {
                int entryKind = entry.getEntryKind();
                if (entryKind == IClasspathEntry.CPE_CONTAINER) {
                    IAdaptable[] children = new ClassPathContainer(project, entry).getChildren();
                    System.out.println(children);
                    for (Object child : children) {
                        if (child instanceof JarPackageFragmentRoot) {
                            JarPackageFragmentRoot jpf = (JarPackageFragmentRoot) child;
                            String filePathUsed;
                            Optional<IndexReader> indexReader = getIndexReader(jpf.getPath());
                            if (indexReader.isPresent()) {
                                filePathUsed = jpf.getPath().toFile().getAbsolutePath();
                            } else {
                                indexReader = getIndexReader(jpf.getSourceAttachmentPath());
                                filePathUsed = jpf.getSourceAttachmentPath().toFile().getAbsolutePath();
                            }

                            if (!indexReader.isPresent()) {
                                readers.add(indexer.indexFile(jpf.getSourceAttachmentPath().toFile()));
                                filePathUsed = jpf.getSourceAttachmentPath().toFile().getAbsolutePath();
                            } else {
                                readers.add(indexReader.get());
                            }

                            pathToJarFileConverter.compute(filePathUsed, (k, v) -> {
                                if (v == null) {
                                    v = new ArrayList<>();
                                }
                                v.add(jpf);
                                return v;
                            });
                        }

                    }
                }
            }

        }
        return new IndexReaderAndMappingDomain(new MultiReader(readers.toArray(new IndexReader[readers.size()])), pathToJarFileConverter);

    }

    private Optional<IndexReader> getIndexReader(IPath path) {
        Optional<IndexReader> indexReader = repository.getReaderForJar(path.toFile());
        return indexReader;
    }

    static class IndexReaderAndMappingDomain {
        IndexReader indexReader;
        Map<String, List<JarPackageFragmentRoot>> pathToJarFileConverter = new HashMap<>();

        public IndexReaderAndMappingDomain(IndexReader indexReader, Map<String, List<JarPackageFragmentRoot>> pathToJarFileConverter) {
            this.indexReader = indexReader;
            this.pathToJarFileConverter = pathToJarFileConverter;
        }

    }

    public static List<IJavaProject> getJavaProjects() {
        List<IJavaProject> projectList = new LinkedList<IJavaProject>();
        try {
            IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
            IProject[] projects = workspaceRoot.getProjects();
            for (int i = 0; i < projects.length; i++) {
                IProject project = projects[i];
                if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
                    projectList.add(JavaCore.create(project));
                }
            }
        } catch (CoreException ce) {
            ce.printStackTrace();
        }
        return projectList;
    }
}
