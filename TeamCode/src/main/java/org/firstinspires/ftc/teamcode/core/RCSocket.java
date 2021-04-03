package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.MiscUtils;
import com.hyperion.net.Message;
import com.hyperion.net.NetEP;

import org.json.JSONObject;

import java.net.Socket;

public class RCSocket extends NetEP {

    public Gerald gerald;

    public RCSocket(Gerald gerald) {
        super();
        this.gerald = gerald;
    }

    @Override
    public void init() throws Exception {
        sender = Message.Sender.RC;
        clientSocket = new Socket(Constants.getString("net.dbIP"), Constants.getInt("net.port"));
    }

    @Override
    protected void onConnected(Message msg) {
        String remote = clientSocket.getRemoteSocketAddress().toString();
        netLog(LogLevel.INFO, "Connected to \"" + msg.sender + "\" [IP: " + remote.substring(remote.indexOf('/') + 1).trim() + "]");
    }

    @Override
    protected void onDisconnected(Message msg) {
        String remote = clientSocket.getRemoteSocketAddress().toString();
        netLog(LogLevel.INFO, "Disconnected from \"" + msg.sender + "\" [IP: " + remote.substring(remote.indexOf('/') + 1).trim() + "]");
    }

    @Override
    protected void onConstantsUpdated(Message msg) throws Exception {
        netLog(LogLevel.INFO, "Constants updated from \"" + msg.sender + "\"");

        Constants.init(new JSONObject(msg.json));
        Constants.write();

        Apndg.init(gerald);
        Motion.init(gerald);
    }

    @Override
    protected void onFieldEdited(Message msg) {
        netLog(LogLevel.INFO, "Field edited from \"" + msg.sender + "\"");

        MiscUtils.writeFieldEditsToFieldJSON(gerald.fieldJSON, msg.json);
        Motion.init(gerald);
    }

    @Override
    protected void onSignal(Message msg) {
        // TODO: opMode started/stopped from dashboard
    }

    //////////////// UNUSED ////////////////

    @Override
    protected void onMetricsUpdated(Message msg) {

    }
    @Override
    protected void onOpModeEnded(Message msg) {

    }

}
