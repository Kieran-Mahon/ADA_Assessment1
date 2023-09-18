package ada_assessment1;

import java.util.LinkedHashSet;

/*
 * @author Kieran
 */
public abstract class Task<E, F> implements Runnable {

    private final int id;
    //Listeners is a set so a listener can only be added once
    private final LinkedHashSet<TaskObserver<F>> listeners;
    
    public Task(E param) {
        this.id = UniqueIdentifier.getInstance().getNewId();
        this.listeners = new LinkedHashSet<>();
    }
    
    public int getId() {
        return this.id;
    }
    
    @Override
    public abstract void run();
    
    public void addListener(TaskObserver<F> o) {
        this.listeners.add(o);
    }
    
    public void removeListener(TaskObserver<F> o) {
        this.listeners.remove(o);
    }
    
    protected void notifyAll(F progress) {
        for (TaskObserver<F> listener : this.listeners) {
            listener.process(progress);
        }
    }
}