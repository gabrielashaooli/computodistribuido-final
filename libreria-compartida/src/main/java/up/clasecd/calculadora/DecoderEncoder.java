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

        //short longitudFolio = dis.readShort();
        //byte[] folioBytes = new byte[longitudFolio];
        //dis.readFully(folioBytes);
        //String folio = new String(folioBytes);
        
        Mensaje m = new Mensaje();
        m.setDestinatario(destinatario);
        m.setHuella(huella);
        m.setNumeroServicio(numeroServicio);
        m.setEvento(evento); // evento es folio en bytes
        m.setDatos(datos);
        // m.setFolio(folio);
        return m;
    }

    public static void escribir(DataOutputStream dos, Mensaje m) throws IOException {
        dos.writeShort(m.getDestinatario());

        byte[] huella = m.getHuella();
        if (huella == null || huella.length != 8) {
            throw new IOException("La huella debe tener exactamente 8 bytes");
        }
        dos.write(huella);

        dos.writeShort(m.getNumeroServicio());

        byte[] evento = m.getEvento(); // evento contiene folio como bytes
        if (evento == null) evento = new byte[0];
        dos.writeShort(evento.length);
        dos.write(evento);

        byte[] datos = m.getDatos();
        if (datos == null) datos = new byte[0];
        dos.writeInt(datos.length);
        dos.write(datos);
        
        dos.flush();
        //String folio = m.getFolio();
        //if (folio == null) folio = "";
        //byte[] folioBytes = folio.getBytes();
        //dos.writeShort(folioBytes.length);
        //dos.write(folioBytes);
    }

    public static Mensaje leer(Socket socket) throws IOException {
        return leer(new DataInputStream(socket.getInputStream()));
    }

    public static void escribir(Socket socket, Mensaje m) throws IOException {
        escribir(new DataOutputStream(socket.getOutputStream()), m);
    }
}

