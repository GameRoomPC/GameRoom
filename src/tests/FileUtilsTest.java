package tests;

import data.io.FileUtils;
import org.junit.Test;

import java.io.File;

/**
 * Created by LM on 26/10/2016.
 */
public class FileUtilsTest {

    @Test
    public final void testInitOrCreateFile(){
        File f = FileUtils.initOrCreateFile("test/testfile.jpg");
        assert (f.exists());
        f.delete();
    }
    @Test
    public final void testInitOrCreateFolder(){
        File f = FileUtils.initOrCreateFolder("test/testfolder");
        assert (f.exists());
        f.delete();
    }
    @Test
    public final void testRelativizePath(){
        File absoluteFile = new File("D:\\Downloads\\Compressed");
        File folder = new File("D:\\Downloads");
        assert(FileUtils.relativizePath(absoluteFile,folder).getPath().equals("Compressed"));
    }
    @Test
    public final void testClearFolder(){
        File f = FileUtils.initOrCreateFile("test/testToDelete1.jpg");
        File testFolder = FileUtils.initOrCreateFolder("test");
        FileUtils.clearFolder(testFolder);
        assert (testFolder.listFiles().length==0);
    }
}
