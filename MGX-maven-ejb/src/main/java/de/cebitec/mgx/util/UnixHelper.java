package de.cebitec.mgx.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sjaenick
 */
public class UnixHelper {

    public static void createDirectory(File targetDir) {
        Path path = Paths.get(targetDir.toURI());
        Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.GROUP_EXECUTE);
        try {
            Files.createDirectory(path, PosixFilePermissions.asFileAttribute(perms));
        } catch (IOException ex) {
            Logger.getLogger(UnixHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void createFile(File targetDir) {
        Path path = Paths.get(targetDir.toURI());
        Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.GROUP_EXECUTE);
        try {
            Files.createFile(path, PosixFilePermissions.asFileAttribute(perms));
        } catch (IOException ex) {
            Logger.getLogger(UnixHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void makeDirectoryGroupWritable(String file) {
        Path path = Paths.get(file);
        Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.GROUP_EXECUTE);
        try {
            Files.setPosixFilePermissions(path, perms);
        } catch (IOException ex) {
            Logger.getLogger(UnixHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void makeFileGroupWritable(String file) {
        Path path = Paths.get(file);
        Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE);
        try {
            Files.setPosixFilePermissions(path, perms);
        } catch (IOException ex) {
            Logger.getLogger(UnixHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
