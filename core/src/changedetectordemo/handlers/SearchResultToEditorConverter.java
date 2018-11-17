package changedetectordemo.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.internal.core.JarEntryDirectory;
import org.eclipse.jdt.internal.core.JarEntryFile;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.PackageFragment;

import changedetectordemo.indexing.LuceneWriteIndexFromFileExample.LuceneSearchResult;

@SuppressWarnings("restriction")
public class SearchResultToEditorConverter {
    public Object createPath(Map<String, List<JarPackageFragmentRoot>> map, LuceneSearchResult a) {
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
    private JarPackageFragmentRoot attemptToFindCorrectJar(String fullyQualifiedName, List<JarPackageFragmentRoot> filesWhereCurrentSourceIsAttached) {
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
