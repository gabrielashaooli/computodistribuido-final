package up.clasecd.calculadora;

/**
 * Mensaje del protocolo
 *
 * @author sebastian
 */
public class Mensaje {

    private Short numeroServicio;
    private byte[] evento;
    private byte[] datos;

    public Mensaje() {
    }

    public Short getNumeroServicio() {
        return numeroServicio;
    }

    public void setNumeroServicio(Short numeroServicio) {
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

    @Override
    public String toString() {
        return "Mensaje{" + "numeroServicio=" + numeroServicio + ", evento=" + evento + ", datos=" + datos + '}';
    }

}
