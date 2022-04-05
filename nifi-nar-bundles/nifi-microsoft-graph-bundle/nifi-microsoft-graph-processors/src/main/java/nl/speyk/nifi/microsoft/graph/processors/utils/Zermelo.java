package nl.speyk.nifi.microsoft.graph.processors.utils;

import okhttp3.*;

import java.io.IOException;

public class Zermelo {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf8");
    //TODO: We a fixed url, must be parameterized
    private static final String url = "https://v22-03-speyk.zportal.nl:443/api/v3/appointments/";
    private final OkHttpClient client = new OkHttpClient();

    private String appointmentJson(long id, String onlineLocationUrl) {
        return "{\"id\":  \"" + id + "\", \"onlineLocationUrl\": \"" + onlineLocationUrl + "\"}";
    }

    public String put(long appointmentId, String onlineLocationUrl) throws IOException {
        RequestBody body = RequestBody.create(appointmentJson(appointmentId, onlineLocationUrl), JSON);
        Request request = new Request.Builder()
                .url(url + appointmentId)
                .put(body)
                .header("Authorization", "Bearer k3lac7tq4epaksamh56db9tmf8")
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }
}
