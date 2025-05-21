package up.clasecd.calculadora;

import java.net.Socket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ManejadorSockets implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(ManejadorSockets.class);
    private final Socket socket;

    public ManejadorSockets(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            LOGGER.info("------> Iniciando el hilo");

            // Esperar mensajes
            while (true) {
                Mensaje mensaje = DecoderEncoder.leer(socket);
                LOGGER.info("Se recibió el mensaje: " + mensaje + ", del socket: " + socket);

                switch (mensaje.getNumeroServicio()) {
                    case (short) 0: // IDENTIFICACIÓN
                        String indicador = new String(mensaje.getDatos());
                        if (indicador.equals("NODO")) {
                            GestorConexiones.getInstance().addNodo(socket);
                        } else {
                            GestorConexiones.getInstance().addCliente(socket);
                        }
                        LOGGER.info("Socket identificado como: " + indicador);
                        break;

                    case (short) 1, (short) 2: // SOLICITUD o RESULTADO
                        // Si este socket NO es nodo, reenvía a nodos
                        if (!GestorConexiones.getInstance().esNodo(socket)) {
                            for (Socket nodoSocket : GestorConexiones.getInstance().getNodos()) {
                                DecoderEncoder.escribir(nodoSocket, mensaje);
                            }
                        }

                        // Si este socket NO es cliente, reenvía a clientes
                        for (Socket clienteSocket : GestorConexiones.getInstance().getClientes()) {
                            DecoderEncoder.escribir(clienteSocket, mensaje);
                        }
                        break;

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
