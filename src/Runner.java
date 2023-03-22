import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Runner {

    public static Scanner teclado = new Scanner(System.in);
    public static PrintStream entrada ;
    public static PrintStream log;

    public static void main(String[] args) throws FileNotFoundException {
        log = new PrintStream(new FileOutputStream("log-indexacao.txt"), true);
        entrada = System.out;
        Map<String, Set<String>> indice = new HashMap<>();
        ExecutorService service = Executors.newFixedThreadPool(5);
        service.submit(new Thread(new LinkProcessor("https://pt.wikipedia.org/wiki/Campina_Grande", 0, service, indice, log)));

        while (true) {
            var texto = getString("O que deseja pes" +
                    "quisar? (ou SAIR para sair): ");

            if (texto.equals("SAIR")) break;

            var palavras = texto.split(" ");

            var linksBuscados = Arrays.asList(palavras)
                    .stream()
                    .map(x -> {
                        if (indice.containsKey(x)) {
                            return indice.get(x);
                        }
                        return new HashSet<>();
                    })
                    .flatMap(set -> set.stream())
                    .collect(Collectors.toSet());

            if (!linksBuscados.isEmpty()) {
                linksBuscados.forEach(x -> entrada.println(x));
            } else {
                System.out.println("Não tenho essa informação");
            }

        }

    }

    public static String getString(String msg) {
        entrada.println(msg);
        return teclado.nextLine().toUpperCase(Locale.ROOT);
    }
}
