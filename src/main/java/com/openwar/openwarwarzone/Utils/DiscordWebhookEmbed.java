package com.openwar.openwarwarzone.Utils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.StringJoiner;


public class DiscordWebhookEmbed {

    public static void sendWebhook(String webhookUrl, int x, int z) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36");

        String jsonPayload = "{"
                + "\"content\": null,"
                + "\"embeds\": ["
                + "  {"
                + "    \"title\": \"SUPPLY - DROP\","
                + "    \"description\": \"Supply drop is comming right now in world Faction\","
                + "    \"color\": 13568265,"
                + "    \"fields\": ["
                + "      {"
                + "        \"name\": \"Coordinate:\","
                + "        \"value\": \"x: " + x + "\\nz: " + z + "\""
                + "      }"
                + "    ],"
                + "    \"thumbnail\": {"
                + "      \"url\": \"https://media.discordapp.net/attachments/1263073609287729192/1395406402268430377/aaaaa.png?ex=687a54fb&is=6879037b&hm=bb563831a7d7bdb9c4820c269d735bcc1ee17b281bae528d939477b1aed91d6f&=&format=webp&quality=lossless&width=985&height=479\""
                + "    }"
                + "  }"
                + "],"
                + "\"attachments\": []"
                + "}";

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == 204) {
            System.out.println("Embed envoyé avec succès !");
        } else {
            System.out.println("Erreur lors de l'envoi : " + responseCode);
        }

        connection.disconnect();
    }
    public static void sendWarzoneWebhook(String webhookUrl, String factionName, List<String> onlineMembers) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36");


        StringJoiner membersJoiner = new StringJoiner("\\n");
        for (String member : onlineMembers) {
            membersJoiner.add(member);
        }
        String membersString = membersJoiner.toString();

        String jsonPayload = "{"
                + "\"content\": null,"
                + "\"embeds\": ["
                + "  {"
                + "    \"title\": \"WARZONE CAPTURE BUILDING\","
                + "    \"description\": \"Warzone building as been captured!\","
                + "    \"color\": 451071,"
                + "    \"fields\": ["
                + "      {"
                + "        \"name\": \"Faction:\","
                + "        \"value\": \"" + escapeJson(factionName) + "\\nOnline members:\\n" + membersString + "\""
                + "      }"
                + "    ],"
                + "    \"thumbnail\": {"
                + "      \"url\": \"https://media.discordapp.net/attachments/1263073609287729192/1395411787876405268/2025-07-17_16.28.01.png?ex=687a59ff&is=6879087f&hm=731ab4546877a1ffbd57273daa230472e6014ccee58922598baa08c9f85a0723&=&format=webp&quality=lossless&width=1666&height=656\""
                + "    }"
                + "  }"
                + "],"
                + "\"attachments\": []"
                + "}";

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == 204) {
            System.out.println("Webhook Warzone envoyé avec succès !");
        } else {
            System.out.println("Erreur lors de l'envoi : " + responseCode);
        }

        connection.disconnect();
    }


    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}
