package up.clasecd.calculadora;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sebastian
 */
public class GestorConexiones {

    private final List<Socket> nodos = new ArrayList<>();
    private final List<Socket> clientes = new ArrayList<>();

    private GestorConexiones() {
    }

    public static GestorConexiones getInstance() {
        return GestorConexionesHolder.INSTANCE;
    }

    private static class GestorConexionesHolder {

        private static final GestorConexiones INSTANCE = new GestorConexiones();
    }
    
    public Boolean esNodo(Socket socket){
        return nodos.contains(socket);
    }

    public void addNodo(Socket socket) {
        nodos.add(socket);
    }

    public void addCliente(Socket socket) {
        clientes.add(socket);
    }

    public List<Socket> getNodos() {
        return new ArrayList<>(nodos);
    }

    public List<Socket> getClientes() {
        return new ArrayList<>(clientes);
    }

    public void remove(Socket socket) {
        nodos.remove(socket);
        clientes.remove(socket);
    }

}
