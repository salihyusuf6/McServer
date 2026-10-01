package com.Bannan.bannanDefaultLobby;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import org.bukkit.entity.Player;

public class Network {

    public static void SendPlayerToServer(Player player, String serverName){
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        try {
            dataOut.writeUTF("Connect");
            dataOut.writeUTF(serverName);
        }
        catch (IOException e){
            BannanDefaultLobby.MyPlugin.getLogger().log(Level.SEVERE,"Failed to build proxy Connect payload",e);
            return;
        }
        player.sendPluginMessage(BannanDefaultLobby.MyPlugin,"BungeeCord",byteOut.toByteArray());
    }

    public static String PlayerCheckEvent(String name,String url) throws Exception{
        HttpClient client = HttpClient.newHttpClient();
        String body =  String.format("{\"Name\":\"%s\",\"Secret\":\"TEMP\"}",name);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
    public static String LoginCheckEvent(String name, String password, String url) throws Exception{
        HttpClient client = HttpClient.newHttpClient();
        String body =  String.format("{\"Name\":\"%s\",\"Password\":\"%s\",\"Secret\":\"TEMP\"}",name,password);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
