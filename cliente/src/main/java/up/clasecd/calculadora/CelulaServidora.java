/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.CelulaServidora to edit this template
 */
package up.clasecd.calculadora;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CelulaServidora {
    private static final Logger LOGGER = LogManager.getLogger(CelulaServidora.class);
    private static final byte[] HUELLA = generarHuella();
    private static Socket socket;
    private static final BlockingQueue<Mensaje> colaEntrada = new LinkedBlockingQueue<>();

    private static byte[] generarHuella() {
        byte[] huella = new byte[8];
        new Random().nextBytes(huella);
        return huella;
    }

    private static void establecerConexion() throws Exception {
        for (int i = Constantes.PUERTO_INICIAL; i <= Constantes.PUERTO_FINAL; i++) {
            try {
                socket = new Socket(Constantes.LOCALHOST, i);
                LOGGER.info("Conectado al nodo en puerto: " + i);
                Mensaje m = new Mensaje();
                m.setDestinatario((short) 0);
                m.setHuella(HUELLA);
                m.setNumeroServicio((short) 0); // Identificación
                m.setEvento("".getBytes());
                m.setDatos("S".getBytes()); // Identificador como Servidor
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

        // Hilo receptor
        new Thread(() -> {
            try {
                while (true) {
                    Mensaje recibido = DecoderEncoder.leer(socket);
                    short tipo = recibido.getNumeroServicio();
                    if (tipo >= 1 && tipo <= 4) {
                        // Enviar ACK
                        Mensaje ack = new Mensaje();
                        ack.setDestinatario((short) 0);
                        ack.setHuella(HUELLA);
                        ack.setNumeroServicio((short) 99);
                        ack.setEvento(recibido.getEvento());
                        ack.setDatos("ACK".getBytes());
                        ack.setFolio(recibido.getFolio());
                        DecoderEncoder.escribir(socket, ack);
                        LOGGER.info("ACK enviado para: " + recibido.getFolio());

                        // Validar tamaño de datos
                        if (recibido.getDatos().length == 8) {
                            colaEntrada.put(recibido);
                            LOGGER.info("Solicitud encolada: " + recibido);
                        } else {
                            LOGGER.warn("Mensaje ignorado por datos inválidos: " + recibido);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error en hilo receptor", e);
            }
        }).start();

        // Hilo de ejecución de servicios
        new Thread(() -> {
            while (true) {
                try {
                    Mensaje m = colaEntrada.take(); // toma siguiente solicitud
                    ByteBuffer buffer = ByteBuffer.wrap(m.getDatos());
                    int a = buffer.getInt();
                    int b = buffer.getInt();
                    int resultado;

                    switch (m.getNumeroServicio()) {
                        case 1 -> resultado = a + b;
                        case 2 -> resultado = a - b;
                        case 3 -> resultado = a * b;
                        case 4 -> resultado = (b != 0) ? a / b : 0;
                        default -> {
                            LOGGER.warn("Servicio desconocido");
                            continue;
                        }
                    }

                    Mensaje respuesta = new Mensaje();
                    respuesta.setDestinatario((short) 0);
                    respuesta.setHuella(HUELLA);
                    respuesta.setNumeroServicio((short) 5); // respuesta final
                    respuesta.setEvento(m.getEvento());
                    respuesta.setFolio(m.getFolio());
                    respuesta.setDatos(ByteBuffer.allocate(4).putInt(resultado).array());

                    DecoderEncoder.escribir(socket, respuesta);
                    LOGGER.info("Resultado enviado: " + resultado + " para " + m.getFolio());

                } catch (Exception e) {
                    LOGGER.error("Error en ejecución de servicios", e);
                }
            }
        }).start();
    }
}
