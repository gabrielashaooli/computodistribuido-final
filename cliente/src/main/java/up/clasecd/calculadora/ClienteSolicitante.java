/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.ClienteSolicitante to edit this template
 */
package up.clasecd.calculadora;


import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClienteSolicitante {
    private static final Logger LOGGER = LogManager.getLogger(ClienteSolicitante.class);
    private static Socket socket;
    private static final byte[] HUELLA = generarHuella();
    private static final Random RANDOM = new Random();
    private static final int MIN_ACKS = 2;

    // Colas por tipo de servicio (1 = suma, 2 = resta, 3 = multiplicar, 4 = dividir)
    private static final Map<Short, BlockingQueue<Mensaje>> colasPorServicio = new ConcurrentHashMap<>();
    // Contador de ACKs por folio
    private static final Map<String, Integer> acusesPorFolio = new ConcurrentHashMap<>();

    static {
        for (short i = 1; i <= 4; i++) {
            colasPorServicio.put(i, new LinkedBlockingQueue<>());
        }
    }

    private static byte[] generarHuella() {
        byte[] huella = new byte[8];
        new Random().nextBytes(huella);
        return huella;
    }

    private static void establecerConexion() throws Exception {
        for (int i = Constantes.PUERTO_INICIAL; i <= Constantes.PUERTO_FINAL; i++) {
            try {
                socket = new Socket(Constantes.LOCALHOST, i);
                LOGGER.info("Conectado con nodo en puerto: " + i);
                Mensaje m = new Mensaje();
                m.setDestinatario((short) 0);
                m.setHuella(HUELLA);
                m.setNumeroServicio((short) 0); // Identificación
                m.setEvento("".getBytes());
                m.setDatos("C".getBytes()); // Identificador como Célula
                DecoderEncoder.escribir(socket, m);
                return;
            } catch (IOException ex) {
                LOGGER.warn("No se pudo conectar con puerto: " + i);
            }
        }
        throw new Exception("No se encontró nodo disponible.");
    }

    public static void main(String[] args) throws Exception {
        establecerConexion();

        // 📨 Hilo de envío: recorre las colas y manda los mensajes
        new Thread(() -> {
            while (true) {
                for (short servicio = 1; servicio <= 4; servicio++) {
                    BlockingQueue<Mensaje> cola = colasPorServicio.get(servicio);
                    Mensaje m = cola.poll();
                    if (m != null) {
                        try {
                            DecoderEncoder.escribir(socket, m);
                            LOGGER.info("Mensaje enviado: " + m);
                        } catch (IOException e) {
                            LOGGER.error("Error al enviar mensaje", e);
                        }
                    }
                }

                try {
                    Thread.sleep(500); // Intervalo de chequeo
                } catch (InterruptedException ignored) {}
            }
        }).start();

        // 🎧 Hilo de escucha: recibe ACKs y respuestas
        new Thread(() -> {
            while (true) {
                try {
                    Mensaje recibido = DecoderEncoder.leer(socket);
                    short tipo = recibido.getNumeroServicio();

                    if (tipo == 99) {
                        String folio = recibido.getFolio();
                        acusesPorFolio.merge(folio, 1, Integer::sum);
                        LOGGER.info("ACK recibido para folio: " + folio + " → total: " + acusesPorFolio.get(folio));
                    } else if (tipo == 5) {
                        LOGGER.info("Resultado recibido → Folio: " + recibido.getFolio() + ", Resultado: " + ByteBuffer.wrap(recibido.getDatos()).getInt());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error al recibir mensaje", e);
                    break;
                }
            }
        }).start();

        // 🧪 Hilo generador: crea solicitudes y las pone en las colas si hay suficientes ACKs
        new Thread(() -> {
            while (true) {
                short servicio = (short) (1 + RANDOM.nextInt(4)); // 1-4
                int a = RANDOM.nextInt(100);
                int b = RANDOM.nextInt(100);
                String folio = UUID.randomUUID().toString().substring(0, 8);

                Mensaje m = new Mensaje();
                m.setDestinatario((short) 0); // nodo
                m.setHuella(HUELLA);
                m.setNumeroServicio(servicio);
                m.setEvento(folio.getBytes());
                m.setFolio(folio);

                byte[] datos = ByteBuffer.allocate(8)
                        .putInt(a)
                        .putInt(b)
                        .array();
                m.setDatos(datos);

                // Simulamos espera de ACKs antes de enviar
                acusesPorFolio.put(folio, 0); // inicializamos

                while (acusesPorFolio.get(folio) < MIN_ACKS) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}
                }

                try {
                    colasPorServicio.get(servicio).put(m);
                    LOGGER.info("Mensaje encolado para servicio " + servicio + ": " + m);
                } catch (InterruptedException e) {
                    LOGGER.error("Error al poner mensaje en cola", e);
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }
}
