package avalanche.base.utils;

public class Persistence {

    PersistenceListener mListener;
    private static Persistence sharedInstance = null;

    protected Persistence() {
    }

    public static Persistence getInstance() {
        if (sharedInstance == null) {
            sharedInstance = new Persistence();
        }
        return sharedInstance;
    }

    public void storeCrash(Object aCrash) {

        //TODO: empty implementation
        boolean success = true;

        if(mListener != null) {
            mListener.storingSuccessful(success);
        }

    }
    public void setListener(PersistenceListener mListener) {
        this.mListener = mListener;
    }
}
