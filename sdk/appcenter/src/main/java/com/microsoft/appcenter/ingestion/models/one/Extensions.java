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
     * Loc property.
     */
    private static final String LOC = "loc";

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
    private LocExtension loc;

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
     * Get loc extension.
     *
     * @return loc extension.
     */
    public LocExtension getLoc() {
        return loc;
    }

    /**
     * Set loc extension.
     *
     * @param loc loc extension.
     */
    public void setLoc(LocExtension loc) {
        this.loc = loc;
    }

    @Override
    public void read(JSONObject object) throws JSONException {

        /* Protocol. */
        ProtocolExtension protocol = new ProtocolExtension();
        protocol.read(object.getJSONObject(PROTOCOL));
        setProtocol(protocol);

        /* User. */
        UserExtension user = new UserExtension();
        user.read(object.getJSONObject(USER));
        setUser(user);

        /* Os. */
        OsExtension os = new OsExtension();
        os.read(object.getJSONObject(OS));
        setOs(os);

        /* App. */
        AppExtension app = new AppExtension();
        app.read(object.getJSONObject(APP));
        setApp(app);

        /* Net. */
        NetExtension net = new NetExtension();
        net.read(object.getJSONObject(NET));
        setNet(net);

        /* SDK. */
        SdkExtension sdk = new SdkExtension();
        sdk.read(object.getJSONObject(SDK));
        setSdk(sdk);

        /* Loc. */
        LocExtension loc = new LocExtension();
        loc.read(object.getJSONObject(LOC));
        setLoc(loc);
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {

        /* Protocol. */
        writer.key(PROTOCOL).object();
        getProtocol().write(writer);
        writer.endObject();

        /* User. */
        writer.key(USER).object();
        getUser().write(writer);
        writer.endObject();

        /* Os. */
        writer.key(OS).object();
        getOs().write(writer);
        writer.endObject();

        /* App. */
        writer.key(APP).object();
        getApp().write(writer);
        writer.endObject();

        /* Net. */
        writer.key(NET).object();
        getNet().write(writer);
        writer.endObject();

        /* SDK. */
        writer.key(SDK).object();
        getSdk().write(writer);
        writer.endObject();

        /* Loc. */
        writer.key(LOC).object();
        getLoc().write(writer);
        writer.endObject();
    }
}
