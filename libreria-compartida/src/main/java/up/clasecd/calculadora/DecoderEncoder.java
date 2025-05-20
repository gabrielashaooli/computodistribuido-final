package up.clasecd.calculadora;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class DecoderEncoder {

    /**
     * Lee el mensaje del input stream con el nuevo protocolo.
     */
    public static Mensaje leer(DataInputStream dis) throws IOException {
        short destinatario = dis.readShort();

        byte[] huella = new byte[8];
        dis.readFully(huella);

        short numeroServicio = dis.readShort();

        short longitudEvento = dis.readShort();
        byte[] evento = new byte[longitudEvento];
        dis.readFully(evento);

        int longitudDatos = dis.readInt();
        byte[] datos = new byte[longitudDatos];
        dis.readFully(datos);

        Mensaje m = new Mensaje();
        m.setDestinatario(destinatario);
        m.setHuella(huella);
        m.setNumeroServicio(numeroServicio);
        m.setEvento(evento);
        m.setDatos(datos);
        return m;
    }

    /**
     * Escribe el mensaje al output stream con el nuevo protocolo.
     */
    public static void escribir(DataOutputStream dos, Mensaje m) throws IOException {
        dos.writeShort(m.getDestinatario());

        byte[] huella = m.getHuella();
        if (huella == null || huella.length != 8) {
            throw new IOException("La huella debe tener exactamente 8 bytes");
        }
        dos.write(huella);

        dos.writeShort(m.getNumeroServicio());

        byte[] evento = m.getEvento();
        dos.writeShort(evento.length);
        dos.write(evento);

        byte[] datos = m.getDatos();
        dos.writeInt(datos.length);
        dos.write(datos);
    }

    public static Mensaje leer(Socket socket) throws IOException {
        return leer(new DataInputStream(socket.getInputStream()));
    }

    public static void escribir(Socket socket, Mensaje m) throws IOException {
        escribir(new DataOutputStream(socket.getOutputStream()), m);
    }
}

