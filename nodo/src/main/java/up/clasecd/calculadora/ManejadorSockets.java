package up.clasecd.calculadora;

import java.net.Socket;
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

                    case 1, 2, 5, 99: {
                        // Validación: ya lo procesamos antes
                        String folio = mensaje.getFolio();
                        if (!foliosProcesados.add(folio)) {
                            LOGGER.debug("Folio ya procesado, ignorando: " + folio);
                            break;
                        }

                        // Reenvío a nodos (excepto al origen)
                        for (Socket nodoSocket : GestorConexiones.getInstance().getNodos()) {
                            if (!nodoSocket.equals(socket)) {
                                DecoderEncoder.escribir(nodoSocket, mensaje);
                            }
                        }

                        // Reenvío a clientes (excepto al origen)
                        for (Socket clienteSocket : GestorConexiones.getInstance().getClientes()) {
                            if (!clienteSocket.equals(socket)) {
                                DecoderEncoder.escribir(clienteSocket, mensaje);
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
