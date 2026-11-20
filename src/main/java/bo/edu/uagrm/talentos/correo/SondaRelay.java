package bo.edu.uagrm.talentos.correo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class SondaRelay {
    private SondaRelay() {}

    public static boolean probar(String host, int puerto, String desde, String hasta) throws IOException {
        System.out.println("[RELAY] conectando a " + host + ":" + puerto);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, puerto), 8000);
            socket.setSoTimeout(8000);
            var entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            var salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            leer(entrada);
            String ehlo = enviar(salida, entrada, "EHLO verificacion.local");
            enviar(salida, entrada, "MAIL FROM:<" + desde + ">");
            String respuesta = enviar(salida, entrada, "RCPT TO:<" + hasta + ">");
            enviar(salida, entrada, "RSET");
            enviar(salida, entrada, "QUIT");

            boolean aceptado = respuesta.startsWith("250");
            boolean cram = ehlo.contains("AUTH CRAM-MD5");
            System.out.println("[RELAY] resultado: " + (aceptado ? "ACEPTADO" : "RECHAZADO")
                    + (cram ? " | el servidor solo anuncia AUTH CRAM-MD5" : ""));
            return aceptado;
        }
    }

    private static String enviar(PrintWriter salida, BufferedReader entrada, String linea) throws IOException {
        System.out.println("  > " + linea);
        salida.print(linea + "\r\n");
        salida.flush();
        return leer(entrada);
    }

    private static String leer(BufferedReader entrada) throws IOException {
        StringBuilder texto = new StringBuilder();
        while (true) {
            String linea = entrada.readLine();
            if (linea == null) break;
            System.out.println("  < " + linea);
            texto.append(linea).append(' ');
            if (linea.length() < 4 || linea.charAt(3) != '-') break;
        }
        return texto.toString().strip();
    }
}
