package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.MiscUtils;
import com.hyperion.net.Message;
import com.hyperion.net.NetEP;

import org.json.JSONObject;

import java.net.Socket;

public class RCSocket extends NetEP {

    public Hardware hw;

    public RCSocket(Hardware hw) {
        super();
        this.hw = hw;
    }

    @Override
    public void init() throws Exception {
        sender = Message.Sender.RC;
        clientSocket = new Socket(Constants.getString("net.ip"), Constants.getInt("net.port"));
    }

    @Override
    protected void onConnected(Message msg) {
        netLog("Connected to device \"" + msg.sender + "\"");
    }

    @Override
    protected void onDisconnected(Message msg) {
        netLog("Disconnected from device \"" + msg.sender + "\"");
    }

    @Override
    protected void onConstantsUpdated(Message msg) throws Exception {
        netLog("Constants updated from device \"" + msg.sender + "\"");

        Constants.init(new JSONObject(msg.json));
        Constants.write();
        hw.initFiles();
    }

    @Override
    protected void onFieldEdited(Message msg) {
        netLog("Field edited from device \"" + msg.sender + "\"");

        MiscUtils.writeFieldEditsToFieldJSON(hw.fieldJSON, msg.json);
        Motion.init(hw);
    }

    //////////////// UNUSED ////////////////

    @Override
    protected void onMetricsUpdated(Message msg) {

    }
    @Override
    protected void onOpModeEnded(Message msg) {

    }

}
