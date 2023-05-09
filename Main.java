import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {


  public static void main(String... args) throws URISyntaxException, IOException, InterruptedException {
//    String url = "http://found.ward.bay.wiki.org/json-schema.json";
    String url = "http://ward.dojo.fed.wiki/dojo-practice-yearbooks.json";
    if (args.length > 0) {
      url = args[1];
    }

    var scanner = new Scanner(System.in);

    while (true) {
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

      System.out.println("# title");
      System.out.println(result.title);
      System.out.println(result.context());
      System.out.println("");

      for (Item item : result.story) {
        System.out.println("# " + item.type);
        System.out.println(item.text);

        var cmd = scanner.nextLine();
        if (cmd.equals("exit")) {
          System.exit(0);
        } else if (cmd.equals("l")) {
          if (item.links().size() > 0) {
            var title = item.links().get(0);
            var slug = title.replaceAll("\\s", "-")
                    .replaceAll("[^A-Za-z0-9-]", "")
                    .toLowerCase(Locale.getDefault());

            url = String.format("http://ward.dojo.fed.wiki/%s.json", slug);
            break;
          } else {
            System.out.println("No link in this item");
          }
        }
      }
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Page {
    public String title;
    public List<Item> story;
    public List<Action> journal;

    public List<String> context() {
      List<String> sites = new ArrayList<String>();
      for (int i=journal.size()-1; i>=0; i--)
        if (journal.get(i).site != null && !sites.contains(journal.get(i).site))
          sites.add(journal.get(i).site);
      return sites;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Item {
    public String type;
    public String id;
    public String title;
    public String text;

    private static final Pattern linkPattern = Pattern.compile("\\[\\[(.*?)]]");

    public List<String> links() {
      var matcher = linkPattern.matcher(text);
      if (matcher.find()) {
        return List.of(matcher.group(1));
      }

      return List.of();
    }
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