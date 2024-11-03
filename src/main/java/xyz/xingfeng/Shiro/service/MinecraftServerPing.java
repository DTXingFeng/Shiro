package xyz.xingfeng.Shiro.service;

import com.mikuac.shiro.dto.action.response.GetStatusResp;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MinecraftServerPing {
    private static final int HANDSHAKE = 0x00;
    private static final int STATUS_REQUEST = 0x00;
    private MinecraftServerStatus minecraftServerStatus = new MinecraftServerStatus();

    public MinecraftServerPing(String serverAddress,int serverPort) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(serverAddress, serverPort), 10000);

        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        // 构造握手数据包
        ByteArrayOutputStream handshakePacket = new ByteArrayOutputStream();
        DataOutputStream handshakeData = new DataOutputStream(handshakePacket);

        handshakeData.writeByte(HANDSHAKE); // Packet ID for handshake
        writeVarInt(handshakeData, 47); // Protocol version (47 = 1.8)
        writeVarInt(handshakeData, serverAddress.length()); // Server address length
        handshakeData.writeBytes(serverAddress); // Server address
        handshakeData.writeShort(serverPort); // Server port
        writeVarInt(handshakeData, 1); // State (1 for status)

        sendPacket(out, handshakePacket.toByteArray()); // 发送握手包

        // 发送状态请求包
        ByteArrayOutputStream statusRequestPacket = new ByteArrayOutputStream();
        DataOutputStream statusRequestData = new DataOutputStream(statusRequestPacket);
        statusRequestData.writeByte(STATUS_REQUEST); // Packet ID for status request

        sendPacket(out, statusRequestPacket.toByteArray());

        // 读取服务器响应
        int packetLength = readVarInt(in); // Packet length
        int packetId = readVarInt(in); // Packet ID

        if (packetId == 0x00) { // Status response
            int jsonLength = readVarInt(in); // Length of the JSON string
            byte[] jsonData = new byte[jsonLength];
            in.readFully(jsonData); // Read the JSON data

            String jsonResponse = new String(jsonData, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(jsonResponse);
            minecraftServerStatus.setVersion(jsonObject.getJSONObject("version").getString("name"));
            minecraftServerStatus.setProtocol(jsonObject.getJSONObject("version").getInt("protocol"));
            minecraftServerStatus.setMax(jsonObject.getJSONObject("players").getInt("max"));
            minecraftServerStatus.setOnline(jsonObject.getJSONObject("players").getInt("online"));

            if ("class java.lang.String".equals(getType(jsonObject.get("description")))){
                minecraftServerStatus.setDescription(filterDescription(jsonObject.getString("description")));
            }else {
                minecraftServerStatus.setDescription(filterDescription(jsonObject.getJSONObject("description").getString("text")));
            }
            MinecraftServerStatus.players = new ArrayList<>();
            if (jsonObject.getJSONObject("players").has("sample")) {
                //在线玩家
                JSONArray sample = jsonObject.getJSONObject("players").getJSONArray("sample");
                for (int i = 0; i < sample.length(); i++) {
                    minecraftServerStatus.addPlayers(sample.getJSONObject(i).getString("name"));
                }
            }


        }

        socket.close();
    }

    public static String getType(Object obj){
        return obj.getClass().toString();
    }
    // 正则表达式，匹配 Minecraft 颜色代码
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("§[0-9A-FK-ORa-fk-or]");

    public static String filterDescription(String description) {
        // 移除颜色代码
        String filteredDescription = COLOR_CODE_PATTERN.matcher(description).replaceAll("");

        // 去除首尾空格
        return filteredDescription.trim();
    }
    private static void sendPacket(DataOutputStream out, byte[] data) throws IOException {
        writeVarInt(out, data.length);
        out.write(data);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0x7F);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) {
                throw new IOException("VarInt 太大");
            }
        } while ((read & 0x80) != 0);

        return result;
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & 0xFFFFFF80) != 0L) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value & 0x7F);
    }

    public MinecraftServerStatus query() {
        return minecraftServerStatus;
    }

    public class MinecraftServerStatus{

        private static String description;
        private static int online;
        private static int max;
        private static String Version;
        private static int Protocol;

        private static List<String> players = new ArrayList<>();

        public List<String> getPlayers() {
            return players;
        }

        public void addPlayers(String player) {
            players.add(player);
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            MinecraftServerStatus.description = description;
        }

        public int getOnline() {
            return online;
        }

        public void setOnline(int online) {
            MinecraftServerStatus.online = online;
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            MinecraftServerStatus.max = max;
        }

        public String getVersion() {
            return Version;
        }

        public void setVersion(String version) {
            Version = version;
        }

        public int getProtocol() {
            return Protocol;
        }

        public void setProtocol(int protocol) {
            Protocol = protocol;
        }
    }
}
