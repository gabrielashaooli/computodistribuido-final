package up.clasecd.calculadora;

import java.net.Socket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author sebastian
 */
public class ManejadorSockets implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(ManejadorSockets.class);
    // socket que gestionara este hilo
    private final Socket socket;

    public ManejadorSockets(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            LOGGER.info("------> Iniciando el hilo");
            // enviar un mensaje de identificacion
            Mensaje m = new Mensaje();
            m.setDatos("NODO".getBytes());
            m.setEvento("".getBytes());
            m.setNumeroServicio(Constantes.SERVICIO_IDENTIFICACION);
            DecoderEncoder.escribir(socket, m);
            // esperar un mensaje
            while (true) {
                Mensaje mensaje = DecoderEncoder.leer(socket);
                LOGGER.info("Se recibio el mensaje: " + mensaje + ", del socket: " + socket);
                // procesar el mensaje
                switch (mensaje.getNumeroServicio()) {
                    // en caso de un mensaje de identificacion
                    case (short) 0:
                        String indicador = new String(mensaje.getDatos());
                        if (indicador.compareTo("NODO") == 0) {
                            GestorConexiones.getInstance().addNodo(socket);
                        } else {
                            GestorConexiones.getInstance().addCliente(socket);
                        }
                        LOGGER.info("socket: " + socket + ", se identifico como:" + indicador);
                        break;
                    // en caso de un mensaje de identificacion
                    case (short) 1, (short) 2:
                        if (!GestorConexiones.getInstance().esNodo(socket)) {
                            for (Socket nodoSocket : GestorConexiones.getInstance().getNodos()) {
                                DecoderEncoder.escribir(nodoSocket, m);
                            }
                        }
                        for (Socket clienteSocket : GestorConexiones.getInstance().getClientes()) {
                            LOGGER.error("se reenvia el mensaje al cliente: " + clienteSocket + ", mensaje: " + m);
                            DecoderEncoder.escribir(clienteSocket, mensaje);
                        }
                        break;
                    default:
                        LOGGER.error("Mensaje fuera del protocolo: " + mensaje);
                }

            }

        } catch (Exception ex) {
            GestorConexiones.getInstance().remove(socket);
            LOGGER.error("Se perdio la conexion con: " + socket);
        }
    }

}
