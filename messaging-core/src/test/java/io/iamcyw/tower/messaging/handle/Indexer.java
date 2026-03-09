package io.iamcyw.tower.messaging.handle;

import org.jboss.jandex.IndexView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;

/**
 * Utility class for indexing classes during testing.
 *
 * <p>This class provides methods to index classes using Jandex for test scenarios.
 * It supports indexing packages and individual classes, which is useful for
 * testing handler discovery and metadata generation.
 *
 * <p>The indexer is used to create an {@link IndexView} that can be used by
 * {@link io.iamcyw.tower.schema.SchemaBuilder} or handler discovery mechanisms
 * to find and analyze classes at build time.
 *
 * @since 2.0
 */
public class Indexer {

    /**
     * Returns an index of all test classes in the test package.
     *
     * <p>This indexes all classes in the {@code io.iamcyw.tower.messaging.test}
     * package, including CommandHandler implementations.
     *
     * @return the index view of test classes
     */
    public static IndexView getAllTestIndex() {
        return getTestIndex("io/iamcyw/tower/messaging/test");
    }

    /**
     * Returns an index of classes in the specified package.
     *
     * @param packageName the package name in path format (e.g., "io/iamcyw/tower/messaging/test")
     * @return the index view of classes in the package
     */
    public static IndexView getTestIndex(String packageName) {
        org.jboss.jandex.Indexer indexer = new org.jboss.jandex.Indexer();
        indexDirectory(indexer, packageName);
        return indexer.complete();
    }

    /**
     * Returns an index of classes in the same package as the given class.
     *
     * @param clazz the class whose package should be indexed
     * @return the index view of classes in the package
     */
    public static IndexView getTestIndex(Class<?> clazz) {
        return getTestIndex(clazz.getPackage().getName().replace('.', '/'));
    }

    /**
     * Indexes all classes in the specified directory.
     *
     * @param indexer the Jandex indexer
     * @param baseDir the base directory to index
     */
    private static void indexDirectory(org.jboss.jandex.Indexer indexer, String baseDir) {
        InputStream directoryStream = getResourceAsStream(baseDir);
        if (directoryStream == null) {
            // Directory doesn't exist, return empty index
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(directoryStream));
        reader.lines()
                .filter(resName -> resName.endsWith(".class"))
                .map(resName -> Paths.get(baseDir, resName))
                .forEach(path -> index(indexer, path.toString()));
    }

    /**
     * Returns an input stream for the specified resource path.
     *
     * @param path the resource path
     * @return the input stream, or null if not found
     */
    private static InputStream getResourceAsStream(String path) {
        return Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(path);
    }

    /**
     * Indexes a single class file.
     *
     * @param indexer the Jandex indexer
     * @param resName the resource name of the class file
     */
    private static void index(org.jboss.jandex.Indexer indexer, String resName) {
        try {
            InputStream stream = getResourceAsStream(resName);
            if (stream != null) {
                indexer.index(stream);
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

}
