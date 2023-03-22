import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkProcessor implements Runnable {

    private static final String REGEX_LINKS = "<a\\s+[^>]*href=\"([^\"]*)\"[^>]*>";
    private static Set<String> IGNORA_PALAVRAS = new HashSet<>(Arrays.asList(new String[]{"A", "O", "E", "AS", "OS", "UM", "UMA", "UNS", "UMAS", "COM", "ESTA", "FOI", "ESTAR"}));
    private static final Integer SUCESSO = 200;
    private String url;
    private String urlInicio;
    private int profundidade;
    private static Set<String> urlsProcessada = new HashSet<>();
    private Map<String, Set<String>> indice;
    private static final int MAXIMA_PROFUNDIDADE = 5;
    private static int contador = 0;
    private static PrintStream log;
    private ExecutorService executorService;

    public LinkProcessor(String url, int profundidade, ExecutorService executorService, Map<String, Set<String>> indice, PrintStream log) {
        this.url = url;
        this.profundidade = profundidade;
        this.urlInicio = url;
        this.executorService = executorService;
        this.indice = indice;
        this.log = log;
    }

    @Override
    public void run() {
        processUrl( url, profundidade);
    }

    private synchronized void processUrl(String url, int profundidade) {
        try {
            if (profundidade > MAXIMA_PROFUNDIDADE || urlsProcessada.contains(url)) {
                return;
            }
            var site = buscarSite();
            if (site.getResponseCode() == SUCESSO) {
                log.println("URL " + contador + ": " + url);
                var html = obterHTML(site);
                var texto = obterTexto(url);
                indexarPalavra(texto, url, indice);
                buscarLinks(html);
                //System.out.println(indice);
                contador++;
            }
        } catch (Exception e) {}
    }

    private static String normalizarPalavra(String palavra) {
        return Normalizer.normalize(palavra, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }

    private static void indexarPalavra(String texto, String url, Map<String, Set<String>> indexador)  {
        log.println("Indexando...");
        String[] palavras = texto.split(" ");

        Arrays.stream(palavras)
                .map((p) -> normalizarPalavra(p.toUpperCase(Locale.ROOT)))
                .filter((p) -> !ignorePalavras(p))
                .forEach((palavra) -> {
                    Set<String> indicePorPalavra = null;
                    //System.out.println(palavra);

                    if (indexador.containsKey(palavra)) {
                        indicePorPalavra = indexador.get(palavra);
                    } else {
                        indicePorPalavra = new HashSet<>();
                        indexador.put(palavra, indicePorPalavra);
                    }

                    indicePorPalavra.add(url);
                });
        log.println("Finalizado indexação.");
    }

    public static boolean ignorePalavras(String palavra) {
        if (palavra.length() <= 2) {
            return true;
        }
        return IGNORA_PALAVRAS.contains(palavra);
    }

    //UTILIZANDO JSOUP
    private synchronized String obterTexto(String url) throws IOException {
        Document document = Jsoup.connect(url).get();
        String texto = document.text();
        return texto;
    }

    private synchronized HttpURLConnection buscarSite() throws IOException {
        urlsProcessada.add(url);
        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setRequestMethod("GET");
        return conn;
    }

    private synchronized void buscarLinks(String html) {
        Pattern pattern = Pattern.compile(REGEX_LINKS);
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            String link = matcher.group(1);
            if (link.startsWith("/") || link.startsWith(urlInicio)) {
                if (link.startsWith("/")) {
                    link = urlInicio + link;
                }
            }
            executorService.submit(new Thread(new LinkProcessor(link, profundidade + 1, executorService, indice, log)));
        }
    }

    private synchronized String obterHTML(HttpURLConnection conn) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder html = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            html.append(inputLine);
        }
        in.close();
        return html.toString();
    }
}
