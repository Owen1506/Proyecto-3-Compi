package tabla;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.List;
import java.util.Map;
import java.io.*;

// Administra el conjunto de tablas de simbolos durante todo el analisis.
// Mantiene una pila de scopes activos para saber en que nivel se esta trabajando
// en cada momento, y un historial completo de todos los simbolos vistos
// para poder consultarlos despues aunque su scope ya haya cerrado.
public class TablaManagement {

    // Pila de tablas activas. La que esta en la cima corresponde al scope actual.
    // Cuando se entra a un bloque se apila una nueva tabla; al salir se desapila.
    public Stack<Tabla> pilaScopes;
    private Stack<Simbolo> pilaFuncionActual = new Stack<>();
    // Registro de todos los simbolos insertados a lo largo del analisis, en orden de aparicion.
    // Se conservan incluso despues de que su scope haya cerrado, lo que permite
    // consultar informacion como el scope de un identificador al exportar tokens.
    List<Simbolo> historial;

    // Contador que asigna un identificador unico y creciente a cada scope que se abre.
    int contadorScopes = 0;

    // Inicializa el manejador creando la pila de scopes y abriendo el scope global (scope 0).
    public TablaManagement() {
        pilaScopes = new Stack<>();
        historial = new ArrayList<>();
        pilaScopes.push(new Tabla(contadorScopes++));
    }

    // Abre un nuevo nivel de scope, por ejemplo al entrar a una funcion o a un bloque.
    // Crea una tabla nueva y la coloca en la cima de la pila.
    public void entrarScope() {
        pilaScopes.push(new Tabla(contadorScopes++));
    }

    // Cierra el scope actual descartando su tabla.
    // Los simbolos ya quedaron en el historial, asi que no se pierde informacion.
    public void salirScope() {
        if (!pilaScopes.isEmpty()) {
            pilaScopes.pop();
        }
    }

    // Inserta un simbolo en la tabla del scope actual y lo agrega al historial global.
    // No valida redeclaraciones; si el nombre ya existe en el scope simplemente lo sobreescribe.
    // Entrada : simbolo a registrar.
    // Salida  : true siempre.
    public boolean insertar(Simbolo s) {
        Tabla actual = pilaScopes.peek();

        // Sin validacion semantica de redeclaracion
        actual.insertar(s);
        historial.add(s);

        return true;
    }

    // Busca un simbolo recorriendo la pila desde el scope mas interno hacia el global.
    // Esto implementa la regla de visibilidad: un identificador oculta a otro con el mismo
    // nombre declarado en un scope exterior.
    // Entrada : nombre del identificador a buscar.
    // Salida  : el simbolo encontrado, o null si no existe en ningun scope activo.
    public Simbolo buscarSimbolo(String nombre) {
        for (int i = pilaScopes.size() - 1; i >= 0; i--) {
            Tabla t = pilaScopes.get(i);
            if (t.existe(nombre)) {
                return t.obtener(nombre);
            }
        }
        return null;
    }
    public void pushFuncionActual(Simbolo s) {
        pilaFuncionActual.push(s);
    }

    public void popFuncionActual() {
        pilaFuncionActual.pop();
    }

    public Simbolo buscarSimboloFuncion() {
        return pilaFuncionActual.isEmpty() ? null : pilaFuncionActual.peek();
    }
    // Busca un simbolo en el historial completo, recorriendolo desde el final hacia el inicio.
    // Util para encontrar el scope en que fue declarado un identificador incluso si su tabla ya fue desapilada.
    // Entrada : nombre del identificador a buscar.
    // Salida  : la ultima aparicion del simbolo en el historial, o null si no se encontro.
    public Simbolo buscarEnHistorial(String nombre) {
        for (int i = historial.size() - 1; i >= 0; i--) {
            if (historial.get(i).nombre.equals(nombre)) {
                return historial.get(i);
            }
        }
        return null;
    }

    // Indica si un nombre existe en algun scope activo en este momento.
    // Entrada : nombre del identificador.
    // Salida  : true si se encuentra en algun nivel de la pila, false si no.
    public boolean existe(String nombre) {
        return buscarSimbolo(nombre) != null;
    }

    public boolean existeEnScope(String nombre, int scope) {

        // Recorre todos los scopes activos
        for (Tabla t : pilaScopes) {

            // Verifica si es el scope solicitado
            if (t.getIdScope() == scope) {

                // Busca el identificador dentro de ese scope
                return t.existe(nombre);
            }
        }

        // Si no encontro el scope o el simbolo
        return false;
    }

    public boolean existeEnHistorial(String nombre) {
        return buscarEnHistorial(nombre) != null;
    }
    
    // Retorna el identificador numerico del scope en el que se esta trabajando ahora mismo.
    public int getScopeActual() {
        return pilaScopes.peek().getIdScope();
    }

    // Imprime en consola todos los scopes activos con sus simbolos.
    // Pensado para depuracion durante el analisis, no para el reporte final.
    public void imprimirActual() {
        for (Tabla t : pilaScopes) {
            System.out.println("Scope: " + t.getIdScope());
            t.imprimir();
        }
    }

    // Imprime en consola el historial completo de simbolos agrupados por scope.
    // Se usa al final del analisis para mostrar la tabla de simbolos completa.
    public void imprimirHistorial() {
        Map<Integer, List<Simbolo>> porScope = new HashMap<>();

        // Se agrupa el historial por numero de scope para imprimir cada nivel por separado.
        for (Simbolo s : historial) {
            porScope
                .computeIfAbsent(s.scope, k -> new ArrayList<>())
                .add(s);
        }

        for (Integer scope : porScope.keySet()) {
            System.out.println("\n--- SCOPE " + scope + " ---");
            for (Simbolo s : porScope.get(scope)) {
                System.out.println(s);
            }
        }
    }

    // Exporta el historial completo de simbolos a un archivo de texto con formato de tabla.
    // Los simbolos se agrupan por scope y se muestran con nombre, tipo, categoria, linea y columna.
    // Entrada : ruta del archivo donde se guardara la tabla de simbolos.
    // Salida  : archivo tabla_simbolos.txt con la informacion formateada por scope.
    public void exportarTXT(String ruta) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(ruta))) {

            Map<Integer, List<Simbolo>> porScope = new HashMap<>();

            // Se agrupa el historial por scope antes de escribir.
            for (Simbolo s : historial) {
                porScope
                    .computeIfAbsent(s.scope, k -> new ArrayList<>())
                    .add(s);
            }

            for (Integer scope : porScope.keySet()) {
                writer.println("\nSCOPE: " + scope);
                writer.println("------------------------------------------------------");
                writer.printf("%-10s %-10s %-12s %-6s %-6s%n",
                        "NOMBRE", "TIPO", "CATEGORIA", "LIN", "COL");
                writer.println("------------------------------------------------------");

                for (Simbolo s : porScope.get(scope)) {
                    writer.printf("%-10s %-10s %-12s %-6d %-6d%n",
                            s.nombre,
                            s.tipo,
                            s.categoria,
                            s.linea,
                            s.columna);
                }
            }

            System.out.println("Tabla exportada correctamente a: " + ruta);

        } catch (IOException e) {
            System.out.println("Error al exportar: " + e.getMessage());
        }
    }
}