package up.clasecd.calculadora;

import java.util.Arrays;

public class Mensaje {

    private short destinatario;       // 0=nodo, 1=servidor, 2=cliente
    private byte[] huella = new byte[8]; // ID único generado por cada célula
    private short numeroServicio;     // Tipo de servicio (suma, resta, acuse, etc.)
    private byte[] evento;            // ID del evento (folio, UUID, timestamp, etc.)
    private byte[] datos;             // Información del servicio (operaciones, resultados...)
    private String folio;             // Folio único del mensaje
    
    public Mensaje() {}

    public short getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(short destinatario) {
        this.destinatario = destinatario;
    }

    public byte[] getHuella() {
        return huella;
    }

    public void setHuella(byte[] huella) {
        this.huella = huella;
    }

    public short getNumeroServicio() {
        return numeroServicio;
    }

    public void setNumeroServicio(short numeroServicio) {
        this.numeroServicio = numeroServicio;
    }

    public byte[] getEvento() {
        return evento;
    }

    public void setEvento(byte[] evento) {
        this.evento = evento;
    }

    public byte[] getDatos() {
        return datos;
    }

    public void setDatos(byte[] datos) {
        this.datos = datos;
    }
    
    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    @Override
    public String toString() {
        return "Mensaje{" +
                "destinatario=" + destinatario +
                ", huella=" + Arrays.toString(huella) +
                ", numeroServicio=" + numeroServicio +
                ", evento=" + new String(evento) +
                ", datos=" + new String(datos) +
                ", folio='" + folio + '\'' +
                '}';
    }
}
