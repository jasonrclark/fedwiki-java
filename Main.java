import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
  public static void main(String... args) throws URISyntaxException, IOException, InterruptedException {
    String url = "http://found.ward.bay.wiki.org/json-schema.json";
    if (args.length > 0) {
      url = args[1];
    }

    HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(url))
            .header("User-Agent", "fedwiki-java")
            .version(HttpClient.Version.HTTP_1_1)
            .GET()
            .build();

    HttpResponse<String> response = HttpClient
            .newBuilder()
            .build()
            .send(request, HttpResponse.BodyHandlers.ofString());

    var mapper = new ObjectMapper();

    var result = mapper.readValue(response.body(), Page.class);

    System.out.println(result.title);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Page {
    public String title;
    public List<Item> story;
    public List<Action> journal;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Item {
    public String type;
    public String id;
    public String title;
    public String text;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Action {
    public String type;
    public String id;
    public Long date;
    public Item item;
    public String site;
  }
}