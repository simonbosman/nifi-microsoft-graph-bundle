package nl.speyk.nifi.microsoft.graph.processors.utils;

import okhttp3.*;

import java.io.IOException;

public class Zermelo {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf8");
    final OkHttpClient client;
    private final String zermeloUrl;
    private final String bearerToken;

    private String appointmentJson(long id, String onlineLocationUrl) {
        return "{\"id\":  \"" + id + "\", \"onlineLocationUrl\": \"" + onlineLocationUrl + "\"}";
    }

    public Zermelo(String url, String bearer) {
        client = new OkHttpClient();
        zermeloUrl = url;
        bearerToken = bearer;
    }

    public String put(long appointmentId, String onlineLocationUrl) throws IOException {
        RequestBody body = RequestBody.create(appointmentJson(appointmentId, onlineLocationUrl), JSON);
        Request request = new Request.Builder()
                .url(zermeloUrl + appointmentId)
                .put(body)
                .header("Authorization", "Bearer " + bearerToken)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }
}
