package changedetectordemo;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import changedetectordemo.handlers.LuceneSearchAdaptor;
import changedetectordemo.handlers.MyListener;
import changedetectordemo.indexing.JarFileIndexer;
import changedetectordemo.indexing.LuceneWriteIndexFromFileExample;
import changedetectordemo.indexing.UniqueNameCalculator;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements IStartup {

    // The plug-in ID
    public static final String PLUGIN_ID = "ChangeDetectorDemo"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    /**
     * The constructor
     */
    public Activator() {
    }

    public static LuceneSearchAdaptor luceneSearchAdaptor;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
     * BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        System.out.println("Started");

//        UniqueNameCalculator unc = new UniqueNameCalculator();
//        LuceneWriteIndexFromFileExample lwi = new LuceneWriteIndexFromFileExample(unc);
//        JarFileIndexer jfi = new JarFileIndexer(lwi, unc);
//        MyListener listener = new MyListener(jfi, lwi, unc);
//
//        JavaCore.addElementChangedListener(listener);

        plugin = this;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in relative
     * path
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    @Override
    public void earlyStartup() {
        UniqueNameCalculator unc = new UniqueNameCalculator();
        LuceneWriteIndexFromFileExample lwi = new LuceneWriteIndexFromFileExample(unc);
        JarFileIndexer jfi = new JarFileIndexer(lwi, unc);
        luceneSearchAdaptor = new LuceneSearchAdaptor(jfi, lwi);
        MyListener listener = new MyListener(jfi, luceneSearchAdaptor, unc);

        JavaCore.addElementChangedListener(listener);

    }
}
