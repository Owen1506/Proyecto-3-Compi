package parser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.io.IOException;
import tabla.Simbolo;
import tabla.TablaManagement;

public class mipsGenerator {

    private StringBuilder datos = new StringBuilder();
    private StringBuilder texto = new StringBuilder();
    private int punteroStackGlobal = 0;
    private TablaManagement tablaSimbolos;
    private Map<String, Integer> offsets = new HashMap<>();

    // Registros enteros
    private Map<String, Integer> ultimoUso = new HashMap<>();
    private Map<String, String> tempARegistro = new HashMap<>();
    private Queue<String> registrosLibres = new LinkedList<>();

    // Registros flotantes
    private Map<String, Integer> ultimoUsoFloat = new HashMap<>();
    private Map<String, String> tempARegistroFloat = new HashMap<>();
    private Queue<String> registrosLibresFloat = new LinkedList<>();

    private int lineaActual = 0;
    private int contadorAux = 0;

    // Parámetros
    private Map<String, Integer> paramIndices = new HashMap<>();
    private Map<String, Boolean> paramEsFloat = new HashMap<>();
    private int numParametrosEnteros = 0;
    private int numParametrosFloat = 0;

    private boolean enRetorno = false;

    private int tamañoStackAcumulado = 0;      // calculado a partir de variables locales
    private int tamanoStackRequerido = 0;       // valor de prepareStack (alineado)
    private String nombreFuncionActual = "";
    private boolean stackAllocated = false;

    public void setTabla(TablaManagement t) { this.tablaSimbolos = t; }
    public TablaManagement getTabla() { return tablaSimbolos; }

    public void agregarDato(String linea) { datos.append(linea).append("\n"); }

    public void agregarTexto(String linea) { texto.append(linea).append("\n"); }

    public void declararVariable(String nombre, int tamaño) {
        if (tamaño > 1) {
            int resto = punteroStackGlobal % tamaño;
            if (resto != 0) {
                punteroStackGlobal += (tamaño - resto);
            }
        }
        offsets.put(nombre, punteroStackGlobal);
        punteroStackGlobal += tamaño;
        tamañoStackAcumulado = punteroStackGlobal;
    }

    public int obtenerOffset(String nombre) {
        return offsets.getOrDefault(nombre, -1);
    }

    public List<String> leerCodigo3D() throws IOException {
        return Files.readAllLines(Paths.get("cod3D.txt"));
    }

    private boolean esTemporal(String s) { return s.matches("t\\d+"); }
    private boolean esAuxiliar(String s) { return s.startsWith("_aux_"); }
    private boolean esAuxiliarFloat(String s) { return s.startsWith("_auxf_"); }

    private void calcularUltimoUso(List<String> lineas) {
        ultimoUso.clear();
        ultimoUsoFloat.clear();
        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i).trim();
            if (linea.isEmpty()) continue;
            String[] partes = linea.split(" ");

