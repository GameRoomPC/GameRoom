package data.io;

import data.game.entry.Platform;
import edu.umd.cs.findbugs.annotations.NonNull;
import system.os.WindowsShortcut;
import ui.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.util.regex.Pattern;

/**
 * Created by LM on 26/09/2016.
 */
public class FileUtils {

    @NonNull
    public static File initOrCreateFolder(@NonNull File f) {
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }

    @NonNull
    public static File initOrCreateFolder(@NonNull String f) {
        return initOrCreateFolder(new File(f));
    }

    @NonNull
    private static File initOrCreateFile(@NonNull File f) {
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return f;
    }

    /**
     * Checks whether GameRoom's temp folder exists, and returns the temp file wanter
     * @param filename the name of the file to fetch, with extension and not, and can include subdirs of the temp folder
     * @return the temp file, whether it exists or not
     */
    public static File getTempFile(@NonNull String filename) {
        if (Main.FILES_MAP == null || Main.FILES_MAP.isEmpty()) {
            throw new IllegalStateException("Cannot get tempfile : FILE_MAP is not initialized");
        }
        File tempFolder = Main.FILES_MAP.get("temp");
        if (tempFolder == null || !tempFolder.exists()) {
            throw new IllegalStateException("Temp folder does not exist");
        }
        return new File(tempFolder.getAbsolutePath() + File.separator + filename);
    }

    /**
     * Initialize (and creates if necessary) a temp file in GameRoom's temp folder. It does not create into the OS' temp
     * folder as temp files as supposed to be deleted at the app's end and {@link File#deleteOnExit()} seems to be not
     * reliable. Hence we create the file (and necessary directories) in GameRoom's temp folder that is cleaned when the
     * app exits properly
     * @param fileName the name of the file to create, including its extension. Can also include subdirs that will be
     *                 created !
     * @return the initialized file, created if possible and non already existing
     */
    @NonNull
    public static File newTempFile(@NonNull String fileName) {
        if (Main.FILES_MAP == null || Main.FILES_MAP.isEmpty()) {
            throw new IllegalStateException("Cannot create tempfile : FILE_MAP is not initialized");
        }
        File tempFolder = Main.FILES_MAP.get("temp");
        if (tempFolder == null || !tempFolder.exists()) {
            throw new IllegalStateException("Temp folder does not exist");
        }

        return initOrCreateFile(tempFolder.getAbsolutePath() + File.separator + fileName);
    }

    @NonNull
    public static File initOrCreateFile(@NonNull String f) {
        return initOrCreateFile(new File(f));
    }

    public static File copyToFolder(File src, File target, String newName) throws IOException {
        return Files.copy(src.toPath(), target.toPath().resolve(newName), StandardCopyOption.REPLACE_EXISTING).toFile();
    }

