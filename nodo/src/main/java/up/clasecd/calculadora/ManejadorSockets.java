package up.clasecd.calculadora;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ManejadorSockets implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(ManejadorSockets.class);
    private final Socket socket;
    private static final Set<String> foliosProcesados = ConcurrentHashMap.newKeySet();

    public ManejadorSockets(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            LOGGER.info("------> Iniciando el hilo con: " + socket);

            while (true) {
                Mensaje mensaje = DecoderEncoder.leer(socket);
                LOGGER.info("Se recibió el mensaje: " + mensaje + ", del socket: " + socket);

                switch (mensaje.getNumeroServicio()) {
                    case 0: {
                        String indicador = new String(mensaje.getDatos());
                        if (indicador.equals("NODO")) {
                            GestorConexiones.getInstance().addNodo(socket);
                        } else {
                            GestorConexiones.getInstance().addCliente(socket);
                        }
                        LOGGER.info("Socket identificado como: " + indicador);
                        break;
                    }

                    case 1, 2, 3, 4, 5, 99: {
                        // Validación: ya lo procesamos antes
                        String folio = new String(mensaje.getEvento()); 
                        short servicioActual = mensaje.getNumeroServicio();
                        //if (!foliosProcesados.add(folio)) {
                        //    LOGGER.debug("Folio ya procesado, ignorando: " + folio);
                        //    break;
                        //}
                        if (servicioActual >= Constantes.SERVICIO_SUMA && servicioActual <= Constantes.SERVICIO_DIVISION) { // Si es una solicitud de operación 1-4
                            if (!foliosProcesados.add(folio)) {
                                // Cambiamos a WARN para que sea visible con tu nivel de log INFO, o puedes mantener DEBUG
                                LOGGER.warn("Nodo: Folio de SOLICITUD (" + folio + ", serv:" + servicioActual + ") ya procesado por este nodo. Ignorando para evitar re-difusión de la solicitud.");
                                break; // No difundir de nuevo esta solicitud
                            }
                        }
                        
                        LOGGER.info("Nodo procesando y difundiendo mensaje con folio: " + folio + ", servicio: " + mensaje.getNumeroServicio());

                        // Reenvío a nodos (excepto al origen)
                        for (Socket nodoSocket : GestorConexiones.getInstance().getNodos()) {
                            if (!nodoSocket.equals(socket)) { 
                                try {
                                    DecoderEncoder.escribir(nodoSocket, mensaje);
                                } catch (IOException e) {
                                    LOGGER.error("Error al reenviar mensaje al nodo " + nodoSocket, e);
                                    // Considerar remover este nodoSocket si la conexión falla persistentemente
                                }
                            }
                        }

                        // Reenvío a clientes (excepto al origen)
                        List<Socket> clientesActuales = GestorConexiones.getInstance().getClientes(); // Obtener la lista una vez
                        LOGGER.info("Nodo [Hilo " + Thread.currentThread().getName() + "]: Va a intentar reenviar mensaje (folio: " + folio + ", serv: " + mensaje.getNumeroServicio() + ") a " + clientesActuales.size() + " cliente(s) registrados.");

                        for (Socket clienteSocketDestino : clientesActuales) {
                            LOGGER.info("Nodo [Hilo " + Thread.currentThread().getName() + "]: Chequeando reenvío. Origen del mensaje actual (" + servicioActual + "): " + socket.getRemoteSocketAddress() + ", Considerando cliente destino: " + clienteSocketDestino.getRemoteSocketAddress());
                            if (!clienteSocketDestino.equals(socket)) {
                                try {
                                    LOGGER.info("Nodo [Hilo " + Thread.currentThread().getName() + "]: SÍ VA A REENVIAR mensaje (folio: " + folio + ", serv: " + servicioActual + ") de " + socket.getRemoteSocketAddress() + " HACIA CLIENTE: " + clienteSocketDestino.getRemoteSocketAddress());
                                    DecoderEncoder.escribir(clienteSocketDestino, mensaje);
                                } catch (IOException e) {
                                    LOGGER.error("Nodo [Hilo " + Thread.currentThread().getName() + "]: Error al reenviar mensaje al cliente " + clienteSocketDestino.getRemoteSocketAddress() + ". Mensaje: " + mensaje, e);
                                }
                            } else {
                                LOGGER.info("Nodo [Hilo " + Thread.currentThread().getName() + "]: NO se reenvía a " + clienteSocketDestino.getRemoteSocketAddress() + " (es el origen del mensaje actual - folio: " + folio + ")");
                            }
                        }
                        break;
                    }

                    default:
                        LOGGER.warn("Mensaje fuera del protocolo: " + mensaje);
                }
            }

        } catch (Exception ex) {
            GestorConexiones.getInstance().remove(socket);
            LOGGER.error("Se perdió la conexión con: " + socket);
        }
    }
}
