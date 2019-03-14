/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

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
     * Metadata extension.
     */
    private static final String METADATA = "metadata";

    /**
     * Protocol property.
     */
    private static final String PROTOCOL = "protocol";

    /**
     * User property.
     */
    private static final String USER = "user";

    /**
     * Device property.
     */
    private static final String DEVICE = "device";

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
     * Metadata extension.
     */
    private MetadataExtension metadata;

    /**
     * Protocol extension.
     */
    private ProtocolExtension protocol;

    /**
     * User extension.
     */
    private UserExtension user;

    /**
     * Device extension.
     */
    private DeviceExtension device;

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
     * Get metadata extension.
     *
     * @return metadata extension.
     */
    public MetadataExtension getMetadata() {
        return metadata;
    }

    /**
     * Set metadata extension.
     *
     * @param metadata metadata extension.
     */
    public void setMetadata(MetadataExtension metadata) {
        this.metadata = metadata;
    }

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
     * Get device extension.
     *
     * @return device extension.
     */
    public DeviceExtension getDevice() {
        return device;
    }

    /**
     * Set device extension.
     *
     * @param device device extension.
     */
    public void setDevice(DeviceExtension device) {
        this.device = device;
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
    @SuppressWarnings("WeakerAccess")
    public LocExtension getLoc() {
        return loc;
    }

    /**
     * Set loc extension.
     *
     * @param loc loc extension.
     */
    @SuppressWarnings("WeakerAccess")
    public void setLoc(LocExtension loc) {
        this.loc = loc;
    }

    @Override
    public void read(JSONObject object) throws JSONException {

        /* Metadata. */
        if (object.has(METADATA)) {
            MetadataExtension metadata = new MetadataExtension();
            metadata.read(object.getJSONObject(METADATA));
            setMetadata(metadata);
        }

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

        /* Device. */
        if (object.has(DEVICE)) {
            DeviceExtension device = new DeviceExtension();
            device.read(object.getJSONObject(DEVICE));
            setDevice(device);
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

        /* Loc. */
        if (object.has(LOC)) {
            LocExtension loc = new LocExtension();
            loc.read(object.getJSONObject(LOC));
            setLoc(loc);
        }
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {

        /* Metadata. */
        if (getMetadata() != null) {
            writer.key(METADATA).object();
            getMetadata().write(writer);
            writer.endObject();
        }

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

        /* Device. */
        if (getDevice() != null) {
            writer.key(DEVICE).object();
            getDevice().write(writer);
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

        /* Loc. */
        if (getLoc() != null) {
            writer.key(LOC).object();
            getLoc().write(writer);
            writer.endObject();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Extensions that = (Extensions) o;

        if (metadata != null ? !metadata.equals(that.metadata) : that.metadata != null)
            return false;
        if (protocol != null ? !protocol.equals(that.protocol) : that.protocol != null)
            return false;
        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        if (device != null ? !device.equals(that.device) : that.device != null) return false;
        if (os != null ? !os.equals(that.os) : that.os != null) return false;
        if (app != null ? !app.equals(that.app) : that.app != null) return false;
        if (net != null ? !net.equals(that.net) : that.net != null) return false;
        if (sdk != null ? !sdk.equals(that.sdk) : that.sdk != null) return false;
        return loc != null ? loc.equals(that.loc) : that.loc == null;
    }

    @Override
    public int hashCode() {
        int result = metadata != null ? metadata.hashCode() : 0;
        result = 31 * result + (protocol != null ? protocol.hashCode() : 0);
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (device != null ? device.hashCode() : 0);
        result = 31 * result + (os != null ? os.hashCode() : 0);
        result = 31 * result + (app != null ? app.hashCode() : 0);
        result = 31 * result + (net != null ? net.hashCode() : 0);
        result = 31 * result + (sdk != null ? sdk.hashCode() : 0);
        result = 31 * result + (loc != null ? loc.hashCode() : 0);
        return result;
    }
}
