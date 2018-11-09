package changedetectordemo.handlers;

import java.util.List;

import org.eclipse.jdt.internal.core.JarEntryResource;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PartInitException;

import changedetectordemo.handlers.MyListener.JarFileInformation;

public class FileTreeWindow extends ApplicationWindow {
    List<JarFileInformation> infos;

    public FileTreeWindow(List<JarFileInformation> infos) {
        super(null);
        this.infos = infos;
        setBlockOnOpen(true);

        open();
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setSize(400, 400);
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        final TableViewer tv = new TableViewer(composite);

        // Set the content and label providers
        tv.setContentProvider(new FileTreeContentProvider());
        tv.setLabelProvider(new FileTreeLabelProvider());

        // Set up the table
        Table table = tv.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        // Add the first name column
        TableColumn tc = new TableColumn(table, SWT.LEFT);
        tc.setText("Column");
        tc.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                tv.refresh();
            }
        });
        table.getColumn(0).pack();

        tv.setInput(infos);

        tv.addDoubleClickListener(a -> {
            IStructuredSelection selection = (IStructuredSelection) a.getSelection();
            Object firstElement = selection.getFirstElement();

            JarEntryResource inf = ((JarFileInformation) firstElement).file;
            this.close();
            try {
                EditorUtility.openInEditor(inf);
            } catch (PartInitException e) {
                e.printStackTrace();
            }
        });

        return composite;
    }
}

class FileTreeContentProvider implements IStructuredContentProvider {

    @SuppressWarnings("unchecked")
    @Override
    public Object[] getElements(Object inputElement) {
        return ((List<JarFileInformation>) inputElement).toArray();
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