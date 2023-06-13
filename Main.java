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

// I N T E R P R E T E R

public class Main {
  // https://stackoverflow.com/questions/5762491/how-to-print-color-in-console-using-system-out-println
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

  // commands
    static Scanner scanner = new Scanner(System.in);
    static int lineno = 0;

  // pages
    static String slug = "welcome-visitors";
    static String origin = "ward.dojo.fed.wiki";

  // lineup
    static List<Panel> lineup = new ArrayList<>();


  public static void main(String... args) throws URISyntaxException, IOException, InterruptedException {
    if (args.length > 0) origin = args[1];
    lineup.add(Panel.load(origin,slug));
    String shown = null;

    while (true) {
      Panel panel = lineup.get(lineup.size()-1);
      Item item = panel.item();
      if(!item.text.equals(shown)) {item.println(); shown = item.text;}
      var cmd = nextLine();
      if (cmd.startsWith("e")) System.exit(0);
      if (cmd.startsWith("l")) {lineup.add(Panel.load(panel.context(),panel.link()));}
      if (cmd.startsWith("t")) test(cmd,item);
      if (cmd.startsWith("f")) find(cmd);
      if (cmd.startsWith("n")) panel.next();
    }
  }

// H E L P E R S


  static void log(String msg) {
    System.out.println(ANSI_CYAN + " <<" + msg + ">>" + ANSI_RESET);
  }

  static void debug(String msg) {
    System.out.println(ANSI_YELLOW + msg + ANSI_RESET);
  }

  static void trouble(String msg) {
    System.out.println(ANSI_RED + " <<" + msg + ">>" + ANSI_RESET);
    System.exit(1);
  }

  static String nextLine() {
    var cmd = scanner.nextLine();
    lineno++;
    if (cmd.length() != 0) log(String.valueOf(lineno) + " " + cmd);
    return cmd;
  }

  static void test (String cmd, Item item) {
    // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/regex/Pattern.html
    Pattern want = Pattern.compile(cmd.split(" ")[1]);
    Matcher have = want.matcher(item.text);
    boolean pass = have.find();
    if (!pass) trouble("no match");
  }

  static void find (String cmd) {
    var panel = lineup.get(lineup.size()-1);
    Pattern want = Pattern.compile(cmd.split(" ")[1]);
    int last = panel.itemno;
    while(panel.next() != last) {
      Matcher have = want.matcher(panel.item().text);
      if(have.find()) return;
    }
    trouble("not found");
  }

  static Page fetch(String url) throws URISyntaxException, IOException, InterruptedException  {
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
    var code = response.statusCode();
    if (code == 200) {
      var mapper = new ObjectMapper();
      Page result = mapper.readValue(response.body(), Page.class);
      System.out.println("");
      for(Panel each : lineup) {System.out.println(each.page.title);}
      System.out.println(result.title);
      System.out.println("==========================================");
      return result;
    }
    else {
      return null;
    }
  }


// R U N T I M E

  private static class Panel {
    public String site;
    public String slug;
    public Page page;
    public int itemno;

    public Panel(String site, String slug) {
      this.site = site;
      this.slug = slug;
    }

    public static Panel load(String site, String slug) throws URISyntaxException, IOException, InterruptedException {
      var panel = new Panel(site,slug);
      String url = String.format("http://%s/%s.json", site, slug);
      panel.page = Main.fetch(url);
      if(panel.page == null) trouble("can't find page at expected site");
      panel.itemno = 0;
      return panel;
    }

    public static Panel load(List<String> context, String slug) throws URISyntaxException, IOException, InterruptedException {
      var site = origin;
      while(true) {
        String url = String.format("http://%s/%s.json", site, slug);
        var page = Main.fetch(url);
        if(page != null) {
          var panel = new Panel(site,slug);
          panel.page = page;
          panel.itemno = 0;
          return panel;
        }
        else {
          if(!context.isEmpty()) {
            site = context.get(0);
            context.remove(0);
          } else {
            trouble("can't find page in current context");
          }
        }
      }
    }

    public int next () {
      this.itemno = (this.itemno+1) % this.page.story.size();
      return this.itemno;
    }

    public Item item () {
      return this.page.story.get(this.itemno);
    }

    public List<String> context () {
      return this.page.context();
    }

    public String link () {
      return this.item().links().get(0);
    }
  }

// F E D E R A T I O N

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
    public String text = "";

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