package tabla;

import java.util.HashMap;
import java.util.Map;

// Representa una tabla de simbolos para un scope especifico.
// Cada bloque o funcion del programa tiene su propia instancia de esta clase,
// que guarda unicamente los simbolos declarados dentro de ese nivel.
public class Tabla {

    // Mapa que asocia el nombre de cada identificador con su simbolo correspondiente.
    // Se usa el nombre como clave para hacer busquedas rapidas.
    Map<String, Simbolo> simbolos;

    // Identificador numerico del scope al que pertenece esta tabla.
    // Permite saber en que nivel de anidamiento fueron declarados los simbolos que contiene.
    int idScope;

    private boolean tieneReturn = false;

    // Crea una tabla vacia para el scope indicado.
    // Entrada: numero de scope que identificara a esta tabla.
    public Tabla(int idScope) {
        this.idScope = idScope;
        this.simbolos = new HashMap<>();
    }

    // Agrega un simbolo a la tabla usando su nombre como clave.
    // Si ya existia un simbolo con ese nombre en el mismo scope, lo reemplaza.
    // Entrada: el simbolo a registrar.
    public void insertar(Simbolo s) {
        simbolos.put(s.nombre, s);
    }

    // Indica si un nombre ya fue declarado en este scope.
    // Entrada : nombre del identificador a buscar.
    // Salida  : true si existe, false si no.
    public boolean existe(String nombre) {
        return simbolos.containsKey(nombre);
    }

    // Retorna el simbolo asociado a un nombre en este scope.
    // Entrada : nombre del identificador.
    // Salida  : el simbolo encontrado, o null si no existe en este scope.
    public Simbolo obtener(String nombre) {
        return simbolos.get(nombre);
    }

    public java.util.Collection<Simbolo> obtenerSimbolos() {
        return simbolos.values();
    }
    // Retorna el identificador numerico del scope de esta tabla.
    public int getIdScope() {
        return idScope;
    }

    public boolean tieneReturn() {
        return tieneReturn;
    }

    public void setTieneReturn(boolean tieneReturn) {
        this.tieneReturn = tieneReturn;
    }

    // Imprime en consola todos los simbolos registrados en esta tabla.
    // Util para depuracion rapida del contenido de un scope especifico.
    public void imprimir() {
        for (Simbolo s : simbolos.values()) {
            System.out.println(s);
        }
    }
}