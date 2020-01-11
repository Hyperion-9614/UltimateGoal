package com.hyperion.dashboard;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.hyperion.common.Constants;
import com.hyperion.common.Options;
import com.hyperion.common.Utils;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Runs dashboard server socket
 */
public class DashboardServer {

    public static SocketIOServer server;
    public static ArrayList<SocketIOClient> dashboardClients = new ArrayList<>();
    public static SocketIOClient rcClient;
    public static Constants constants;
    public static Options options;

    public static void main(String[] args) {
        constants = new Constants(new File(System.getProperty("user.dir") + "/HyperiLibs/src/main/res/data/constants.json"));
        options = new Options(new File(constants.RES_DATA_PREFIX + "/options.json"));

        try {
            Configuration configuration = new Configuration();
            constants.HOST_IP = InetAddress.getLocalHost().getHostAddress();
            constants.write();
            configuration.setHostname(constants.HOST_IP);
            configuration.setPort(constants.PORT);
            server = new SocketIOServer(configuration);

            server.addConnectListener(client -> {
                String address = client.getRemoteAddress().toString()
                                 .substring(0, client.getRemoteAddress().toString().indexOf(":"))
                                 .replace("/", "");
                String type;
                if (rcClient == null && address.equals(constants.RC_IP)) {
                    rcClient = client;
                    type = "RC";
                } else {
                    dashboardClients.add(client);
                    type = "UI";
                }
                Utils.printSocketLog(type, "SERVER", "connected", options);

                client.sendEvent("constantsUpdated", Utils.readDataJSON("constants", constants));
                Utils.printSocketLog("SERVER", type, "constantsUpdated", options);
                client.sendEvent("optionsUpdated", Utils.readDataJSON("options", constants));
                Utils.printSocketLog("SERVER", type, "optionsUpdated", options);
                client.sendEvent("configUpdated", Utils.readFile(new File(constants.RES_DATA_PREFIX + "/MainConfig.xml")));
                Utils.printSocketLog("SERVER", type, "configUpdated", options);
                client.sendEvent("dashboardUpdated", Utils.readDataJSON("dashboard", constants));
                Utils.printSocketLog("SERVER", type, "dashboardUpdated", options);
            });

            server.addDisconnectListener(client -> {
                String address = client.getRemoteAddress().toString()
                        .substring(0, client.getRemoteAddress().toString().indexOf(":"))
                        .replace("/", "");
                if (rcClient != null && address.equals(constants.RC_IP)) {
                    rcClient = null;
                    Utils.printSocketLog("RC", "SERVER", "disconnected", options);
                } else {
                    Utils.printSocketLog("UI", "SERVER", "disconnected", options);
                    dashboardClients.remove(client);
                }
            });

            server.addEventListener("dashboardUpdated", String.class, (client, data, ackRequest) -> {
                Utils.printSocketLog("UI", "SERVER", "dashboardUpdated", options);

                try {
                    for (SocketIOClient dashboardClient : dashboardClients) {
                        dashboardClient.sendEvent("dashboardUpdated", data);
                        Utils.printSocketLog("SERVER", "UI", "dashboardUpdated", options);
                    }
                    if (rcClient != null) {
                        rcClient.sendEvent("dashboardUpdated", data);
                        Utils.printSocketLog("SERVER", "RC", "dashboardUpdated", options);
                    }

                    Utils.writeDataJSON(data, "dashboard", constants);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            server.addEventListener("pathFound", String.class, (client, data, ackRequest) -> {
                for (SocketIOClient dashboardClient : dashboardClients) {
                    dashboardClient.sendEvent("pathFound", data);
                    Utils.printSocketLog("SERVER", "UI", "pathFound", options);
                }
            });

            server.addEventListener("pathCompleted", String.class, (client, data, ackRequest) -> {
                for (SocketIOClient dashboardClient : dashboardClients) {
                    dashboardClient.sendEvent("pathCompleted", data);
                    Utils.printSocketLog("SERVER", "UI", "pathCompleted", options);
                }
            });

            server.addEventListener("opModeEnded", String.class, (client, data, ackRequest) -> {
                for (SocketIOClient dashboardClient : dashboardClients) {
                    dashboardClient.sendEvent("opModeEnded", data);
                    Utils.printSocketLog("SERVER", "UI", "opModeEnded", options);
                }
            });

            server.addEventListener("unimetryUpdated", String.class, (client, data, ackRequest) -> {
                Utils.printSocketLog("RC", "SERVER", "unimetryUpdated", options);

                for (SocketIOClient dashboardClient : dashboardClients) {
                    dashboardClient.sendEvent("unimetryUpdated", data);
                    Utils.printSocketLog("SERVER", "UI", "unimetryUpdated", options);
                }
            });

            server.addEventListener("constantsUpdated", String.class, (client, data, ackRequest) -> {
                for (SocketIOClient dashboardClient : dashboardClients) {
                    dashboardClient.sendEvent("constantsUpdated", data);
                    Utils.printSocketLog("SERVER", "UI", "constantsUpdated", options);
                }
                if (rcClient != null) {
                    rcClient.sendEvent("constantsUpdated", data);
                    Utils.printSocketLog("SERVER", "RC", "constantsUpdated", options);
                }
            });

            server.addEventListener("configUpdated", String.class, (client, data, ackRequest) -> {
                for (SocketIOClient dashboardClient : dashboardClients) {
                    dashboardClient.sendEvent("configUpdated", data);
                    Utils.printSocketLog("SERVER", "UI", "configUpdated", options);
                }
                if (rcClient != null) {
                    rcClient.sendEvent("configUpdated", data);
                    Utils.printSocketLog("SERVER", "RC", "configUpdated", options);
                }
            });

            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