    public static File moveToFolder(File src, File target) {
        File movedFile = new File(target.getAbsolutePath() + File.separator + src.getName());
        if (src.exists() && !movedFile.exists()) {
            if (target.isDirectory()) {
                target.mkdirs();
            } else {
                target.getParentFile().mkdirs();
            }
            if (src.isDirectory()) {
                movedFile.mkdirs();
            } else {
                movedFile.getParentFile().mkdirs();
            }


            if (src.exists()) {
                if (src.isDirectory() && src.listFiles().length > 0) {
                    //move all subfiles recursively
                    for (File subFile : src.listFiles()) {
                        moveToFolder(subFile, movedFile);
                    }
                    //deleteFiles empty folder afterwards
                    src.delete();
                } else if (src.isDirectory() && src.listFiles().length == 0) {
                    //deleteFiles empty folder
                    src.delete();
                } else {
                    //is a file, we'll try to move it or at least copy it
                    try {
                        Files.move(src.toPath(), movedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        Main.LOGGER.error("Error while moving file : " + src.getAbsolutePath() + ", copying it");
                        try {
                            Files.copy(src.toPath(), movedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            src.delete();
                        } catch (IOException e1) {
                            Main.LOGGER.error("Could not copy file " + src.getAbsolutePath());
                            e1.printStackTrace();
                        }
                    }
                }
            }
        }
        return movedFile;
    }

    public static void clearFolder(File folder) {
        if (folder.exists() && folder.isDirectory()) {
            for (int i = 0; i < folder.listFiles().length; i++) {
                File temp = folder.listFiles()[i];
                if (temp.isDirectory()) {
                    clearFolder(temp);
                }
                temp.delete();
            }
        }
    }

    public static void deleteFolder(File folder) {
        if (folder.exists() && folder.isDirectory()) {
            for (File sub : folder.listFiles()) {
                deleteFolder(sub);
            }
        }
        folder.delete();
    }

    public static File relativizePath(File targetFile, File baseFile) {
        String pathSeparator = File.separator;
        //  We need the -1 argument to split to make sure we get a trailing
        //  "" token if the base ends in the path separator and is therefore
        //  a directory. We require directory paths to end in the path
        //  separator -- otherwise they are indistinguishable from files.
        String[] base = baseFile.getPath().split(Pattern.quote(pathSeparator), -1);
        String[] target = targetFile.getPath().split(Pattern.quote(pathSeparator), 0);

        //  First get all the common elements. Store them as a string,
        //  and also count how many of them there are.
        String common = "";
        int commonIndex = 0;
        for (int i = 0; i < target.length && i < base.length; i++) {
            if (target[i].equals(base[i])) {
                common += target[i] + pathSeparator;
                commonIndex++;
            } else break;
        }

        if (commonIndex == 0) {
            //  Whoops -- not even a single common path element. This most
            //  likely indicates differing drive letters, like C: and D:.
            //  These paths cannot be relativized. Return the target path.

            return targetFile;
            //  This should never happen when all absolute paths
            //  begin with / as in *nix.
        }

        String relative = "";
        if (base.length == commonIndex) {
            //  Comment this out if you prefer that a relative path not start with ./
            //relative = "." + pathSeparator;
        } else {
            int numDirsUp = base.length - commonIndex - 1;
            //  The number of directories we have to backtrack is the length of
            //  the base path MINUS the number of common path elements, minus
            //  one because the last element in the path isn't a directory.
            for (int i = 1; i <= (numDirsUp); i++) {
                relative += ".." + pathSeparator;
            }
        }
        relative += targetFile.getPath().substring(common.length());


        //Main.LOGGER.debug("Original path : " + targetFile.getPath());
        //Main.LOGGER.debug("Relativized : " + new File(relative).getPath());

        return new File(relative);

    }

    public static String getExtension(File file) {
        return getExtension(file.getAbsolutePath());
    }

    //does not include the .
    public static String getExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int extensionPos = filename.lastIndexOf('.');
        int lastUnixPos = filename.lastIndexOf('/');
        int lastWindowsPos = filename.lastIndexOf('\\');
        int lastSeparator = Math.max(lastUnixPos, lastWindowsPos);
        int index = lastSeparator > extensionPos ? -1 : extensionPos;
        if (index == -1) {
            return "";
        } else {
            return filename.substring(index + 1);
        }
    }

    public static File tryResolveLnk(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return file;
        }
        try {
            if (FileUtils.getExtension(file).equals("lnk")) {
                WindowsShortcut shortcut = new WindowsShortcut(file);
                return new File(shortcut.getRealFilename());
            }
        } catch (IOException | ParseException ignored) {
        }
        return file;
    }

    public static String tryResolveLnk(String path) {
        if (path == null) {
            return null;
        }
        return tryResolveLnk(new File(path)).getAbsolutePath();
    }

    public static String getNameNoExtension(String name, String[] possibleExtensions) {
        if (name == null) {
            throw new IllegalArgumentException("Name was null");
        }
        if (possibleExtensions == null) {
            throw new IllegalArgumentException("PossibleExtensions is null");
        }
        boolean endsWith = false;
        for (String ext : possibleExtensions) {
            if (ext != null) {
                endsWith = endsWith
                        || name.toLowerCase().endsWith(ext
                        .replace("*", "")
                        .replace(".", "")
                        .toLowerCase()
                );
            }
        }
        int dotIndex = name.lastIndexOf('.');
        if (name.length() - dotIndex < 5 && endsWith) { //can have up to 4 letters of ext
            return getNameNoExtension(name.substring(0, dotIndex), possibleExtensions);
        }
        return name;
    }
}
