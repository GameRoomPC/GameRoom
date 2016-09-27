package data;

import ui.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Created by LM on 26/09/2016.
 */
public class FileUtils {

    public static File initOrCreateFolder(File f) {
        File file = f;
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public static File initOrCreateFolder(String f) {
        return initOrCreateFolder(new File(f));
    }

    public static File initOrCreateFile(File f) {
        File file = f;
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public static File initOrCreateFile(String f) {
        return initOrCreateFile(new File(f));
    }

    public static void moveToFolder(File src, File target) {
        if(src.exists() && ! new File(target.getAbsolutePath()+File.separator+src.getName()).exists()) {
            if (target.isDirectory()) {
                target.mkdirs();
            } else {
                target.getParentFile().mkdirs();
            }
            File movedFile = new File(target.getAbsolutePath() + File.separator + src.getName());
            if (src.isDirectory()) {
                movedFile.mkdirs();
            } else {
                movedFile.getParentFile().mkdirs();
            }


            if (src.exists()) {
                if (src.isDirectory() && src.listFiles().length > 0) {
                    for (File subFile : src.listFiles()) {
                        moveToFolder(subFile, new File(target.getAbsolutePath() + File.separator + src.getName()));
                    }
                } else {
                    try {
                        Files.move(src.toPath(), new File(target.toPath() + File.separator + src.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        Main.LOGGER.error("Error while moving file : " + src.getAbsolutePath() + ", copying it");
                        try {
                            Files.copy(src.toPath(), new File(target.toPath() + File.separator + src.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
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

}
