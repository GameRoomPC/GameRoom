package data.game.scanner;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 16/10/2017.
 */
public class ScanTask implements Callable {
    private Callable callable;
    private CountDownLatch latch;

    public ScanTask(GameScanner scanner, Callable callable){
        this.callable = callable;
        latch = new CountDownLatch(1);

        if(scanner != null){
            scanner.addLatch(latch);
        }
    }

    @Override
    public Object call() throws Exception {
        if(callable != null){
            try {
                callable.call();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        latch.countDown();
        return null;
    }


}
