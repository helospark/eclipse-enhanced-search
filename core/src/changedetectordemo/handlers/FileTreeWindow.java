package changedetectordemo.handlers;

import java.util.Collections;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.text.contentassist.BoldStylerProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;

import changedetectordemo.handlers.DialogOpenerHandler.DialogInput;
import changedetectordemo.handlers.DialogOpenerHandler.IndexReaderAndMappingDomain;
import changedetectordemo.indexing.LuceneWriteIndexFromFileExample;
import changedetectordemo.indexing.LuceneWriteIndexFromFileExample.LuceneSearchResult;
import changedetectordemo.indexing.LuceneWriteIndexFromFileExample.LuceneSearchResultRoot;
import changedetectordemo.indexing.LuceneWriteIndexFromFileExample.SearchRequest;
import changedetectordemo.indexing.LuceneWriteIndexFromFileExample.TextSearchResult;

public class FileTreeWindow extends Dialog {
    private DialogInput dialogInput;
    private LuceneWriteIndexFromFileExample luceneWriteIndexFromFileExample;
    private SearchResultToEditorConverter searchResultToEditorConverter;

    volatile IndexReaderAndMappingDomain indexReaderAndMappingDomain = null;

    private Shell shell;
    private Text field;
    Button fileNameCheckBox;
    Button contentCheckbox;
    Button filePathCheckbox;
    TreeViewer treeViewer;
    Button caseSensitiveButton;

    Composite statusLine;

    public FileTreeWindow(Shell shell, DialogInput dialogInput, LuceneWriteIndexFromFileExample luceneWriteIndexFromFileExample,
            SearchResultToEditorConverter searchResultToEditorConverter) {
        super(shell);
        this.dialogInput = dialogInput;
        this.luceneWriteIndexFromFileExample = luceneWriteIndexFromFileExample;
        this.searchResultToEditorConverter = searchResultToEditorConverter;
    }

    public void open() {
        this.createContents();
        this.shell.open();
    }

    public void close() {
        this.shell.close();
    }

