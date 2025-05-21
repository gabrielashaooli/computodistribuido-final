package up.clasecd.calculadora;

import java.io.IOException;
import java.net.Socket;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.UUID;


public class Cliente {

    private static final Logger LOGGER = LogManager.getLogger(Cliente.class);
    private static Socket socket;
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    // Huella única generada al iniciar el cliente
    private static final byte[] HUELLA = generarHuella();

    private static byte[] generarHuella() {
        byte[] huella = new byte[8];
        RANDOM.nextBytes(huella);
        return huella;
    }

    private static void establecerConexion() throws Exception {
        for (int i = Constantes.PUERTO_INICIAL; i <= Constantes.PUERTO_FINAL; i++) {
            try {
                socket = new Socket("localhost", i);
                LOGGER.info("Se logró la conexión con: " + i);
                // enviar mensaje de identificación con huella
                Mensaje m = new Mensaje();
                m.setDestinatario((short) 0); // Nodo
                m.setHuella(HUELLA);
                m.setNumeroServicio(Constantes.SERVICIO_IDENTIFICACION);
                m.setEvento("".getBytes());
                m.setDatos("CLIENTE".getBytes());
                DecoderEncoder.escribir(socket, m);
                return;
            } catch (IOException iOException) {
                LOGGER.warn("No se logró la conexión con: " + i, iOException);
            }
        }
        throw new Exception("No existe ningún nodo del CD");
    }

    public static void main(String[] args) throws Exception {
        establecerConexion();

        new Thread(() -> {
            do {
                try {
                    Mensaje mensaje = new Mensaje();
                    mensaje.setDestinatario((short) 0); // Nodo
                    mensaje.setHuella(HUELLA);
                    mensaje.setNumeroServicio(Constantes.SERVICIO_SOLICITUD);
                    mensaje.setEvento((System.nanoTime() + "").getBytes());
                    mensaje.setDatos((RANDOM.nextInt(100) + ":" + RANDOM.nextInt(100)).getBytes());
                    mensaje.setFolio(UUID.randomUUID().toString().substring(0, 8));

                    DecoderEncoder.escribir(socket, mensaje);
                    LOGGER.info("Se solicita: " + mensaje);
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

        new Thread(() -> {
            try {
                while (true) {
                    Mensaje mensaje = DecoderEncoder.leer(socket);
                    LOGGER.info("Se recibió el mensaje: " + mensaje);

                    switch (mensaje.getNumeroServicio()) {
                        case (short) 1:
                            String datos = new String(mensaje.getDatos());
                            String[] split = datos.split(":");
                            Integer a = Integer.valueOf(split[0]);
                            Integer b = Integer.valueOf(split[1]);
                            Integer resultado = a + b;
                            Mensaje resultadoMensaje = new Mensaje();
                            resultadoMensaje.setDestinatario((short) 0);
                            resultadoMensaje.setHuella(HUELLA);
                            resultadoMensaje.setNumeroServicio(Constantes.SERVICIO_RESULTADO);
                            resultadoMensaje.setEvento(mensaje.getEvento());
                            resultadoMensaje.setDatos(resultado.toString().getBytes());
                            resultadoMensaje.setFolio(mensaje.getFolio()); 
                            DecoderEncoder.escribir(socket, resultadoMensaje);
                            LOGGER.info("Se envía respuesta: " + resultadoMensaje);
                            break;
                        case (short) 2:
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
