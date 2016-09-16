package de.cebitec.mgx.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 * @author sjaenick
 */
public class UnixHelper {

    public static boolean isGroupWritable(final File f) throws IOException {
        if (!f.exists()) {
            return false;
        }
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(Paths.get(f.getAbsolutePath()));
        return perms.contains(PosixFilePermission.GROUP_WRITE);
    }

    public static void createDirectory(final File targetDir) throws IOException {
        if (targetDir.exists() && targetDir.isDirectory()) {
            return;
        }
        Path path = Paths.get(targetDir.toURI());
        Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.GROUP_EXECUTE);
        Files.createDirectory(path, PosixFilePermissions.asFileAttribute(perms));
    }

    public static void createFile(final File targetDir) throws IOException {
        Path path = Paths.get(targetDir.toURI());
        Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.GROUP_EXECUTE);
        Files.createFile(path, PosixFilePermissions.asFileAttribute(perms));
    }

    public static void makeDirectoryGroupWritable(final String file) throws IOException {
        Path path = Paths.get(file);
        Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.GROUP_EXECUTE);
        Files.setPosixFilePermissions(path, perms);
    }

    public static void makeFileGroupWritable(final String file) throws IOException {
        Path path = Paths.get(file);
        Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE);
        Files.setPosixFilePermissions(path, perms);
    }

    public static void copyFile(final File in, final File out) throws IOException {
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        FileChannel inChannel = fis.getChannel();
        FileChannel outChannel = fos.getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
            fis.close();
            fos.close();
        }
    }

    public static String readFile(File f) throws IOException {
        if (!f.exists() && f.canRead()) {
            throw new IOException("File " + f.getAbsolutePath() + " missing or unreadable.");
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }
}
