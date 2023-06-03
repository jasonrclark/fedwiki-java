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
import java.util.regex.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {

  public static void main(String... args) throws URISyntaxException, IOException, InterruptedException {
    String slug = "dojo-practice-yearbooks";
    String context = "http://ward.dojo.fed.wiki/%s.json";
    if (args.length > 0) slug = args[1];
    Page result = fetch(context,slug);
    var scanner = new Scanner(System.in);
    var lineno = 0;

    while (!result.story.isEmpty()) {
      for (Item item : result.story) {
        item.println();
        var cmd = scanner.nextLine();
        lineno++;
        if (cmd.length() != 0) System.out.println(" <<" + String.valueOf(lineno) + " " + cmd + ">>");
        if (cmd.startsWith("e")) System.exit(0);
        if (cmd.startsWith("l")) {result = fetch(context,item.links().get(0)); break;}
        if (cmd.startsWith("t")) {
          // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/regex/Pattern.html
          Pattern want = Pattern.compile(cmd.split(" ")[1]);
          Matcher have = want.matcher(item.text);
          boolean pass = have.find();
          System.out.println(" <<" + (pass ? "pass" : "fail") + ">>");
          if (!pass) System.exit(1);
        }
      }
    }
  }

  static Page fetch(String context, String slug) throws URISyntaxException, IOException, InterruptedException  {
    String url = String.format(context, slug);
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
    if (response.statusCode() != 200) return new Page();
    var mapper = new ObjectMapper();
    Page result = mapper.readValue(response.body(), Page.class);
    System.out.println("");
    System.out.println(result.title);
    System.out.println(result.context());
    System.out.println("==========================================");
    Thread.sleep(100);
    return result;
  }



  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Page {
    public String title = "Empty";
    public List<Item> story = List.of();
    public List<Action> journal = List.of();

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
        var slug = matcher.group(1)
          .replaceAll("\\s", "-")
          .replaceAll("[^A-Za-z0-9-]", "")
          .toLowerCase(Locale.getDefault());
        return List.of(slug);
      }
      return List.of();
    }

    public void println() {
      System.out.println(text);
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