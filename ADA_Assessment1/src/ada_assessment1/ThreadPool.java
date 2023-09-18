package ada_assessment1;

import java.util.ArrayList;
import java.util.LinkedList;

/*
 * @author Kieran
 */
public class ThreadPool {
    
    //Used to signal the pool is alive or not
    private boolean alive;
    //The number of available threads
    private int availableThreads;
    //Collection to hold the threads
    private final ArrayList<CustomThread> pool;
    //Queue of tasks
    private final LinkedList<Runnable> tasksToDo;
    
    public ThreadPool(int initialSize) {
        //Throw exception if resize is less than 0
        if (initialSize < 0) {
            throw new IllegalArgumentException("Initial size need to be equal or more than 0!");
        }
        System.out.println("Pool Started With " + initialSize + " Threads!");
        this.alive = true;
        this.availableThreads = initialSize;
        //Initialise queue
        this.tasksToDo = new LinkedList<>();
        //Create custom thread array list
        this.pool = new ArrayList<>();
        //Create custom threads within list and start them
        for (int i = 0; i < initialSize; i++) {
            addThread();
        }
    }
    
    //Add thread to pool
    private void addThread() {
        CustomThread newCustomThread = new CustomThread();
        this.pool.add(newCustomThread);
        Thread thread = new Thread(newCustomThread);
        thread.start();
    }
    
    //Returns total num of threads
    public int getSize() {
        return this.pool.size();
    }
    
    //Returns the number of available threads
    public int getAvailable() {
        return this.availableThreads;
    }
    
    //Resizes the thread pool
    //When adding more threads they get added instantly
    //When removing threads they will finish the current task and then be removed
    public synchronized void resize(int newSize) {
        System.out.println("Thread Resized To: " + newSize);
        //Return if the current size is the same as the new size
        if (newSize == getSize()) {
            return;
        }
        //Throw exception if resize is less than 0
        if (newSize < 0) {
            throw new IllegalArgumentException("Resize need to be equal to 0 or more than 0!");
        }
        //Current thread size
        int currentSize = getSize();
        //Add more threads if more than current size
        if (newSize > getSize()) {
            for (int i = 0; i < (newSize - currentSize); i++) {
                addThread();
                this.availableThreads++;
            }
        } else { //Remove threads if less (equal is already returned)
            for (int i = 0; i < (currentSize - newSize); i++) {
                //Asks threads to stop when task is finished
                this.pool.remove(0).requestStop(2);
            }
        }
        //Notify threads of change
        notifyAll();
    }
    
    //Destroys pool when queue is empty
    public synchronized void destroyPool() {
        this.alive = false;
        //Current thread size
        int currentSize = getSize();
        //Request threads to stop when the queue is empty
        for (int i = 0; i < (currentSize); i++) {
            this.pool.remove(0).requestStop(1);
        }
        //Notify threads of change
        notifyAll();
        System.out.println("Pool Destroyed");
    }
    
    public synchronized boolean perform(Runnable task) {
        //If pool is destroyed then return false
        if (this.alive == false) {
            System.out.println("Failed to add task as pool is already destroyed!");
            return false;
        }
        //Add task to queue
        this.tasksToDo.add(task);
        System.out.println("Task Added");
        //Notify threads that a task was added
        notifyAll();
        return true;
    }
    
    //Used by the internal threads to grab a task
    private synchronized Runnable getTask() {
        //Poll for task (returns null if no task)
        Runnable task = this.tasksToDo.poll();
        //Print to console task info
        try {
            Task t = (Task) task;
            if (t != null){
                System.out.println("Running task: " + t.getId());
            }
        } catch (ClassCastException e) {
            System.out.println("Running task");
        }
        //If task is not null then remove 1 from available threads
        if (task != null) {
            this.availableThreads--;
        }
        //Returns the task or null
        return task;
    }
    
    //Used by the internal threads to change if they are available or not
    private synchronized void changeAvailableThreads(int value) {
        this.availableThreads = this.availableThreads + value;
    }
    
    //Used by the internal threads to request a wait so the lock
    //is on the thread pool class
    private synchronized void requestWait() {
        try {
            wait();
        } catch (InterruptedException ex) {}
    }
    
    //Internal thread class
    class CustomThread implements Runnable {
        //0 = no stop requested
        //1 = stop when all tasks in the queue are done
        //2 = stop when current task is done and do not help with clearing the queue
        private int requestStopLevel;

        public CustomThread() {
            this.requestStopLevel = 0;
        }
        
        @Override
        public void run() {
            while (true) {
                //Reference to task to be run (can be null)
                Runnable task = null;
                //Try to get a task but only if not currently requested a level 2 stop
                if (this.requestStopLevel != 2) {
                    task = getTask();
                }
                
                //Break loop but only if the task queue is empty (get task will
                //return null if so) and stop is requested
                boolean requestBreak = false;
                switch (this.requestStopLevel){
                    case 1: //Stop when all tasks in queue are finished
                        if (task == null) {
                            requestBreak = true;
                        }
                        break;
                    case 2: //Stop when current task is finished
                        requestBreak = true;
                        break;
                    default: //Stop NOT requested (stop request level 0)
                        //Make thread wait if queue is empty
                        if (task == null) {
                            requestWait();
                        }
                        break;
                }
                
                //Break loop if requested to
                if (requestBreak == true) {
                    changeAvailableThreads(-1); //Removes 1
                    break;
                }
                
                //Run thread
                if (task != null) {
                    task.run();
                    System.out.println("Task finished");
                    changeAvailableThreads(1); //Adds 1
                }
            }
            System.out.println(Thread.currentThread().getName() + " - Thread Stopped");
        }
        
        //Requests thread to stop
        private synchronized void requestStop(int level) {
            System.out.println("Stop Requested");
            this.requestStopLevel = level;
        }
    }
}