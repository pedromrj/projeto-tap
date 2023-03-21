import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Runner {

    public static Scanner teclado = new Scanner(System.in);
    public static PrintStream input ;
    public static PrintStream log;

    public static void main(String[] args) throws FileNotFoundException {
        log = new PrintStream(new FileOutputStream("log-indexacao.txt"), true);
        input = System.out;
        Map<String, Set<String>> indice = new HashMap<>();
        ExecutorService service = Executors.newFixedThreadPool(1000);
        service.submit(new Thread(new LinkProcessor("https://pt.wikipedia.org/wiki/Campina_Grande", 0, service, indice, log)));

        while (true) {
            var texto = getString("O que deseja pesquisar? (ou SAIR para sair): ");
            if (texto.equals("SAIR")) break;

            if (indice.containsKey(texto)) {
                indice.get(texto)
                        .forEach(x -> input.println(x));
            } else {
                System.out.println("Não tenho essa informação");
            }

        }

    }

    public static String getString(String msg) {
        input.println(msg);
        return teclado.nextLine().toUpperCase(Locale.ROOT);
    }
}
