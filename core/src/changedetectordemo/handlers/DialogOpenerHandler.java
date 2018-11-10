package changedetectordemo.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import changedetectordemo.Activator;

public class DialogOpenerHandler extends AbstractHandler {
    private LuceneSearchAdaptor luceneSearchAdaptor;

    public DialogOpenerHandler() {
        this(Activator.luceneSearchAdaptor);
    }

    public DialogOpenerHandler(LuceneSearchAdaptor luceneSearchAdaptor) {
        this.luceneSearchAdaptor = luceneSearchAdaptor;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
//        List<JarFileInformation> result = luceneSearchAdaptor.findFile("pom*");
//        System.out.println("Result: " + result);

        Display.getDefault().asyncExec(() -> {
//      try {
            FileTreeWindow dialog = new FileTreeWindow(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), luceneSearchAdaptor);
            dialog.open();
//        EditorUtility.openInEditor(asd);
//      } catch (PartInitException e) {
            // TODO Auto-generated catch block
//        e.printStackTrace();
//      }
        });
        return null;
    }

}
