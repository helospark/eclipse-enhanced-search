package changedetectordemo.handlers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
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

import changedetectordemo.handlers.MyListener.JarFileInformation;

public class FileTreeWindow extends SelectionStatusDialog {
    private boolean multi;
    private Text pattern;
    private Table list;
    private String initialPatternText;
    private LuceneSearchAdaptor adaptor;

    public FileTreeWindow(Shell shell, LuceneSearchAdaptor adaptor) {
        super(shell);
        this.adaptor = adaptor;
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
                    CompletableFuture.supplyAsync(() -> adaptor.findFile(newFile))
                            .thenAccept(a -> {
                                Display.getDefault().asyncExec(() -> {
                                    list.removeAll();
                                    for (var b : a) {
                                        TableItem ti = new TableItem(list, SWT.NONE);
                                        ti.setData(b);
                                        ti.setText(b.name);
                                    }
                                });
                            }).exceptionally(asd -> {
                                asd.printStackTrace();
                                return null;
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
                JarFileInformation jarInformation = (JarFileInformation) selection.getData();

                Object inf = jarInformation.file;
                thiz.close();
                try {
                    EditorUtility.openInEditor(inf);
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
}

class FileTreeContentProvider implements IStructuredContentProvider {

    @SuppressWarnings("unchecked")
    @Override
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof List) {
            return ((List<JarFileInformation>) inputElement).toArray();
        } else {
            return (Object[]) inputElement;
        }
    }

}

class FileTreeLabelProvider implements ITableLabelProvider {

    public FileTreeLabelProvider() {
    }

    public void addListener(ILabelProviderListener arg0) {
    }

    public void dispose() {
    }

    public boolean isLabelProperty(Object arg0, String arg1) {
        return false;
    }

    public void removeListener(ILabelProviderListener arg0) {
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