            if (partes.length >= 3 && partes[1].equals("=")) {
                String destino = partes[0];
                if (esTemporal(destino)) {
                    ultimoUso.putIfAbsent(destino, -1);
                }
            }
            for (String token : partes) {
                token = token.replaceAll("[,|<>()]", "");
                if (esTemporal(token)) {
                    ultimoUso.put(token, i);
                }
            }
            for (String token : partes) {
                if (token.contains("<|") && token.contains("|>")) {
                    String interior = token.substring(token.indexOf("<|") + 2, token.indexOf("|>"));
                    for (String op : interior.split(",")) {
                        if (esTemporal(op)) {
                            ultimoUso.put(op, i);
                        }
                    }
                }
            }
        }
    }

    private String asignarRegistro(String temp) {
        if (tempARegistro.containsKey(temp)) return tempARegistro.get(temp);
        if (registrosLibres.isEmpty()) {
            for (String t : new ArrayList<>(tempARegistro.keySet())) {
                if (esAuxiliar(t) && ultimoUso.getOrDefault(t, -1) < lineaActual) {
                    String reg = tempARegistro.remove(t);
                    registrosLibres.add(reg);
                    break;
                }
            }
            if (registrosLibres.isEmpty()) {
                throw new RuntimeException("No hay registros enteros libres");
            }
        }
        String reg = registrosLibres.poll();
        tempARegistro.put(temp, reg);
        return reg;
    }

    private String asignarRegistroFloat(String temp) {
        if (tempARegistroFloat.containsKey(temp)) return tempARegistroFloat.get(temp);
        if (registrosLibresFloat.isEmpty()) {
            for (String t : new ArrayList<>(tempARegistroFloat.keySet())) {
                if (esAuxiliarFloat(t) && ultimoUsoFloat.getOrDefault(t, -1) < lineaActual) {
                    String reg = tempARegistroFloat.remove(t);
                    registrosLibresFloat.add(reg);
                    break;
                }
            }
            if (registrosLibresFloat.isEmpty()) {
                throw new RuntimeException("No hay registros flotantes libres");
            }
        }
        String reg = registrosLibresFloat.poll();
        tempARegistroFloat.put(temp, reg);
        return reg;
    }

    private void liberarRegSiUltimoUso(String temp) {
        if ((!esTemporal(temp) && !esAuxiliar(temp)) || !ultimoUso.containsKey(temp)) return;
        if (ultimoUso.getOrDefault(temp, -1) == lineaActual && tempARegistro.containsKey(temp)) {
            String reg = tempARegistro.remove(temp);
            registrosLibres.add(reg);
        }
    }

    private void liberarRegFloatSiUltimoUso(String temp) {
        if ((!esTemporal(temp) && !esAuxiliarFloat(temp)) || !ultimoUsoFloat.containsKey(temp)) return;
        if (ultimoUsoFloat.getOrDefault(temp, -1) == lineaActual && tempARegistroFloat.containsKey(temp)) {
            String reg = tempARegistroFloat.remove(temp);
            registrosLibresFloat.add(reg);
        }
    }

    private void liberarAuxiliaresDeLineaActual() {
        Iterator<Map.Entry<String, String>> it = tempARegistro.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entrada = it.next();
            if (esAuxiliar(entrada.getKey()) && ultimoUso.getOrDefault(entrada.getKey(), -1) == lineaActual) {
                registrosLibres.add(entrada.getValue());
                it.remove();
            }
        }
        Iterator<Map.Entry<String, String>> itf = tempARegistroFloat.entrySet().iterator();
        while (itf.hasNext()) {
            Map.Entry<String, String> entrada = itf.next();
            if (esAuxiliarFloat(entrada.getKey()) && ultimoUsoFloat.getOrDefault(entrada.getKey(), -1) == lineaActual) {
                registrosLibresFloat.add(entrada.getValue());
                itf.remove();
            }
        }
    }

    private void liberarRegistroForzado(String reg) {
        if (reg.startsWith("$f")) {
            for (Map.Entry<String, String> e : tempARegistroFloat.entrySet()) {
                if (e.getValue().equals(reg)) {
                    tempARegistroFloat.remove(e.getKey());
                    registrosLibresFloat.add(reg);
                    return;
                }
            }
            registrosLibresFloat.add(reg);
        } else {
            for (Map.Entry<String, String> e : tempARegistro.entrySet()) {
                if (e.getValue().equals(reg)) {
                    tempARegistro.remove(e.getKey());
                    registrosLibres.add(reg);
                    return;
                }
            }
            registrosLibres.add(reg);
        }
    }

    private int getStackSizeAligned() {
        int total = Math.max(tamañoStackAcumulado, tamanoStackRequerido);
        return (total + 3) & ~3;
    }

    private void asignarStackSiNecesario() {
        if (!stackAllocated) {
            int total = getStackSizeAligned();
            if (total > 0) {
                agregarTexto("addi $sp, $sp, -" + total);
            }
            stackAllocated = true;
        }
    }

    private String cargarOperando(String op) {
        if (paramIndices.containsKey(op)) {
            int idx = paramIndices.get(op);
            boolean esFloat = paramEsFloat.get(op);
            if (esFloat) {
                String regArg = "$f" + (12 + idx * 2);
                String aux = "_auxf_param_" + (contadorAux++);
                String regTemp = asignarRegistroFloat(aux);
                ultimoUsoFloat.put(aux, lineaActual);
                agregarTexto("mov.s " + regTemp + ", " + regArg);
                return regTemp;
            } else {
                String regArg = "$a" + idx;
                String aux = "_aux_param_" + (contadorAux++);
                String regTemp = asignarRegistro(aux);
                ultimoUso.put(aux, lineaActual);
                agregarTexto("move " + regTemp + ", " + regArg);
                return regTemp;
            }
        }

        if (esTemporal(op)) {
            if (tempARegistroFloat.containsKey(op)) return tempARegistroFloat.get(op);
            if (tempARegistro.containsKey(op)) return tempARegistro.get(op);
            return asignarRegistro(op);
        }

        if (tablaSimbolos.existeEnHistorial(op)) {
            Simbolo s = tablaSimbolos.buscarEnHistorial(op);
            if (s.getTipo().equals("string")) {
                throw new RuntimeException("No se puede cargar un string como valor numérico: " + op);
            }
            boolean esFloat = s.getTipo().equals("float");
            boolean esByte = s.getTipo().equals("char") || s.getTipo().equals("bool");
            int off = obtenerOffset(op);
            if (off != -1) {
                asignarStackSiNecesario();
                if (esFloat) {
                    String aux = "_auxf_var_" + (contadorAux++);
                    String reg = asignarRegistroFloat(aux);
                    ultimoUsoFloat.put(aux, lineaActual);
                    agregarTexto("l.s " + reg + ", " + off + "($sp)");
                    return reg;
                } else if (esByte) {
                    String aux = "_aux_var_" + (contadorAux++);
                    String reg = asignarRegistro(aux);
                    ultimoUso.put(aux, lineaActual);
                    agregarTexto("lb " + reg + ", " + off + "($sp)");
                    return reg;
                } else {
                    String aux = "_aux_var_" + (contadorAux++);
                    String reg = asignarRegistro(aux);
                    ultimoUso.put(aux, lineaActual);
                    agregarTexto("lw " + reg + ", " + off + "($sp)");
                    return reg;
                }
            } else {
                if (esFloat) {
                    String aux = "_auxf_global_" + (contadorAux++);
                    String reg = asignarRegistroFloat(aux);
                    ultimoUsoFloat.put(aux, lineaActual);
                    agregarTexto("la " + reg + ", " + op);
                    agregarTexto("l.s " + reg + ", 0(" + reg + ")");
                    return reg;
                } else if (esByte) {
                    String aux = "_aux_global_" + (contadorAux++);
                    String reg = asignarRegistro(aux);
                    ultimoUso.put(aux, lineaActual);
                    agregarTexto("la " + reg + ", " + op);
                    agregarTexto("lb " + reg + ", 0(" + reg + ")");
                    return reg;
                } else {
                    String aux = "_aux_global_" + (contadorAux++);
                    String reg = asignarRegistro(aux);
                    ultimoUso.put(aux, lineaActual);
                    agregarTexto("la " + reg + ", " + op);
                    agregarTexto("lw " + reg + ", 0(" + reg + ")");
                    return reg;
                }
            }
        }

        if ((op.startsWith("'") && op.endsWith("'") && op.length() == 3) ||
            (op.length() == 1 && !Character.isDigit(op.charAt(0)) && !op.equals("."))) {
            int ascii = op.startsWith("'") ? (int) op.charAt(1) : (int) op.charAt(0);
            String aux = "_aux_lit_" + (contadorAux++);
            String reg = asignarRegistro(aux);
            ultimoUso.put(aux, lineaActual);
            agregarTexto("li " + reg + ", " + ascii);
            return reg;
        }

        if (op.contains(".")) {
            String aux = "_auxf_lit_" + (contadorAux++);
            String reg = asignarRegistroFloat(aux);
            ultimoUsoFloat.put(aux, lineaActual);
            agregarTexto("li.s " + reg + ", " + op);
            return reg;
        }

        String aux = "_aux_lit_" + (contadorAux++);
        String reg = asignarRegistro(aux);
        ultimoUso.put(aux, lineaActual);
        agregarTexto("li " + reg + ", " + op);
        return reg;
    }

    private void almacenarEnVariable(String nombre, String regFuente) {
        if (!tablaSimbolos.existeEnHistorial(nombre)) return;
        Simbolo s = tablaSimbolos.buscarEnHistorial(nombre);
        if (s.getTipo().equals("string")) return;
        boolean esFloat = s.getTipo().equals("float");
        boolean esByte = s.getTipo().equals("char") || s.getTipo().equals("bool");
        int off = obtenerOffset(nombre);
        if (off != -1) {
            asignarStackSiNecesario();
            if (esFloat) {
                agregarTexto("s.s " + regFuente + ", " + off + "($sp)");
            } else if (esByte) {
                agregarTexto("sb " + regFuente + ", " + off + "($sp)");
            } else {
                agregarTexto("sw " + regFuente + ", " + off + "($sp)");
            }
        } else {
            String aux = esFloat ? "_auxf_global_store_" : "_aux_global_store_";
            aux += (contadorAux++);
            String regDir;
            if (esFloat) {
                regDir = asignarRegistroFloat(aux);
                ultimoUsoFloat.put(aux, lineaActual);
            } else {
                regDir = asignarRegistro(aux);
                ultimoUso.put(aux, lineaActual);
            }
            agregarTexto("la " + regDir + ", " + nombre);
            if (esFloat) {
                agregarTexto("s.s " + regFuente + ", 0(" + regDir + ")");
            } else if (esByte) {
                agregarTexto("sb " + regFuente + ", 0(" + regDir + ")");
            } else {
                agregarTexto("sw " + regFuente + ", 0(" + regDir + ")");
            }
        }
    }

    private boolean esEnteroConstante(String token) {
        try {
            Integer.parseInt(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String calcularDireccionArreglo(String nombreArray, String filaToken, String colToken) {
        Simbolo arrSim = tablaSimbolos.buscarEnHistorial(nombreArray);
        if (arrSim == null) throw new RuntimeException("Arreglo no encontrado: " + nombreArray);
        int columnas = arrSim.getColumnas();

        if (esEnteroConstante(filaToken) && esEnteroConstante(colToken)) {
            int fila = Integer.parseInt(filaToken);
            int col = Integer.parseInt(colToken);
            int offset = (fila * columnas + col) * 4;
            String aux = "_arr_const_offset_" + (contadorAux++);
            String regBase = asignarRegistro(aux);
            ultimoUso.put(aux, lineaActual);
            agregarTexto("la " + regBase + ", " + nombreArray);
            return "CONST:" + regBase + ":" + offset;
        }

        String regBase   = asignarRegistro("_arr_base_" + (contadorAux++));
        String regFila   = cargarOperando(filaToken);
        String regCol    = cargarOperando(colToken);
        String regColumnas = asignarRegistro("_arr_cols_" + (contadorAux++));

        ultimoUso.put("_arr_base_" + (contadorAux-2), lineaActual);
        ultimoUso.put("_arr_cols_" + (contadorAux-1), lineaActual);

        agregarTexto("la " + regBase + ", " + nombreArray);
        agregarTexto("li " + regColumnas + ", " + columnas);
        agregarTexto("mul " + regFila + ", " + regFila + ", " + regColumnas);
        agregarTexto("add " + regFila + ", " + regFila + ", " + regCol);
        agregarTexto("sll " + regFila + ", " + regFila + ", 2");
        agregarTexto("add " + regFila + ", " + regBase + ", " + regFila);

        liberarRegistroForzado(regBase);
        liberarRegistroForzado(regColumnas);
        liberarRegistroForzado(regCol);
        return regFila;
    }

    private void ejecutarArrStore(String nombreArray, String fila, String col, String valor) {
        String dirInfo = calcularDireccionArreglo(nombreArray, fila, col);
        String regValor = cargarOperando(valor);
        if (dirInfo.startsWith("CONST:")) {
            String[] partes = dirInfo.split(":");
            String base = partes[1];
            int offset = Integer.parseInt(partes[2]);
            agregarTexto("sw " + regValor + ", " + offset + "(" + base + ")");
            liberarRegistroForzado(base);
        } else {
            agregarTexto("sw " + regValor + ", 0(" + dirInfo + ")");
            liberarRegistroForzado(dirInfo);
        }
        if (esTemporal(valor)) liberarRegSiUltimoUso(valor);
        else if (regValor.startsWith("$f")) liberarRegFloatSiUltimoUso(valor);
    }

    private void ejecutarArrLoad(String destino, String nombreArray, String fila, String col) {
        String dirInfo = calcularDireccionArreglo(nombreArray, fila, col);
        String regDest = asignarRegistro(destino);
        if (dirInfo.startsWith("CONST:")) {
            String[] partes = dirInfo.split(":");
            String base = partes[1];
            int offset = Integer.parseInt(partes[2]);
            agregarTexto("lw " + regDest + ", " + offset + "(" + base + ")");
            liberarRegistroForzado(base);
        } else {
            agregarTexto("lw " + regDest + ", 0(" + dirInfo + ")");
            liberarRegistroForzado(dirInfo);
        }
    }

    public String generarCodigo() throws IOException {
        List<String> lineas = leerCodigo3D();
        registrosLibres.addAll(Arrays.asList("$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$t8", "$t9"));
        for (int i = 0; i < 10; i++) registrosLibresFloat.add("$f" + i);
        calcularUltimoUso(lineas);

        for (lineaActual = 0; lineaActual < lineas.size(); lineaActual++) {
            String linea = lineas.get(lineaActual).trim();
            if (linea.isEmpty()) continue;

            if (enRetorno) {
                if (linea.endsWith(":") && !linea.matches("L\\d+:.*")) {
                    enRetorno = false;
                } else {
                    continue;
                }
            }

            String[] partes = linea.split(" ");

            // print_string
            if (partes[0].equals("print_string")) {
                asignarStackSiNecesario();
                String argumento = linea.substring(linea.indexOf(' ') + 1);
                imprimirString(argumento);
                liberarAuxiliaresDeLineaActual();
                continue;
            }

            // Asignación de string con espacios
            if (partes.length > 3 && partes[1].equals("=") && tablaSimbolos.existeEnHistorial(partes[0])) {
                Simbolo s = tablaSimbolos.buscarEnHistorial(partes[0]);
                if (s.getTipo().equals("string")) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 2; i < partes.length; i++) {
                        if (i > 2) sb.append(" ");
                        sb.append(partes[i]);
                    }
                    datos.append(".align 2\n");
                    agregarDato(partes[0] + ": .asciiz \"" + sb.toString() + "\"");
                    liberarAuxiliaresDeLineaActual();
                    continue;
                }
            }

            // Reserva del stack justo antes de la primera instrucción real
            if (!stackAllocated && !partes[0].equals("prepareStack") &&
                !partes[0].startsWith("data_") && !partes[0].startsWith("param_") &&
                !partes[0].endsWith(":")) {
                asignarStackSiNecesario();
            }

            if (partes.length == 1) {
                if (partes[0].endsWith(":")) {
                    agregarTexto(partes[0]);
                    if (!partes[0].startsWith("L")) {
                        nombreFuncionActual = partes[0].replace(":", "");
                        tamañoStackAcumulado = 0;
                        tamanoStackRequerido = 0;
                        punteroStackGlobal = 0;
                        offsets.clear();
                        paramIndices.clear();
                        paramEsFloat.clear();
                        numParametrosEnteros = 0;
                        numParametrosFloat = 0;
                        enRetorno = false;
                        stackAllocated = false;
                    }
                } else if (partes[0].startsWith("data_")) {
                    procesarDeclaracionDatos(partes[0]);
                } else if (partes[0].startsWith("param_")) {
                    procesarParametro(partes[0]);
                }
            }
            else if (partes.length == 2) {
                if (partes[0].equals("prepareStack")) {
                    int val = Integer.parseInt(partes[1]);
                    tamanoStackRequerido = (val + 3) & ~3;
                } else if (partes[0].equals("clearStack")) {
                    int total = getStackSizeAligned();
                    if (total > 0) {
                        agregarTexto("addi $sp, $sp, " + total);
                    }
                } else if (partes[0].equals("call")) {
                    agregarTexto("jal " + partes[1]);
                } else if (partes[0].equals("push")) {
                    String regVal = cargarOperando(partes[1]);
                    agregarTexto("move $a" + numParametrosEnteros + ", " + regVal);
                    numParametrosEnteros++;
                } else if (partes[0].equals("goto")) {
                    agregarTexto("j " + partes[1]);
                } else if (partes[0].equals("print")) {
                    String op = partes[1];
                    if (tablaSimbolos.existeEnHistorial(op)) {
                        Simbolo s = tablaSimbolos.buscarEnHistorial(op);
                        String tipo = s.getTipo();
                        switch (tipo) {
                            case "string":
                                imprimirString(op);
                                break;
                            case "float":
                                String regFloat = cargarOperando(op);
                                agregarTexto("mov.s $f12, " + regFloat);
                                agregarTexto("li $v0, 2");
                                agregarTexto("syscall");
                                break;
                            case "char":
                                String regChar = cargarOperando(op);
                                agregarTexto("move $a0, " + regChar);
                                agregarTexto("li $v0, 11");
                                agregarTexto("syscall");
                                break;
                            default: // int, bool
                                String regInt = cargarOperando(op);
                                agregarTexto("move $a0, " + regInt);
                                agregarTexto("li $v0, 1");
                                agregarTexto("syscall");
                        }
                    } else {
                        // Literal
                        if (esLiteralCaracter(op)) {
                            String reg = cargarOperando(op);
                            agregarTexto("move $a0, " + reg);
                            agregarTexto("li $v0, 11");
                            agregarTexto("syscall");
                        } else if (op.contains(".")) {
                            String reg = cargarOperando(op);
                            agregarTexto("mov.s $f12, " + reg);
                            agregarTexto("li $v0, 2");
                            agregarTexto("syscall");
                        } else {
                            String reg = cargarOperando(op);
                            agregarTexto("move $a0, " + reg);
                            agregarTexto("li $v0, 1");
                            agregarTexto("syscall");
                        }
                    }
                }
            }
            else if (partes.length == 3) {
                if (partes[1].equals("=")) {
                    String destino = partes[0];
                    String fuente = partes[2];
                    if (esTemporal(destino)) {
                        String regFuente = cargarOperando(fuente);
                        boolean esFloat = regFuente.startsWith("$f");
                        if (esFloat) {
                            String regDest = asignarRegistroFloat(destino);
                            agregarTexto("mov.s " + regDest + ", " + regFuente);
                        } else {
                            String regDest = asignarRegistro(destino);
                            agregarTexto("move " + regDest + ", " + regFuente);
                        }
                        if (esTemporal(fuente)) {
                            if (esFloat) liberarRegFloatSiUltimoUso(fuente);
                            else liberarRegSiUltimoUso(fuente);
                        }
                    } else if (tablaSimbolos.existeEnHistorial(destino)) {
                        Simbolo sd = tablaSimbolos.buscarEnHistorial(destino);
                        if (sd.getTipo().equals("string")) {
                            datos.append(".align 2\n");
                            agregarDato(destino + ": .asciiz \"" + fuente + "\"");
                        } else {
                            String regFuente = cargarOperando(fuente);
                            almacenarEnVariable(destino, regFuente);
                            if (esTemporal(fuente)) {
                                if (regFuente.startsWith("$f")) liberarRegFloatSiUltimoUso(fuente);
                                else liberarRegSiUltimoUso(fuente);
                            }
                        }
                    } else if (destino.equals("return")) {
                        String regVal = cargarOperando(fuente);
                        if (regVal.startsWith("$f")) {
                            agregarTexto("mov.s $f0, " + regVal);
                        } else {
                            agregarTexto("move $v0, " + regVal);
                        }
                        agregarTexto("jr $ra");
                        enRetorno = true;
                    }
                } else if (partes[0].equals("return")) {
                    String regVal = cargarOperando(partes[1]);
                    if (regVal.startsWith("$f")) {
                        agregarTexto("mov.s $f0, " + regVal);
                    } else {
                        agregarTexto("move $v0, " + regVal);
                    }
                    agregarTexto("jr $ra");
                    enRetorno = true;
                } else if (partes[0].endsWith(":") && partes[1].equals(".space")) {
                    if (datos.indexOf(partes[0]) == -1) {
                        datos.append(".align 2\n");
                        agregarDato(linea);
                    }
                }
            }
            else if (partes.length == 4) {
                if (partes[0].equals("if") && partes[2].equals("goto")) {
                    String condicion = partes[1];
                    String etiqueta = partes[3];
                    String operador = condicion.substring(0, condicion.indexOf("<"));
                    String interior = condicion.substring(condicion.indexOf("<|") + 2, condicion.indexOf("|>"));
                    String[] operandos = interior.split(",");
                    String reg1 = cargarOperando(operandos[0]);
                    String reg2 = cargarOperando(operandos[1]);
                    boolean floatOp = reg1.startsWith("$f") || reg2.startsWith("$f");
                    if (floatOp) {
                        if (operador.equals("n_equal")) {
                            agregarTexto("c.eq.s " + reg1 + ", " + reg2);
                            agregarTexto("bc1f " + etiqueta);
                        } else {
                            String condMips;
                            switch (operador) {
                                case "equal":       condMips = "c.eq.s"; break;
                                case "less_t":      condMips = "c.lt.s"; break;
                                case "less_te":     condMips = "c.le.s"; break;
                                case "greather_t":  condMips = "c.lt.s"; String tmp = reg1; reg1 = reg2; reg2 = tmp; break;
                                case "greather_te": condMips = "c.le.s"; String tmp2 = reg1; reg1 = reg2; reg2 = tmp2; break;
                                default: condMips = "c.eq.s";
                            }
                            agregarTexto(condMips + " " + reg1 + ", " + reg2);
                            agregarTexto("bc1t " + etiqueta);
                        }
                    } else {
                        switch (operador) {
                            case "equal":       agregarTexto("beq " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "n_equal":     agregarTexto("bne " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "less_t":      agregarTexto("blt " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "less_te":     agregarTexto("ble " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "greather_t":  agregarTexto("bgt " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "greather_te": agregarTexto("bge " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                        }
                    }
                }
            }
            else if (partes.length == 5) {
                if (partes[1].equals("=") && esOperadorAritmetico(partes[3])) {
                    String dest = partes[0], op1 = partes[2], oper = partes[3], op2 = partes[4];
                    String regOp1 = cargarOperando(op1);
                    String regOp2 = cargarOperando(op2);
                    boolean floatOp = regOp1.startsWith("$f") || regOp2.startsWith("$f");
                    if (floatOp) {
                        String regDest = asignarRegistroFloat(dest);
                        switch (oper) {
                            case "+": agregarTexto("add.s " + regDest + ", " + regOp1 + ", " + regOp2); break;
                            case "-": agregarTexto("sub.s " + regDest + ", " + regOp1 + ", " + regOp2); break;
                            case "*": agregarTexto("mul.s " + regDest + ", " + regOp1 + ", " + regOp2); break;
                            case "/": agregarTexto("div.s " + regDest + ", " + regOp1 + ", " + regOp2); break;
                        }
                    } else {
                        String regDest = asignarRegistro(dest);
                        switch (oper) {
                            case "+": agregarTexto("add " + regDest + ", " + regOp1 + ", " + regOp2); break;
                            case "-": agregarTexto("sub " + regDest + ", " + regOp1 + ", " + regOp2); break;
                            case "*": agregarTexto("mul " + regDest + ", " + regOp1 + ", " + regOp2); break;
                            case "/": agregarTexto("div " + regDest + ", " + regOp1 + ", " + regOp2); break;
                        }
                    }
                } else if (partes[0].equals("arr_store")) {
                    ejecutarArrStore(partes[1].replace(",", ""), partes[2].replace(",", ""), partes[3].replace(",", ""), partes[4]);
                }
            }
            else if (partes.length == 6) {
                // Formato: destino = arr_load array, fila, columna
                if (partes[1].equals("=") && partes[2].equals("arr_load")) {
                    String destino = partes[0];
                    String nombreArray = partes[3].replace(",", "");
                    String fila = partes[4].replace(",", "");
                    String col  = partes[5];
                    ejecutarArrLoad(destino, nombreArray, fila, col);
                } else if (partes[0].equals("if") && partes[4].equals("goto")) {
                    String op1 = partes[1], relop = partes[2], op2 = partes[3], etiqueta = partes[5];
                    String reg1 = cargarOperando(op1);
                    String reg2 = cargarOperando(op2);
                    boolean floatOp = reg1.startsWith("$f") || reg2.startsWith("$f");
                    if (floatOp) {
                        if (relop.equals("!=")) {
                            agregarTexto("c.eq.s " + reg1 + ", " + reg2);
                            agregarTexto("bc1f " + etiqueta);
                        } else {
                            String condMips;
                            switch (relop) {
                                case "==": condMips = "c.eq.s"; break;
                                case "<":  condMips = "c.lt.s"; break;
                                case "<=": condMips = "c.le.s"; break;
                                case ">":  condMips = "c.lt.s"; String tmp = reg1; reg1 = reg2; reg2 = tmp; break;
                                case ">=": condMips = "c.le.s"; String tmp2 = reg1; reg1 = reg2; reg2 = tmp2; break;
                                default: condMips = "c.eq.s";
                            }
                            agregarTexto(condMips + " " + reg1 + ", " + reg2);
                            agregarTexto("bc1t " + etiqueta);
                        }
                    } else {
                        switch (relop) {
                            case "==": agregarTexto("beq " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "!=": agregarTexto("bne " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "<":  agregarTexto("blt " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "<=": agregarTexto("ble " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case ">":  agregarTexto("bgt " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case ">=": agregarTexto("bge " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                        }
                    }
                }
            }

            liberarAuxiliaresDeLineaActual();
        }

        tempARegistro.clear();
        registrosLibres.clear();
        tempARegistroFloat.clear();
        registrosLibresFloat.clear();
        return generarPrograma();
    }

    private void imprimirString(String str) {
        if (tablaSimbolos.existeEnHistorial(str)) {
            Simbolo s = tablaSimbolos.buscarEnHistorial(str);
            if (s.getTipo().equals("string")) {
                agregarTexto("la $a0, " + str);
                agregarTexto("li $v0, 4");
                agregarTexto("syscall");
                return;
            }
        }
        String etiqueta = "_str_lit_" + (contadorAux++);
        datos.append(".align 2\n");
        agregarDato(etiqueta + ": .asciiz \"" + str + "\"");
        agregarTexto("la $a0, " + etiqueta);
        agregarTexto("li $v0, 4");
        agregarTexto("syscall");
    }

    private boolean esOperadorAritmetico(String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/");
    }

    private boolean esLiteralCaracter(String op) {
        return (op.startsWith("'") && op.endsWith("'") && op.length() == 3) ||
               (op.length() == 1 && !Character.isDigit(op.charAt(0)) && !op.equals("."));
    }

    private void procesarDeclaracionDatos(String token) {
        String contenido = token.substring(5);
        if (contenido.contains("_array_")) {
            // El .space se agrega en la línea correspondiente
        } else {
            String[] partes = contenido.split("_", 2);
            if (partes.length < 2) return;
            String tipo = partes[0];
            String nombre = partes[1];
            if (nombre.isEmpty()) return;
            switch (tipo) {
                case "char":
                case "bool":
                    declararVariable(nombre, 1);
                    break;
                case "int":
                case "float":
                    declararVariable(nombre, 4);
                    break;
                case "string":
                    break;
            }
        }
    }

    private void procesarParametro(String token) {
        String[] partes = token.split("_", 3);
        if (partes.length < 3) return;
        String tipo = partes[1];
        String nombre = partes[2];
        if (tipo.equals("float")) {
            if (numParametrosFloat >= 2) throw new RuntimeException("Demasiados parámetros flotantes");
            paramIndices.put(nombre, numParametrosFloat);
            paramEsFloat.put(nombre, true);
            numParametrosFloat++;
        } else {
            if (numParametrosEnteros >= 4) throw new RuntimeException("Demasiados parámetros enteros");
            paramIndices.put(nombre, numParametrosEnteros);
            paramEsFloat.put(nombre, false);
            numParametrosEnteros++;
        }
    }

    public String generarPrograma() {
        StringBuilder resultado = new StringBuilder();
        resultado.append(".data\n");
        resultado.append(datos);
        resultado.append("\n.text\n");
        resultado.append(".globl main\n");
        resultado.append("main:\n");
        resultado.append(texto);
        resultado.append("li $v0, 10\n");
        resultado.append("syscall\n");
        return resultado.toString();
    }

    public void imprimirOffsets() {
        System.out.println("\n=== OFFSETS DEL STACK ===");
        for (Map.Entry<String, Integer> entry : offsets.entrySet()) {
            System.out.println("Variable: " + entry.getKey() + " | Offset: " + entry.getValue());
        }
        System.out.println("Stack total reservado: " + punteroStackGlobal + " bytes");
        System.out.println("=========================\n");
    }
}