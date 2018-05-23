package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Model;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Common Schema Part A extensions.
 */
public class Extensions implements Model {

    /**
     * Protocol property.
     */
    private static final String PROTOCOL = "protocol";

    /**
     * User property.
     */
    private static final String USER = "user";

    /**
     * Os property.
     */
    private static final String OS = "os";

    /**
     * App property.
     */
    private static final String APP = "app";

    /**
     * Net property.
     */
    private static final String NET = "net";

    /**
     * SDK property.
     */
    private static final String SDK = "sdk";

    /**
     * Location property.
     */
    private static final String LOCATION = "location";

    /**
     * Protocol extension.
     */
    private ProtocolExtension protocol;

    /**
     * User extension.
     */
    private UserExtension user;

    /**
     * Os extension.
     */
    private OsExtension os;

    /**
     * Application extension.
     */
    private AppExtension app;

    /**
     * Net extension.
     */
    private NetExtension net;

    /**
     * SDK extension.
     */
    private SdkExtension sdk;

    /**
     * Loc extension.
     */
    private LocationExtension location;

    /**
     * Get protocol extension.
     *
     * @return protocol extension.
     */
    public ProtocolExtension getProtocol() {
        return protocol;
    }

    /**
     * Set protocol extension.
     *
     * @param protocol protocol extension.
     */
    public void setProtocol(ProtocolExtension protocol) {
        this.protocol = protocol;
    }

    /**
     * Get user extension.
     *
     * @return user extension.
     */
    public UserExtension getUser() {
        return user;
    }

    /**
     * Set user extension.
     *
     * @param user user extension.
     */
    public void setUser(UserExtension user) {
        this.user = user;
    }

    /**
     * Get os extension.
     *
     * @return os extension.
     */
    public OsExtension getOs() {
        return os;
    }

    /**
     * Set os extension.
     *
     * @param os os extension.
     */
    public void setOs(OsExtension os) {
        this.os = os;
    }

    /**
     * Get app extension.
     *
     * @return app extension.
     */
    public AppExtension getApp() {
        return app;
    }

    /**
     * Set app extension.
     *
     * @param app app extension.
     */
    public void setApp(AppExtension app) {
        this.app = app;
    }

    /**
     * Get net extension.
     *
     * @return net extension.
     */
    public NetExtension getNet() {
        return net;
    }

    /**
     * Set net extension.
     *
     * @param net net extension.
     */
    public void setNet(NetExtension net) {
        this.net = net;
    }

    /**
     * Get SDK extension.
     *
     * @return SDK extension.
     */
    public SdkExtension getSdk() {
        return sdk;
    }

    /**
     * Set SDK extension.
     *
     * @param sdk SDK extension.
     */
    public void setSdk(SdkExtension sdk) {
        this.sdk = sdk;
    }

    /**
     * Get location extension.
     *
     * @return location extension.
     */
    public LocationExtension getLocation() {
        return location;
    }

    /**
     * Set location extension.
     *
     * @param location location extension.
     */
    public void setLocation(LocationExtension location) {
        this.location = location;
    }

    @Override
    public void read(JSONObject object) throws JSONException {

        /* Protocol. */
        if (object.has(PROTOCOL)) {
            ProtocolExtension protocol = new ProtocolExtension();
            protocol.read(object.getJSONObject(PROTOCOL));
            setProtocol(protocol);
        }

        /* User. */
        if (object.has(USER)) {
            UserExtension user = new UserExtension();
            user.read(object.getJSONObject(USER));
            setUser(user);
        }

        /* Os. */
        if (object.has(OS)) {
            OsExtension os = new OsExtension();
            os.read(object.getJSONObject(OS));
            setOs(os);
        }

        /* App. */
        if (object.has(APP)) {
            AppExtension app = new AppExtension();
            app.read(object.getJSONObject(APP));
            setApp(app);
        }

        /* Net. */
        if (object.has(NET)) {
            NetExtension net = new NetExtension();
            net.read(object.getJSONObject(NET));
            setNet(net);
        }

        /* SDK. */
        if (object.has(SDK)) {
            SdkExtension sdk = new SdkExtension();
            sdk.read(object.getJSONObject(SDK));
            setSdk(sdk);
        }

        /* Location. */
        if (object.has(LOCATION)) {
            LocationExtension loc = new LocationExtension();
            loc.read(object.getJSONObject(LOCATION));
            setLocation(loc);
        }
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {

        /* Protocol. */
        if (getProtocol() != null) {
            writer.key(PROTOCOL).object();
            getProtocol().write(writer);
            writer.endObject();
        }

        /* User. */
        if (getUser() != null) {
            writer.key(USER).object();
            getUser().write(writer);
            writer.endObject();
        }

        /* Os. */
        if (getOs() != null) {
            writer.key(OS).object();
            getOs().write(writer);
            writer.endObject();
        }

        /* App. */
        if (getApp() != null) {
            writer.key(APP).object();
            getApp().write(writer);
            writer.endObject();
        }

        /* Net. */
        if (getNet() != null) {
            writer.key(NET).object();
            getNet().write(writer);
            writer.endObject();
        }

        /* SDK. */
        if (getSdk() != null) {
            writer.key(SDK).object();
            getSdk().write(writer);
            writer.endObject();
        }

        /* Location. */
        if (getLocation() != null) {
            writer.key(LOCATION).object();
            getLocation().write(writer);
            writer.endObject();
        }
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Extensions that = (Extensions) o;

        if (protocol != null ? !protocol.equals(that.protocol) : that.protocol != null)
            return false;
        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        if (os != null ? !os.equals(that.os) : that.os != null) return false;
        if (app != null ? !app.equals(that.app) : that.app != null) return false;
        if (net != null ? !net.equals(that.net) : that.net != null) return false;
        if (sdk != null ? !sdk.equals(that.sdk) : that.sdk != null) return false;
        return location != null ? location.equals(that.location) : that.location == null;
    }

    @Override
    public int hashCode() {
        int result = protocol != null ? protocol.hashCode() : 0;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (os != null ? os.hashCode() : 0);
        result = 31 * result + (app != null ? app.hashCode() : 0);
        result = 31 * result + (net != null ? net.hashCode() : 0);
        result = 31 * result + (sdk != null ? sdk.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        return result;
    }
}
