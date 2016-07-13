package avalanche.crash.model;

/**
 * This class provides properties that can be attached to a crash report via a custom alert view flow
 *
 */
public class CrashMetaData {
    private String mUserDescription;
    private String mUserEmail;
    private String mUserID;

    public String getUserDescription() {
        return mUserDescription;
    }

    public void setUserDescription(final String userDescription) {
        this.mUserDescription = userDescription;
    }

    public String getUserEmail() {
        return mUserEmail;
    }

    public void setUserEmail(final String userEmail) {
        this.mUserEmail = userEmail;
    }

    public String getUserID() {
        return mUserID;
    }

    public void setUserID(final String userID) {
        this.mUserID = userID;
    }

    @Override
    public String toString() {
        return "\n" + CrashMetaData.class.getSimpleName()
                + "\n" + "userDescription " + mUserDescription
                + "\n" + "userEmail       " + mUserEmail
                + "\n" + "userID          " + mUserID
                ;
    }
}
