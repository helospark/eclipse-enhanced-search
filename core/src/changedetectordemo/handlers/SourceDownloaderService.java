package changedetectordemo.handlers;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.MavenJdtPlugin;

public class SourceDownloaderService {
    Set<String> blacklistedDependencies = new ConcurrentSkipListSet<>();
    boolean hasMaven;
    Object buildPathManager = false;
    boolean initialized = false;

    public SourceDownloaderService() {
    }

    public boolean attemptToDownloadSource(IPackageFragmentRoot root) {
        initIfNeeded();
        if (hasMaven()) {
            try {
                String path = root.getPath().toString();
                if (blacklistedDependencies.contains(path)) {
                    return false;
                }
                blacklistedDependencies.add(path);
                getMavenBuildPathManager().scheduleDownload(root, true, false);

                // No future is returned by scheduleDownload, we must poll :(
                pollUntilSourcePresent(root);
                boolean success = root.getSourceAttachmentPath() != null;
                if (!success) {
                    blacklistedDependencies.add(path);
                }
                return success;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false; // only Maven is supported
    }

    private void initIfNeeded() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    try {
                        buildPathManager = MavenJdtPlugin.getDefault().getBuildpathManager();
                    } catch (NoClassDefFoundError e) {
                        // No Maven present, it's ok
                    }
                    initialized = true;
                }
            }
        }
    }

    private void pollUntilSourcePresent(IPackageFragmentRoot root) throws JavaModelException, InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 8000) {
            if (root.getSourceAttachmentPath() != null) {
                break;
            }
            Thread.sleep(100);
        }
    }

    private boolean hasMaven() {
        return buildPathManager != null;
    }

    private IClasspathManager getMavenBuildPathManager() {
        return (IClasspathManager) buildPathManager; // Maven exists at this point, we can safely cast to it
    }

}
