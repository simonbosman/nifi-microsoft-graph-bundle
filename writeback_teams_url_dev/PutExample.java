import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PutExample {
  public static final MediaType JSON = MediaType.get("application/json; charset=utf8");

  final OkHttpClient client = new OkHttpClient();

  String put(String url, String json) throws IOException {
    RequestBody body = RequestBody.create(json, JSON);
    Request request = new Request.Builder()
      .url(url)
      .put(body)
      .header("Authorization", "Bearer k3lac7tq4epaksamh56db9tmf8")
      .build();
    try (Response response = client.newCall(request).execute()) {
      return response.body().string();
    }
  }

  String appointmentJson(long id, String onlineLocationUrl) {
    return "{\"id\":  \"" + id + "\", \"onlineLocationUrl\": \"" + onlineLocationUrl + "\"}";
  }

  public static void main(String[] args) throws IOException {
    PutExample example = new PutExample();
    String json = example.appointmentJson(1960300, "http://www.test.org");
    System.out.println(json);
    String response = example.put("https://v22-03-speyk.zportal.nl:443/api/v3/appointments/1960300", json);
    System.out.println(response);
  }
}