    private void createContents() {
        FileTreeWindow dialog = this;
        this.shell = new Shell(this.getParent(), SWT.SHELL_TRIM | SWT.BORDER | SWT.PRIMARY_MODAL | SWT.SHEET);
        this.shell.setSize(774, 338);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        this.shell.setLayout(layout);

        field = new Text(shell, SWT.SEARCH);

        Composite underSearchRow = new Composite(shell, 0);
        underSearchRow.setLayout(new RowLayout());

        Group checkboxComposite = new Group(underSearchRow, SWT.SHADOW_ETCHED_IN);
        checkboxComposite.setText("search in");
        checkboxComposite.setLayout(new RowLayout());
        fileNameCheckBox = new Button(checkboxComposite, SWT.CHECK);
        fileNameCheckBox.setText("filename");
        fileNameCheckBox.setSelection(true);
        addSearchOnChangedListener(fileNameCheckBox);

        filePathCheckbox = new Button(checkboxComposite, SWT.CHECK);
        filePathCheckbox.setText("filepath");
        filePathCheckbox.setSelection(true);
        addSearchOnChangedListener(filePathCheckbox);

        contentCheckbox = new Button(checkboxComposite, SWT.CHECK);
        contentCheckbox.setText("content");
        addSearchOnChangedListener(contentCheckbox);

        Group caseSensitiveGroup = new Group(underSearchRow, SWT.SHADOW_ETCHED_IN);
        caseSensitiveGroup.setLayout(new RowLayout());
        caseSensitiveGroup.setText("criteria");
        caseSensitiveButton = new Button(caseSensitiveGroup, SWT.CHECK);
        caseSensitiveButton.setText("Case sensitive");
        addSearchOnChangedListener(caseSensitiveButton);

        statusLine = new Composite(shell, 0);
        RowLayout statusLineLayout = new RowLayout();
        statusLineLayout.fill = true;
        statusLine.setLayout(statusLineLayout);
        Composite progressGroup = new Composite(statusLine, SWT.SHADOW_ETCHED_IN);
        progressGroup.setLayout(new RowLayout());
        Label label = new Label(progressGroup, 0);
        label.setText("Initializing...");

        new Thread(() -> {
            while (!dialogInput.domain.isDone()) {
                try {
                    if (dialogInput.numberOfFilesRemaining > 0) {
                        setStatus(label, "Indexing (" + dialogInput.numberOfFilesRemaining + " remaining)");
                    }
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            setStatus(label, "Ready");
            indexReaderAndMappingDomain = dialogInput.domain.join();
        }).start();

        treeViewer = new TreeViewer(shell);
        treeViewer.setLabelProvider(new MyLabelProvider());
        treeViewer.setContentProvider(new MyContentProvider());
        treeViewer.setInput(new LuceneSearchResultRoot(Collections.emptyList()));
        treeViewer.refresh();

        Composite buttonLine = new Composite(shell, SWT.RIGHT_TO_LEFT);
        RowLayout buttonRowLayout = new RowLayout();
        buttonLine.setLayout(buttonRowLayout);
        Button openButton = new Button(buttonLine, 0);
        openButton.setText("Open");
        openButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
                Object selectedElement = selection.getFirstElement();
                openSelection(dialog, selectedElement);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {

            }
        });

        Button cancelButton = new Button(buttonLine, 0);
        cancelButton.setText("Cancel");
        cancelButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                dialog.close();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        treeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        field.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        underSearchRow.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        statusLine.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        buttonLine.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

        field.addModifyListener(a -> scheduleSearch());

        treeViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                if (indexReaderAndMappingDomain == null) {
                    return;
                }
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                Object selectedElement = selection.getFirstElement();

                openSelection(dialog, selectedElement);
            }

        });

    }

    private void openSelection(FileTreeWindow dialog, Object selectedElement) {
        Object path = null;
        if (selectedElement instanceof LuceneSearchResult) {
            LuceneSearchResult jarInformation = (LuceneSearchResult) selectedElement;
            path = searchResultToEditorConverter.createPath(indexReaderAndMappingDomain.pathToJarFileConverter, jarInformation);
        } else if (selectedElement instanceof TextSearchResult) {
            TextSearchResult jarInformation = (TextSearchResult) selectedElement;
            path = searchResultToEditorConverter.createPath(indexReaderAndMappingDomain.pathToJarFileConverter, jarInformation.parent);
        }
        try {
            EditorUtility.openInEditor(path);
        } catch (PartInitException sgdnjimsafsfaasdfasdfasdfy) {
            sgdnjimsafsfaasdfasdfasdfy.printStackTrace();
        }
        dialog.close();
    }

    private void setStatus(Label label, String text) {
        Display.getDefault().asyncExec(() -> {
            label.setText(text);
            statusLine.pack(); // refresh layout
        });
    }

    private void addSearchOnChangedListener(Button button) {
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                scheduleSearch();
            }
        });
    }

    private void scheduleSearch() {
        String file = field.getText();
        if (file.length() > 2 && indexReaderAndMappingDomain != null) {
            String newFile = file + "*";

            SearchRequest request = SearchRequest.builder()
                    .withContent(newFile)
                    .withIncludeContent(contentCheckbox.getSelection())
                    .withIncludeFileName(fileNameCheckBox.getSelection())
                    .withIncludeFilePath(filePathCheckbox.getSelection())
                    .withCaseSensitive(caseSensitiveButton.getSelection())
                    .withMultiReader(indexReaderAndMappingDomain.indexReader)
                    .build();

            LuceneSearchResultRoot result = luceneWriteIndexFromFileExample.findFile(request);

            Display.getDefault().asyncExec(() -> {
                treeViewer.setInput(result);
                treeViewer.expandAll();
                treeViewer.refresh();
            });
        }
    }

    static class MyContentProvider implements ITreeContentProvider {

        @Override
        public Object[] getElements(Object inputElement) {
            return getChildren(inputElement);
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof LuceneSearchResultRoot) {
                return ((LuceneSearchResultRoot) parentElement).results.toArray();
            } else if (parentElement instanceof LuceneSearchResult) {
                return ((LuceneSearchResult) parentElement).textSearchResults.toArray();
            } else {
                return null;
            }
        }

        @Override
        public Object getParent(Object element) {
            System.out.println("I think this is not called");
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            Object[] children = getChildren(element);
            return children != null && children.length != 0;
        }

    }

    static class MyLabelProvider extends DelegatingStyledCellLabelProvider {

        public MyLabelProvider() {
            super(new IStyledLabelProvider() {

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
                public StyledString getStyledText(Object element) {
                    if (element instanceof LuceneSearchResult) {
                        return new StyledString(((LuceneSearchResult) element).fullyQualifiedName);
                    } else if (element instanceof TextSearchResult) {
                        return formatLine(element);
                    }
                    return new StyledString("NOT_DEFINED");
                }

                private StyledString formatLine(Object element) {
                    String line = ((TextSearchResult) element).foundLine;

                    String[] parts = line.split("</?B>");

                    StyledString result = new StyledString();
                    for (int i = 0; i < parts.length; ++i) {
                        if (i % 2 == 0) {
                            result.append(parts[i]);
                        } else {
                            result.append(parts[i], new BoldStylerProvider(Display.getDefault().getSystemFont()).getBoldStyler());
                        }
                    }

                    return result;
                }

                @Override
                public Image getImage(Object element) {
                    // TODO Auto-generated method stub
                    return null;
                }

            });
        }

    }

}
