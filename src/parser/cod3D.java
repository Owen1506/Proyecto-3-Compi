package parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Generador de código de tres direcciones (3D).
 * Proporciona utilidades para construir código intermedio
 * durante el proceso de compilación.
 */
public class cod3D {

    /** Almacena las líneas de código de tres direcciones generadas. */
    private StringBuilder codigo = new StringBuilder();

    /** Contador para generar nombres únicos de temporales. */
    private int temp = 0;

    /** Contador para generar etiquetas únicas. */
    private int labelCounter = 0;

    private boolean capturando = false;
    private List<String> codigoCapturado = new ArrayList<>();
    /**
     * Genera una nueva etiqueta única para saltos o marcadores.
     *
     * Entrada: ninguna.
     * Salida: String con formato "L<n>" donde n es un entero incremental.
     * Restricción: las etiquetas no se reutilizan dentro de la misma instancia.
     */
    public String nuevaEtiqueta() {
        return "L" + (labelCounter++);
    }

    /**
     * Genera un nuevo nombre de variable temporal único.
     *
     * Entrada: ninguna.
     * Salida: String con formato "t<n>" donde n comienza en 1 e incrementa.
     * Restricción: los temporales no se reutilizan dentro de la misma instancia.
     */
    public String nuevaTemporal(){
        temp++;
        return "t" + temp;
    }

    /**
     * Agrega una línea de código de tres direcciones al buffer.
     *
     * Entrada: linea — instrucción en formato de tres direcciones (e.g. "t1 = a + b").
     * Salida: ninguna (modifica el estado interno del buffer).
     * Restricción: no valida el formato de la línea recibida.
     */
    public void appendCod3D(String linea){
        if (capturando) {
        codigoCapturado.add(linea);
        } else {
        codigo.append(linea).append("\n");
        }
    }
    /**
     * Reemplaza la última ocurrencia de una línea exacta dentro del buffer.
     * Útil para correcciones sobre instrucciones recién generadas (e.g. backpatching).
     *
     * Entrada:
     *   lineaVieja — texto exacto de la línea a reemplazar.
     *   lineaNueva — texto que sustituirá a lineaVieja.
     * Salida: ninguna (modifica el estado interno del buffer).
     * Restricción: la comparación es exacta (sensible a espacios y mayúsculas).
     *              Si lineaVieja no existe, el buffer no se modifica.
     *              Solo reemplaza la última ocurrencia encontrada.
     */
    public void reemplazarLinea(String lineaVieja, String lineaNueva){
        String[] lineas = codigo.toString().split("\n");
        for(int i = lineas.length - 1; i >= 0; i--){
            if(lineas[i].equals(lineaVieja)){
                lineas[i] = lineaNueva;
                break;
            }
        }
        codigo.setLength(0);
        for(String linea : lineas){
            codigo.append(linea).append("\n");
        }
    }

    /**
     * Retorna el código de tres direcciones acumulado hasta el momento.
     *
     * Entrada: ninguna.
     * Salida: String con todas las instrucciones generadas, separadas por saltos de línea.
     * Restricción: ninguna.
     */
    public String getCodigo(){
        return codigo.toString();
    }

    public void startCapture() {
        capturando = true;
        codigoCapturado.clear();
    }

    public void stopCapture() {
        capturando = false;
    }

    public List<String> getCapturedCode() {
        return new ArrayList<>(codigoCapturado);
    }

}