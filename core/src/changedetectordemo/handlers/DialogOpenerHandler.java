package changedetectordemo.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
    private SearchResultToEditorConverter searchResultToEditorConverter;

    public DialogOpenerHandler() {
        this(Activator.luceneIndexRepository, Activator.jarFileIndexer, Activator.luceneWriteIndexFromFileExample, Activator.searchResultToEditorConverter);
    }

    public DialogOpenerHandler(LuceneIndexRepository repository, JarFileIndexer indexer, LuceneWriteIndexFromFileExample luceneWriteIndexFromFileExample,
            SearchResultToEditorConverter searchResultToEditorConverter) {
        this.repository = repository;
        this.indexer = indexer;
        this.luceneWriteIndexFromFileExample = luceneWriteIndexFromFileExample;
        this.searchResultToEditorConverter = searchResultToEditorConverter;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        List<IJavaProject> javaProjects = getJavaProjects();

        DialogInput dialogInput = new DialogInput();
        CompletableFuture<IndexReaderAndMappingDomain> indexReaderAndMapping = CompletableFuture.supplyAsync(() -> getIndexReaderFor(javaProjects, dialogInput));
        dialogInput.domain = indexReaderAndMapping;
        Display.getDefault().asyncExec(() -> {
            FileTreeWindow dialog = new FileTreeWindow(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), dialogInput, luceneWriteIndexFromFileExample,
                    searchResultToEditorConverter);
            dialog.open();
        });

        return null;
    }

    private IndexReaderAndMappingDomain getIndexReaderFor(List<IJavaProject> javaProjects, DialogInput dialogInput) {
        try {
            List<IndexReader> readers = new ArrayList<>();
            Map<String, List<JarPackageFragmentRoot>> pathToJarFileConverter = new HashMap<>();

            // for progress
            for (IJavaProject project : javaProjects) {
                try {
                    dialogInput.numberOfFilesRemaining += project.getResolvedClasspath(true).length;
                } catch (Exception e) {
                    e.printStackTrace(); // just progress
                }
            }

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
                                IPath path = jpf.getSourceAttachmentPath();
                                if (path == null) {
                                    System.out.println("Skipping " + jpf.getPath());
                                    decreaseRemaining(dialogInput);
                                    continue;
                                }
                                if (indexReader.isPresent()) {
                                    filePathUsed = jpf.getPath().toFile().getAbsolutePath();
                                } else {
                                    indexReader = getIndexReader(path);
                                    filePathUsed = path.toFile().getAbsolutePath();
                                }

                                if (!indexReader.isPresent()) {
                                    readers.add(indexer.indexFile(path.toFile()));
                                    filePathUsed = path.toFile().getAbsolutePath();
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
                                decreaseRemaining(dialogInput);
                            }

                        }
                    }
                }

            }
            MultiReader reader = new MultiReader(readers.toArray(new IndexReader[readers.size()]));
            return new IndexReaderAndMappingDomain(reader, pathToJarFileConverter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void decreaseRemaining(DialogInput dialogInput) {
        if (dialogInput.numberOfFilesRemaining > 1) {
            dialogInput.numberOfFilesRemaining -= 1;
        }
    }

    private Optional<IndexReader> getIndexReader(IPath path) {
        Optional<IndexReader> indexReader = repository.getReaderForJar(path.toFile());
        return indexReader;
    }

    static class DialogInput {
        CompletableFuture<IndexReaderAndMappingDomain> domain;
        volatile int numberOfFilesRemaining = 0;

        public void setDomain(CompletableFuture<IndexReaderAndMappingDomain> domain) {
            this.domain = domain;
        }

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
