package data.io;

import ui.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

/**
 * Created by LM on 26/09/2016.
 */
public class FileUtils {

    public static File initOrCreateFolder(File f) {
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }

    public static File initOrCreateFolder(String f) {
        return initOrCreateFolder(new File(f));
    }

    private static File initOrCreateFile(File f) {
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

    public static File newTempFile(String fileName) {
        if(Main.FILES_MAP == null ||Main.FILES_MAP.isEmpty()){
            throw new IllegalStateException("Cannot create tempfile : FILE_MAP is not initialized");
        }
        File tempFolder = Main.FILES_MAP.get("temp");
        if(tempFolder == null || !tempFolder.exists()){
            throw new IllegalStateException("Temp folder does not exist");
        }

        return initOrCreateFile(tempFolder.getAbsolutePath() + File.separator + fileName);
    }


    public static File initOrCreateFile(String f) {
        return initOrCreateFile(new File(f));
    }

    public static void moveToFolder(File src, File target) {
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
                    //delete empty folder afterwards
                    src.delete();
                } else if (src.isDirectory() && src.listFiles().length == 0) {
                    //delete empty folder
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

    public static void deleteFolder(File folder){
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
            }
            else break;
        }

        if (commonIndex == 0)
        {
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
        }
        else {
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

}
