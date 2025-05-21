package up.clasecd.calculadora;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Nodo {

    private static final Logger LOGGER = LogManager.getLogger(Nodo.class);
    private static ServerSocket serverSocket;

    /**
     *
     * @throws Exception
     */
    private static void getServerSocket() throws Exception {
        // recorre el rango de puertos y trata de obtener el server socket
        for (int i = Constantes.PUERTO_INICIAL; i <= Constantes.PUERTO_FINAL; i++) {
            try {
                LOGGER.info("Intentando obtener el puerto: " + i);
                serverSocket = new ServerSocket(i);
                return;
            } catch (IOException ex) {
                LOGGER.warn("El puerto: " + i + ", esta ocupado");
            }
        }
        // si no se logro ningun SS con un puerto en el rango se manda un error
        throw new Exception("No se logro tomar nungun puerto");
    }

    private static void conectarConOtrosNodos() {
        // recorre el rango de puertos y trata conectarse con otros nodos
        for (int i = Constantes.PUERTO_INICIAL; i <= Constantes.PUERTO_FINAL; i++) {
            // nos saltamos el puerto que este nodo esta utilizando
            if (serverSocket.getLocalPort() == i) {
                continue;
            }
            // se intenta la conexion
            try {
                LOGGER.info("Intentando la conexion con el puerto: " + i);
                Socket socket = new Socket(Constantes.LOCALHOST, i);
                // utilizar el socket para los siguientes pasos
                new Thread(new ManejadorSockets(socket)).start();
            } catch (IOException ex) {
                LOGGER.warn("El puerto: " + i + ", no esta disponible");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // obtener el serversocker, dentro de un rango de puertos
        getServerSocket();
        // conectarse con otros nodos
        conectarConOtrosNodos();
        // esperar clientes/conexion/otrosNodos/celulas
        LOGGER.info("Esperando conexiones...");
        while (true) {
            Socket socket = serverSocket.accept();
            LOGGER.info("Llego el cliente: " + socket.getInetAddress());
            // utilizar el socket para los siguientes pasos 
            new Thread(new ManejadorSockets(socket)).start();
        }
    }

}
