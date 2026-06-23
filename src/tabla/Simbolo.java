package tabla;

// Representa un simbolo individual dentro de la tabla de simbolos.
// Cada simbolo corresponde a un identificador reconocido durante el analisis:
// puede ser una variable, una funcion o un parametro.
public class Simbolo {

    // Nombre con el que fue declarado el identificador en el codigo fuente.
    String nombre;

    // Tipo de dato asociado al simbolo (int, float, char, string, bool, expint, frac).
    String tipo;

    // Indica que clase de construccion representa el simbolo: variable, funcion o parametro.
    String categoria;

    // Linea del archivo fuente donde fue declarado el simbolo.
    int linea;

    // Columna del archivo fuente donde fue declarado el simbolo.
    int columna;

    // Nivel de anidamiento en el que fue declarado.
    // El scope 0 es el global; cada bloque o funcion abre uno nuevo.
    int scope;

    int filas = 0;

    int columnas = 0;

    int elemSize;

    int offSetMips;

    boolean valor = false;

    private String parametros;  // ej: "int,float,bool" o "" para vacío

    // Construye un simbolo con toda la informacion necesaria para registrarlo en la tabla.
    // Entrada: nombre del identificador, tipo de dato, categoria (variable/funcion/parametro),
    //          linea y columna donde aparece en el fuente, y el scope en que fue declarado.
    public Simbolo(String nombre, String tipo, String categoria, int linea, int columna, int scope) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.categoria = categoria;
        this.linea = linea;
        this.columna = columna;
        this.scope = scope;
    }

    public String getNombre() {
        return nombre;
    }
    public String getCategoria(){
        return categoria;
    }

    public void setCategoria(String categoria){
        this.categoria = categoria;
    }
    // Retorna el nivel de scope en que fue declarado este simbolo.
    // Lo usan otras clases para saber si un simbolo es accesible desde el contexto actual.
    public int getScope() { return scope; }
    
    public String getTipo() { return tipo; }
    // Genera una representacion legible del simbolo para imprimirla en consola o exportarla.
    // Incluye scope, nombre, tipo, categoria, linea y columna en un formato de una sola linea.

    public String toString() {
        return String.format(
            "[Scope %d] Nombre: %s : Tipo: %s Categoria: %s  Linea:%d Columna:%d ",
            scope,
            nombre,
            tipo,
            categoria,
            linea,
            columna
        );
    }

    public String getParametros(){ 
        return parametros; 
    }
    public void setParametros(String param){ 
        this.parametros = param; 
    }
    
    public void setFilas(int x){ 
        filas = x; 
    }

    public int getFilas(){ 
        return filas; 
    }

    public void setColumnas(int y){ 
        columnas = y; 
    }

    public int getColumnas(){ 
        return columnas; 
    }
    
    public void setElemSize(int elem){ 
        elemSize = elem; 
    }

    public int getElemSize(){ 
        return elemSize; 
    }
}