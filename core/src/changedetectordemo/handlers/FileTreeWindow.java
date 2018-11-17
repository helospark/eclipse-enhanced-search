package changedetectordemo.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.internal.core.JarEntryDirectory;
import org.eclipse.jdt.internal.core.JarEntryFile;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

import changedetectordemo.handlers.DialogOpenerHandler.IndexReaderAndMappingDomain;
import changedetectordemo.indexing.LuceneWriteIndexFromFileExample;
import changedetectordemo.indexing.LuceneWriteIndexFromFileExample.LuceneSearchResult;

public class FileTreeWindow extends SelectionStatusDialog {
    private boolean multi;
    private Text pattern;
    private Table list;
    private String initialPatternText;
    private IndexReaderAndMappingDomain indexReader;
    private LuceneWriteIndexFromFileExample luceneWriteIndexFromFileExample;

    public FileTreeWindow(Shell shell, IndexReaderAndMappingDomain indexReader, LuceneWriteIndexFromFileExample luceneWriteIndexFromFileExample) {
        super(shell);
        this.indexReader = indexReader;
        this.luceneWriteIndexFromFileExample = luceneWriteIndexFromFileExample;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout myLayout = new GridLayout();
        myLayout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        myLayout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        myLayout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        myLayout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        composite.setLayout(myLayout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite content = new Composite(composite, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        content.setLayoutData(gd);

        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        content.setLayout(layout);

        final Label headerLabel = createHeader(content);

        pattern = new Text(content, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);

        gd = new GridData(GridData.FILL_HORIZONTAL);
        pattern.setLayoutData(gd);

        list = new Table(content, (multi ? SWT.MULTI : SWT.SINGLE)
                | SWT.BORDER | SWT.V_SCROLL | SWT.VIRTUAL);
        gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = list.getItemHeight() * 15;
        list.setLayoutData(gd);

        pattern.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                String file = pattern.getText();
                if (file.length() > 1) {
                    String newFile = file + "*";
                    List<LuceneSearchResult> result = luceneWriteIndexFromFileExample.findFile(indexReader.indexReader, newFile);

                    Display.getDefault().asyncExec(() -> {
                        list.removeAll();
                        for (var b : result) {
                            TableItem ti = new TableItem(list, SWT.NONE);
                            ti.setData(b);
                            ti.setText(b.fullyQualifiedName);
                        }
                    });
                }
            }
        });

        var thiz = this;

        list.addMouseListener(new MouseListener() {

            @Override
            public void mouseUp(MouseEvent e) {
            }

            @Override
            public void mouseDown(MouseEvent e) {
            }

            @Override
            public void mouseDoubleClick(MouseEvent e) {
                TableItem selection = list.getSelection()[0];
                LuceneSearchResult jarInformation = (LuceneSearchResult) selection.getData();
                Object path = FileTreeWindow.createPath(indexReader.pathToJarFileConverter, jarInformation);

                thiz.close();
                try {
                    EditorUtility.openInEditor(path);
                } catch (PartInitException sgdnjimsafsfaasdfasdfasdfy) {
                    sgdnjimsafsfaasdfasdfasdfy.printStackTrace();
                }
            }
        });

        applyDialogFont(content);

        if (initialPatternText != null) {
            pattern.setText(initialPatternText);
        }

        return dialogArea;
    }

    private Label createHeader(Composite parent) {
        Composite header = new Composite(parent, SWT.NONE);

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        header.setLayout(layout);

        Label headerLabel = new Label(header, SWT.NONE);
        headerLabel.setText("header");

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        headerLabel.setLayoutData(gd);

        header.setLayoutData(gd);
        return headerLabel;
    }

    @Override
    protected void computeResult() {
        // TODO Auto-generated method stub

    }

    public static Object createPath(Map<String, List<JarPackageFragmentRoot>> map, LuceneSearchResult a) {
        JarPackageFragmentRoot root;
        List<JarPackageFragmentRoot> filesWhereCurrentSourceIsAttached = map.get(a.jarPath);
        if (filesWhereCurrentSourceIsAttached.size() == 1) {
            root = filesWhereCurrentSourceIsAttached.get(0);
        } else {
            root = attemptToFindCorrectJar(a.fullyQualifiedName, filesWhereCurrentSourceIsAttached);
        }
        String fqn = a.fullyQualifiedName;
        if (fqn.endsWith(".java")) {
            fqn = fqn.replaceAll("\\.java", "\\.class");

//            root.getClassFile();

        }
        String[] parts = fqn.split("/");

        if (fqn.endsWith(".class")) {
            String[] newParts = Arrays.copyOf(parts, parts.length - 1);
            if (root.getModuleDescription() != null) {
                // TODO: is this right???
                newParts = Arrays.copyOfRange(newParts, 1, newParts.length);
            }
            PackageFragment qweqweq = root.getPackageFragment(newParts);
            return qweqweq.getClassFile(parts[parts.length - 1]);
        }

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

    // This hack is massive. It tries to find the correct jar in case there are
    // multiple (as in case of modularized JDK).
    // On a side node, you can search for "Hack" now in your codebase and
    // dependencies.
    private static JarPackageFragmentRoot attemptToFindCorrectJar(String fullyQualifiedName, List<JarPackageFragmentRoot> filesWhereCurrentSourceIsAttached) {
        int dotIndex = fullyQualifiedName.indexOf('.');
        int slashIndex = fullyQualifiedName.indexOf('/');

        String packageNameWithoutModule = fullyQualifiedName;
        if (dotIndex != -1 && dotIndex < slashIndex) {
            packageNameWithoutModule = packageNameWithoutModule.substring(slashIndex + 1);
        }
        String[] parts = packageNameWithoutModule.split("/");
        parts = Arrays.copyOf(parts, parts.length - 1);

        List<JarPackageFragmentRoot> result = new ArrayList<>();
        for (var root : filesWhereCurrentSourceIsAttached) {
            if (root.getPackageFragment(parts).exists()) {
                result.add(root);
            }
        }
        if (result.size() == 0) {
            System.out.println("No files");
            return filesWhereCurrentSourceIsAttached.get(0);
        }
        if (result.size() > 1) {
            System.out.println("Same package in multiple files");
        }
        return result.get(result.size() - 1);
    }
}
