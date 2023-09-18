package ada_assessment1;

/*
 * @author Kieran
 */
public interface TaskObserver<F> {
    public void process(F progress);
}