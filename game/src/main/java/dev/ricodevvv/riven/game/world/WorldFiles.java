package dev.ricodevvv.riven.game.world;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Zips, unzips, copies and deletes world folders. Same job as BedWars2023's WorldZipper.
 */
public final class WorldFiles {

    private static final Set<String> IGNORED = new HashSet<>(Arrays.asList("session.lock", "uid.dat"));

    private WorldFiles() {}

    /**
     * Zips a whole folder (without session.lock and uid.dat). Writes to a temp file first so a crash never
     * leaves a half written template.
     *
     * @param folder world folder
     * @param zip target file
     * @throws IOException if writing fails
     */
    public static void zip(File folder, File zip) throws IOException {
        final File parent = zip.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Couldn't create " + parent);
        }

        final File temp = new File(zip.getPath() + ".tmp");
        final Path root = folder.toPath();

        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(temp)); Stream<Path> paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                if (Files.isDirectory(path) || IGNORED.contains(path.getFileName().toString())) {
                    continue;
                }

                out.putNextEntry(new ZipEntry(root.relativize(path).toString().replace(File.separatorChar, '/')));
                Files.copy(path, out);
                out.closeEntry();
            }
        }

        Files.move(temp.toPath(), zip.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Unzips a file into a folder, refusing entries that try to escape it.
     *
     * @param zip file
     * @param folder target folder (created if missing)
     * @throws IOException if reading fails or an entry is invalid
     */
    public static void unzip(File zip, File folder) throws IOException {
        final Path root = folder.toPath().toAbsolutePath().normalize();

        try (ZipInputStream in = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry entry;

            while ((entry = in.getNextEntry()) != null) {
                final Path target = root.resolve(entry.getName()).normalize();

                if (!target.startsWith(root)) {
                    throw new IOException("Invalid zip entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }

                Files.createDirectories(target.getParent());

                try (OutputStream out = new FileOutputStream(target.toFile())) {
                    copy(in, out);
                }
            }
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        final byte[] buffer = new byte[8192];
        int read;

        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    /**
     * Copies a world folder (without session.lock and uid.dat).
     *
     * @param from source folder
     * @param to target folder
     * @throws IOException if copying fails
     */
    public static void copy(File from, File to) throws IOException {
        final Path root = from.toPath();
        final Path target = to.toPath();

        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                if (IGNORED.contains(path.getFileName().toString())) {
                    continue;
                }

                final Path destination = target.resolve(root.relativize(path).toString());

                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * Deletes a folder and everything in it.
     *
     * @param folder folder
     * @throws IOException if something can't be deleted
     */
    public static void delete(File folder) throws IOException {
        if (!folder.exists()) {
            return;
        }

        try (Stream<Path> paths = Files.walk(folder.toPath())) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                Files.deleteIfExists(path);
            }
        }
    }

}
