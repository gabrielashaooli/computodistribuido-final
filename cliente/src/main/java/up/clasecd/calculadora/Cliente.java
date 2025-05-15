package up.clasecd.calculadora;

import java.io.IOException;
import java.net.Socket;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author sebastian
 */
public class Cliente {

    private static final Logger LOGGER = LogManager.getLogger(Cliente.class);
    private static Socket socket;
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private static void establecerConexion() throws Exception {
        for (int i = Constantes.PUERTO_INICIAL; i <= Constantes.PUERTO_FINAL; i++) {
            try {
                socket = new Socket("localhost", i);
                LOGGER.info("Se logro la conexion con:  + i");
                // enviar el mensaje de identificacion que soy cliente
                // enviar un mensaje de identificacion
                Mensaje m = new Mensaje();
                m.setDatos("CLIENTE".getBytes());
                m.setEvento("".getBytes());
                m.setNumeroServicio(Constantes.SERVICIO_IDENTIFICACION);
                DecoderEncoder.escribir(socket, m);
                return;
            } catch (IOException iOException) {
                LOGGER.warn("No se logro la conexion con: " + i, iOException);
            }
        }
        throw new Exception("No existe ningun nodo del CD");
    }

    public static void main(String[] args) throws Exception {
        // establecer conexion con un nodo del CD
        establecerConexion();

        new Thread(() -> {
            do {
                try {
                    Mensaje mensaje = new Mensaje();
                    mensaje.setDatos((RANDOM.nextInt(100) + ":" + RANDOM.nextInt(100)).getBytes());
                    mensaje.setEvento((System.nanoTime() + "").getBytes());
                    mensaje.setNumeroServicio(Constantes.SERVICIO_SOLICITUD);
                    DecoderEncoder.escribir(socket, mensaje);
                    LOGGER.info("se solicita: " + mensaje);
                } catch (IOException iOException) {
                    LOGGER.error("Al enviar un mensaje de prueba", iOException);
                }
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException interruptedException) {
                    LOGGER.error("Al dormir el hilo", interruptedException);
                }
            } while (true);
        }).start();

        // crear un hilo de atencion que recibe y procesa los mensajes
        new Thread(() -> {
            try {
                while (true) {
                    // leemos el mensaje
                    Mensaje mensaje = DecoderEncoder.leer(socket);
                    LOGGER.info("se recibio el mensaje: " + mensaje);

                    // se procesa el mensaje
                    switch (mensaje.getNumeroServicio()) {
                        case (short) 1:
                            String datos = new String(mensaje.getDatos());
                            String[] split = datos.split(":");
                            Integer a = Integer.valueOf(split[0]);
                            Integer b = Integer.valueOf(split[1]);
                            Integer resultado = a + b;
                            // enviar resultado
                            Mensaje resultadoMensaje = new Mensaje();
                            resultadoMensaje.setDatos(resultado.toString().getBytes());
                            resultadoMensaje.setEvento(mensaje.getEvento());
                            resultadoMensaje.setNumeroServicio(Constantes.SERVICIO_RESULTADO);
                            DecoderEncoder.escribir(socket, resultadoMensaje);
                            LOGGER.info("se envia respuesta: " + resultadoMensaje);
                            break;
                        case (short) 2:
                            /// imprime el resultado
                            LOGGER.info("Resultado: " + new String(mensaje.getDatos()));
                            break;
                        default:
                            LOGGER.warn("Mensaje fuera del protocolo");
                    }

                }
            } catch (Exception e) {
                LOGGER.error("Cliente desconectado");
            }

        }).start();
    }

}
