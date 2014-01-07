package de.cebitec.mgx.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
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
    
    public static boolean isGroupWritable(File f) {
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(Paths.get(f.getAbsolutePath()));
            return perms.contains(PosixFilePermission.GROUP_WRITE);
        } catch (IOException ex) {
            Logger.getLogger(UnixHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

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

    public static void copyFile(File in, File out) throws IOException {
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
}
