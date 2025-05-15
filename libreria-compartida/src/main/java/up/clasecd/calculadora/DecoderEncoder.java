package up.clasecd.calculadora;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author sebastian
 */
public class DecoderEncoder {

    private static final Logger LOGGER = LogManager.getLogger(DecoderEncoder.class);

    /**
     * Lee del IS el protocolo
     *
     * @param dis
     * @return
     * @throws IOException
     */
    public static Mensaje leer(DataInputStream dis) throws IOException {
        // leer del IS el protocolo del mensaje
        Short numeroServicio = dis.readShort();
        Short longitudEvento = dis.readShort();
        byte[] evento = new byte[longitudEvento];
        dis.readFully(evento);
        Integer longitudDatos = dis.readInt();
        byte[] datos = new byte[longitudDatos];
        dis.readFully(datos);
        // crear una instancia de mensaje
        Mensaje m = new Mensaje();
        m.setDatos(datos);
        m.setEvento(evento);
        m.setNumeroServicio(numeroServicio);
        return m;
    }

    /**
     * Escribe el mensaje en el IS de acuerdo al protocolo
     *
     * @param dos
     * @param m
     * @throws IOException
     */
    public static void escribir(DataOutputStream dos, Mensaje m) throws IOException {
        // escribir el mensaje en el formato del protocolo
        dos.writeShort(m.getNumeroServicio());
        dos.writeShort(m.getEvento().length);
        dos.write(m.getEvento());
        dos.writeInt(m.getDatos().length);
        dos.write(m.getDatos());
    }

    /**
     * Lee del sokcket un mensaje
     *
     * @param socket
     * @return
     * @throws IOException
     */
    public static Mensaje leer(Socket socket) throws IOException {
        return leer(new DataInputStream(socket.getInputStream()));
    }

    /**
     * Escribe en el OS del socket el mensaje
     *
     * @param socket
     * @param m
     * @throws IOException
     */
    public static void escribir(Socket socket, Mensaje m) throws IOException {
        escribir(new DataOutputStream(socket.getOutputStream()), m);
    }

}
