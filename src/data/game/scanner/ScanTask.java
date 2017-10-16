package data.game.scanner;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/** This class is to be only used to performing scanning of games. Why couldn't we just use a {@link Callable} ? This
 * will add a latch to the given {@link GameScanner} in constructor that will countdown only when the task has finished
 * executing or failed with an exception (which we can not guarantee with a {@link Callable} adding itself {@link CountDownLatch}
 * to the {@link GameScanner}).
 * This allows the {@link GameScanner} to know when all the {@link ScanTask} he has created have been executed.
 * @author LM. Garret (admin@gameroom.me)
 * @date 16/10/2017.
 */
public class ScanTask implements Callable {
    private Callable callable;
    private CountDownLatch latch;

    /**
     * Creates a basic {@link ScanTask}
     * @param scanner the scanner that is creating this task. A latch will be added to it using {@link GameScanner#addLatch(CountDownLatch)}
     * @param callable the action to execute. May be null or throw exceptions, we don't mind
     */
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
