package changedetectordemo;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import changedetectordemo.handlers.LuceneIndexRepository;
import changedetectordemo.handlers.SearchResultToEditorConverter;
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

//    public static LuceneSearchAdaptor luceneSearchAdaptor;

    public static LuceneIndexRepository luceneIndexRepository;

    public static JarFileIndexer jarFileIndexer;

    public static LuceneWriteIndexFromFileExample luceneWriteIndexFromFileExample;

    public static SearchResultToEditorConverter searchResultToEditorConverter;

    static {
        UniqueNameCalculator unc = new UniqueNameCalculator();
        luceneWriteIndexFromFileExample = new LuceneWriteIndexFromFileExample(unc);
        luceneIndexRepository = new LuceneIndexRepository(unc);
        jarFileIndexer = new JarFileIndexer(luceneWriteIndexFromFileExample, unc, luceneIndexRepository);
        searchResultToEditorConverter = new SearchResultToEditorConverter();
    }

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
//        luceneSearchAdaptor = new LuceneSearchAdaptor(jarFileIndexer, luceneWriteIndexFromFileExample);
//        MyListener listener = new MyListener(jarFileIndexer, luceneSearchAdaptor, unc);

//        JavaCore.addElementChangedListener(listener);

    }
}
