package ada_assessment1;

/*
 * @author Kieran
 */
public class UniqueIdentifier {
    
    private int id;
    private static UniqueIdentifier singleton = null;
    
    private UniqueIdentifier() {
        this.id = 0;
    }
    
    public static UniqueIdentifier getInstance() {
        if (singleton == null) {
            synchronized(UniqueIdentifier.class) {
                if (singleton == null) {
                    singleton = new UniqueIdentifier();
                }
            }
        }
        return singleton;
    }
    
    public int getNewId() {
        this.id++;
        return this.id;
    }
}