package im.actor.model.api.rpc;
/*
 *  Generated by the Actor API Scheme generator.  DO NOT EDIT!
 */

import im.actor.model.droidkit.bser.Bser;
import im.actor.model.droidkit.bser.BserParser;
import im.actor.model.droidkit.bser.BserObject;
import im.actor.model.droidkit.bser.BserValues;
import im.actor.model.droidkit.bser.BserWriter;
import im.actor.model.droidkit.bser.DataInput;
import im.actor.model.droidkit.bser.DataOutput;
import static im.actor.model.droidkit.bser.Utils.*;
import java.io.IOException;
import im.actor.model.network.parser.*;
import java.util.List;
import java.util.ArrayList;
import im.actor.model.api.*;

public class RequestSendAuthCall extends Request<ResponseVoid> {

    public static final int HEADER = 0x5a;
    public static RequestSendAuthCall fromBytes(byte[] data) throws IOException {
        return Bser.parse(new RequestSendAuthCall(), data);
    }

    private long phoneNumber;
    private String smsHash;
    private int appId;
    private String apiKey;

    public RequestSendAuthCall(long phoneNumber, String smsHash, int appId, String apiKey) {
        this.phoneNumber = phoneNumber;
        this.smsHash = smsHash;
        this.appId = appId;
        this.apiKey = apiKey;
    }

    public RequestSendAuthCall() {

    }

    public long getPhoneNumber() {
        return this.phoneNumber;
    }

    public String getSmsHash() {
        return this.smsHash;
    }

    public int getAppId() {
        return this.appId;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    @Override
    public void parse(BserValues values) throws IOException {
        this.phoneNumber = values.getLong(1);
        this.smsHash = values.getString(2);
        this.appId = values.getInt(3);
        this.apiKey = values.getString(4);
    }

    @Override
    public void serialize(BserWriter writer) throws IOException {
        writer.writeLong(1, this.phoneNumber);
        if (this.smsHash == null) {
            throw new IOException();
        }
        writer.writeString(2, this.smsHash);
        writer.writeInt(3, this.appId);
        if (this.apiKey == null) {
            throw new IOException();
        }
        writer.writeString(4, this.apiKey);
    }

    @Override
    public String toString() {
        String res = "rpc SendAuthCall{";
        res += "phoneNumber=" + this.phoneNumber;
        res += "}";
        return res;
    }

    @Override
    public int getHeaderKey() {
        return HEADER;
    }
}
