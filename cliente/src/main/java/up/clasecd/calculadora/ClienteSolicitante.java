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
    // Mapa para rastrear el folio del último mensaje enviado por cada servicio que está esperando ACKs
    private static final Map<Short, String> ultimoFolioEnviadoEsperandoAcks = new ConcurrentHashMap<>();

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

        // Hilo de envío: recorre las colas y manda los mensajes aplicando la lógica de MIN_ACKS
        new Thread(() -> {
            while (true) {
                for (short servicio = 1; servicio <= 4; servicio++) {
                    BlockingQueue<Mensaje> cola = colasPorServicio.get(servicio);
                    String folioPendienteServicio = ultimoFolioEnviadoEsperandoAcks.get(servicio);

                    // Verificar si hay un mensaje de este servicio esperando ACKs
                    if (folioPendienteServicio != null) {
                        int acksRecibidos = acusesPorFolio.getOrDefault(folioPendienteServicio, 0);
                        if (acksRecibidos < MIN_ACKS) {
                            // LOGGER.debug("Servicio " + servicio + " esperando ACKs para folio: " + folioPendienteServicio + " (" + acksRecibidos + "/" + MIN_ACKS + ")");
                            continue; // Saltar esta cola de servicio por ahora, pasar a la siguiente
                        } else {
                            // Suficientes ACKs recibidos para el mensaje anterior de este servicio
                            ultimoFolioEnviadoEsperandoAcks.remove(servicio);
                            // LOGGER.info("Servicio " + servicio + " liberado para enviar. Folio " + folioPendienteServicio + " recibió " + acksRecibidos + " ACKs.");
                        }
                    }

                    // Si no hay mensaje pendiente de ACKs para este servicio, o si ya se cumplieron, intentar enviar el siguiente.
                    Mensaje mensajeParaEnviar = cola.peek(); // Mirar el mensaje sin quitarlo de la cola

                    if (mensajeParaEnviar != null) {
                        try {
                            mensajeParaEnviar = cola.poll(); // Ahora sí, tomar el mensaje de la cola
                            if (mensajeParaEnviar != null) { // Doble chequeo por si acaso
                                DecoderEncoder.escribir(socket, mensajeParaEnviar);
                                LOGGER.info("Mensaje enviado: " + new String(mensajeParaEnviar.getEvento()) + " por servicio " + servicio);

                                // Registrar este mensaje como pendiente de ACKs para este servicio
                                // Asegurar que el folio esté en el mapa de acuses para ser contado
                                acusesPorFolio.putIfAbsent(new String(mensajeParaEnviar.getEvento()), 0);
                                ultimoFolioEnviadoEsperandoAcks.put(servicio, new String(mensajeParaEnviar.getEvento()));
                            }
                        } catch (IOException e) {
                            String folioDelError = (mensajeParaEnviar != null && mensajeParaEnviar.getEvento() != null) ? new String(mensajeParaEnviar.getEvento()) : "desconocido";
                            LOGGER.error("Error al enviar mensaje con folio: " + folioDelError, e);
                            // Considerar qué hacer con el mensaje: ¿re-encolarlo? ¿descartarlo?
                            // Por ahora, lo simple es que se pierde en este intento.
                            // Si lo re-encolas, cuidado con el orden y los bucles.
                        }
                    }
                } // Fin del bucle for servicios

                try {
                    Thread.sleep(200); // Intervalo de chequeo de colas y ACKs
                } catch (InterruptedException e) {
                    LOGGER.error("Hilo de envío interrumpido", e);
                    Thread.currentThread().interrupt(); // Restablecer el estado de interrupción
                }
            }
        }).start();

        // Hilo de escucha: recibe ACKs y respuestas
        new Thread(() -> {
            LOGGER.info("ClienteSolicitante: Hilo de escucha INICIADO en socket: " + socket); // Log de inicio
            while (true) {
                Mensaje recibido = null; // Para logging en caso de error antes de asignar
                try {
                    // Log ANTES de leer, para saber si el hilo está activo en el bucle
                    LOGGER.debug("ClienteSolicitante: Hilo de escucha esperando leer del socket...");
                    recibido = DecoderEncoder.leer(socket); // Esta es una operación bloqueante

                    // Log INMEDIATAMENTE después de leer, para confirmar recepción de CUALQUIER mensaje
                    LOGGER.info("ClienteSolicitante: Hilo de escucha RECIBIÓ MENSAJE BRUTO: " + recibido.toString());

                    short tipo = recibido.getNumeroServicio();
                    String folioRecibido = new String(recibido.getEvento());

                    if (tipo == Constantes.SERVICIO_ACUSE) { // Usar constantes
                        int conteoActual = acusesPorFolio.merge(folioRecibido, 1, Integer::sum);
                        LOGGER.info("ClienteSolicitante: ACK recibido para folio: " + folioRecibido + " → total ACKs: " + conteoActual);
                        if (conteoActual >= MIN_ACKS) { // Usar >= por si acaso llegan más de los necesarios
                            LOGGER.info("ClienteSolicitante: Folio " + folioRecibido + " ha alcanzado MIN_ACKS (" + MIN_ACKS + ").");
                        }
                    } else if (tipo == Constantes.SERVICIO_IMPRIMIR_RESULTADO) { // Usar constantes
                        int resultadoCalculado = ByteBuffer.wrap(recibido.getDatos()).getInt();
                        LOGGER.info("ClienteSolicitante: Resultado recibido → Folio: " + folioRecibido + ", Resultado: " + resultadoCalculado);
                    } else {
                        LOGGER.warn("ClienteSolicitante: Mensaje recibido con TIPO DE SERVICIO INESPERADO: " + tipo + " para Folio: " + folioRecibido + ". Mensaje completo: " + recibido.toString());
                    }
                } catch (java.io.EOFException eofe) {
                    LOGGER.error("ClienteSolicitante: Hilo de escucha - EOFException (Fin de Stream), el otro extremo probablemente cerró la conexión. Folio (si se leyó algo): " + ((recibido != null && recibido.getEvento() != null) ? new String(recibido.getEvento()) : "N/A"), eofe);
                    break; // Terminar el hilo
                } catch (java.net.SocketException se) {
                    LOGGER.error("ClienteSolicitante: Hilo de escucha - SocketException (ej. conexión reseteada, rota). Folio (si se leyó algo): " + ((recibido != null && recibido.getEvento() != null) ? new String(recibido.getEvento()) : "N/A"), se);
                    break; // Terminar el hilo
                } catch (IOException ioe) {
                    LOGGER.error("ClienteSolicitante: Hilo de escucha - IOException general al leer/decodificar. Folio (si se leyó algo): " + ((recibido != null && recibido.getEvento() != null) ? new String(recibido.getEvento()) : "N/A"), ioe);
                    break; // Terminar el hilo
                } catch (Exception e) {
                    LOGGER.error("ClienteSolicitante: Hilo de escucha - Error INESPERADO al procesar mensaje. Folio (si se leyó algo): " + ((recibido != null && recibido.getEvento() != null) ? new String(recibido.getEvento()) : "N/A"), e);
                    // Considera si quieres terminar el hilo o intentar continuar (break o no)
                    break; 
                }
            }
            LOGGER.warn("ClienteSolicitante: Hilo de escucha TERMINADO.");
        }).start();

        // Hilo generador: crea solicitudes y las pone en las colas si hay suficientes ACKs
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
                m.setEvento(folio.getBytes()); // Usamos el folio como 'evento'
                //m.setFolio(folio); 

                byte[] datos = ByteBuffer.allocate(8)
                        .putInt(a)
                        .putInt(b)
                        .array();
                m.setDatos(datos);

                try {
                    colasPorServicio.get(servicio).put(m);
                    LOGGER.info("Mensaje encolado para servicio " + servicio + " con Folio: " + new String(m.getEvento()));
                } catch (InterruptedException e) {
                    LOGGER.error("Error al poner mensaje en cola", e);
                    Thread.currentThread().interrupt(); // Restablecer el estado de interrupción
                }

                try {
                    Thread.sleep(5000); // Intervalo para generar nuevas solicitudes
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt(); // Restablecer el estado de interrupción
                }
            }
        }).start();
    }
}
