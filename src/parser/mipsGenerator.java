package parser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.io.IOException;
import tabla.Simbolo;
import tabla.TablaManagement;

public class mipsGenerator {

    private static class FrameLayout {
        final boolean esMain;
        final Map<String, Integer> offsets = new LinkedHashMap<>();
        final Map<String, String> tipos = new HashMap<>();
        int nextOffset;

        FrameLayout(boolean esMain) {
            this.esMain = esMain;
            this.nextOffset = esMain ? 0 : 4;   // slot 0 reservado para $ra
        }

        void agregarVariable(String nombre, int tamaño, String tipo) {
            if (tamaño > 1 && nextOffset % tamaño != 0) {
                nextOffset += tamaño - (nextOffset % tamaño);
            }
            offsets.put(nombre, nextOffset);
            tipos.put(nombre, tipo);
            nextOffset += tamaño;
        }

        int tamaño() {
            return (nextOffset + 3) & ~3;
        }

        int offset(String nombre) {
            Integer off = offsets.get(nombre);
            if (off == null)
                throw new RuntimeException("Variable '" + nombre + "' no encontrada en el frame de " + (esMain ? "main" : "funcion"));
            return off;
        }

        boolean tiene(String nombre) {
            return offsets.containsKey(nombre);
        }

        String tipo(String nombre) {
            return tipos.get(nombre);
        }
    }

    private StringBuilder datos = new StringBuilder();
    private StringBuilder texto = new StringBuilder();
    private TablaManagement tablaSimbolos;

    private FrameLayout frameActual = null;
    private String nombreFuncionActual = "";
    private boolean stackAllocated = false;
    private boolean preScanHecho = false;

    private Map<String, Integer> ultimoUso = new HashMap<>();
    private Map<String, String> tempARegistro = new HashMap<>();
    private Queue<String> registrosLibres = new LinkedList<>();
    private Map<String, Integer> ultimoUsoFloat = new HashMap<>();
    private Map<String, String> tempARegistroFloat = new HashMap<>();
    private Queue<String> registrosLibresFloat = new LinkedList<>();

    private int lineaActual = 0;
    private int contadorAux = 0;
    private int contStringLit = 0;
    private List<String> paramsNombres = new ArrayList<>();
    private List<String> paramsRegs = new ArrayList<>();

    private List<String> argsPendientes = new ArrayList<>();
    private List<Boolean> argsFloatPendientes = new ArrayList<>();

    private String tipoRetornoLlamada = "int";
    private boolean funcionActualTieneRetorno = false;

    private boolean potenciaUsada = false;
    private boolean potenciaFloatUsada = false;

    private Map<String, String> stringVars = new LinkedHashMap<>();

    private String convertirAFloat(String reg) {
        if (reg.startsWith("$f")) return reg;

        String temp = "_auxf_cvrt_" + (contadorAux++);
        String freg = asignarRegistroFloat(temp);
        ultimoUsoFloat.put(temp, lineaActual);
        agregarTexto("mtc1 " + reg + ", " + freg);
        agregarTexto("cvt.s.w " + freg + ", " + freg);
        return freg;
    }

    public void setTabla(TablaManagement t) { this.tablaSimbolos = t; }
    public TablaManagement getTabla() { return tablaSimbolos; }

    public void agregarDato(String linea) { datos.append(linea).append("\n"); }
    public void agregarTexto(String linea) { texto.append(linea).append("\n"); }

    public List<String> leerCodigo3D() throws IOException {
        return Files.readAllLines(Paths.get("cod3D.txt"));
    }

    private boolean esTemporal(String s) { return s.matches("t\\d+"); }
    private boolean esAuxiliar(String s) { return s.startsWith("_aux_"); }
    private boolean esAuxiliarFloat(String s) { return s.startsWith("_auxf_"); }

    private void calcularUltimoUso(List<String> lineas) {
        ultimoUso.clear(); ultimoUsoFloat.clear();
        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i).trim();
            if (linea.isEmpty()) continue;
            String[] partes = linea.split(" ");
            if (partes.length >= 3 && partes[1].equals("=") && esTemporal(partes[0]))
                ultimoUso.putIfAbsent(partes[0], -1);
            for (String token : partes) {
                token = token.replaceAll("[,|<>()]", "");
                if (esTemporal(token)) ultimoUso.put(token, i);
            }
            for (String token : partes) {
                if (token.contains("<|") && token.contains("|>")) {
                    String interior = token.substring(token.indexOf("<|")+2, token.indexOf("|>"));
                    for (String op : interior.split(","))
                        if (esTemporal(op)) ultimoUso.put(op, i);
                }
            }
        }
    }

    private String asignarRegistro(String temp) {
        if (tempARegistro.containsKey(temp)) return tempARegistro.get(temp);
        if (registrosLibres.isEmpty()) {
            for (String t : new ArrayList<>(tempARegistro.keySet()))
                if (esAuxiliar(t) && ultimoUso.getOrDefault(t,-1) < lineaActual) {
                    registrosLibres.add(tempARegistro.remove(t)); break;
                }
            if (registrosLibres.isEmpty())
                for (String t : new ArrayList<>(tempARegistro.keySet()))
                    if (esTemporal(t) && ultimoUso.getOrDefault(t,-1) < lineaActual) {
                        registrosLibres.add(tempARegistro.remove(t)); break;
                    }
            if (registrosLibres.isEmpty()) throw new RuntimeException("No hay registros enteros libres");
        }
        String reg = registrosLibres.poll();
        tempARegistro.put(temp, reg);
        return reg;
    }

    private String asignarRegistroFloat(String temp) {
        if (tempARegistroFloat.containsKey(temp)) return tempARegistroFloat.get(temp);
        if (registrosLibresFloat.isEmpty()) {
            for (String t : new ArrayList<>(tempARegistroFloat.keySet()))
                if (esAuxiliarFloat(t) && ultimoUsoFloat.getOrDefault(t,-1) < lineaActual) {
                    registrosLibresFloat.add(tempARegistroFloat.remove(t)); break;
                }
            if (registrosLibresFloat.isEmpty())
                for (String t : new ArrayList<>(tempARegistroFloat.keySet()))
                    if (esTemporal(t) && ultimoUsoFloat.getOrDefault(t,-1) < lineaActual) {
                        registrosLibresFloat.add(tempARegistroFloat.remove(t)); break;
                    }
            if (registrosLibresFloat.isEmpty()) throw new RuntimeException("No hay registros flotantes libres");
        }
        String reg = registrosLibresFloat.poll();
        tempARegistroFloat.put(temp, reg);
        return reg;
    }

    private void liberarRegSiUltimoUso(String temp) {
        if (!esTemporal(temp)) return;
        if (ultimoUso.getOrDefault(temp,-1) == lineaActual && tempARegistro.containsKey(temp))
            registrosLibres.add(tempARegistro.remove(temp));
    }
    private void liberarRegFloatSiUltimoUso(String temp) {
        if (!esTemporal(temp)) return;
        if (ultimoUsoFloat.getOrDefault(temp,-1) == lineaActual && tempARegistroFloat.containsKey(temp))
            registrosLibresFloat.add(tempARegistroFloat.remove(temp));
    }
    private void liberarAuxiliarDespuesDeUso(String reg) {
        if (reg == null) return;
        if (reg.startsWith("$f")) {
            for (Map.Entry<String,String> e : tempARegistroFloat.entrySet())
                if (e.getValue().equals(reg) && esAuxiliarFloat(e.getKey())) {
                    registrosLibresFloat.add(tempARegistroFloat.remove(e.getKey())); return;
                }
        } else if (reg.startsWith("$t")) {
            for (Map.Entry<String,String> e : tempARegistro.entrySet())
                if (e.getValue().equals(reg) && esAuxiliar(e.getKey())) {
                    registrosLibres.add(tempARegistro.remove(e.getKey())); return;
                }
        }
    }
    private void liberarAuxiliaresDeLineaActual() {
        Iterator<Map.Entry<String,String>> it = tempARegistro.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String,String> e = it.next();
            if (esAuxiliar(e.getKey()) && ultimoUso.getOrDefault(e.getKey(),-1) == lineaActual)
                { registrosLibres.add(e.getValue()); it.remove(); }
        }
        Iterator<Map.Entry<String,String>> itf = tempARegistroFloat.entrySet().iterator();
        while (itf.hasNext()) {
            Map.Entry<String,String> e = itf.next();
            if (esAuxiliarFloat(e.getKey()) && ultimoUsoFloat.getOrDefault(e.getKey(),-1) == lineaActual)
                { registrosLibresFloat.add(e.getValue()); itf.remove(); }
        }
    }
    private void liberarRegistroForzado(String reg) {
        if (reg.startsWith("$f")) {
            for (Map.Entry<String,String> e : tempARegistroFloat.entrySet())
                if (e.getValue().equals(reg)) { registrosLibresFloat.add(tempARegistroFloat.remove(e.getKey())); return; }
            registrosLibresFloat.add(reg);
        } else {
            for (Map.Entry<String,String> e : tempARegistro.entrySet())
                if (e.getValue().equals(reg)) { registrosLibres.add(tempARegistro.remove(e.getKey())); return; }
            registrosLibres.add(reg);
        }
    }

    private void emitirEpilogo() {
        if (!stackAllocated || frameActual == null) return;
        int total = frameActual.tamaño();
        if (!nombreFuncionActual.equals("main") && total > 0) {
            agregarTexto("lw $ra, 0($sp)");
        }
        if (total > 0) {
            agregarTexto("addi $sp, $sp, " + total);
        }
        agregarTexto("jr $ra");
    }

    private void preScanFuncion(List<String> lineas, int inicio) {
        boolean esMain = nombreFuncionActual.equals("main");
        frameActual = new FrameLayout(esMain);
        paramsNombres.clear();
        paramsRegs.clear();

        int enteros = 0, floats = 0;
        for (int i = inicio + 1; i < lineas.size(); i++) {
            String linea = lineas.get(i).trim();
            if (linea.isEmpty()) continue;
            String[] partes = linea.split(" ");
            if (partes.length == 1 && partes[0].endsWith(":") && !partes[0].startsWith("L")) break;

            if (partes.length == 1 && partes[0].startsWith("data_")) {
                String contenido = partes[0].substring(5);
                if (contenido.contains("_array_")) continue;
                String[] p = contenido.split("_", 2);
                if (p.length < 2) continue;
                String tipo = p[0], nombre = p[1];
                if (nombre.isEmpty()) continue;
                if (tipo.equals("string")) continue;
                int tam = (tipo.equals("char") || tipo.equals("bool")) ? 1 : 4;
                frameActual.agregarVariable(nombre, tam, tipo);
            } else if (partes.length == 1 && partes[0].startsWith("param_")) {
                String[] p = partes[0].split("_", 3);
                if (p.length < 3) continue;
                String tipo = p[1], nombre = p[2];
                boolean esFloat = tipo.equals("float");
                frameActual.agregarVariable(nombre, 4, tipo);
                paramsNombres.add(nombre);
                String reg;
                if (esFloat) {
                    if (floats < 2) reg = "$f" + (12 + floats * 2);
                    else reg = "stack";
                    floats++;
                } else {
                    if (enteros < 4) reg = "$a" + enteros;
                    else reg = "stack";
                    enteros++;
                }
                paramsRegs.add(reg);
            }
        }
        preScanHecho = true;
    }

    private void asignarStackSiNecesario() {
        if (stackAllocated || frameActual == null) return;
        int total = frameActual.tamaño();
        if (total > 0) {
            agregarTexto("addi $sp, $sp, -" + total);
            if (!nombreFuncionActual.equals("main")) {
                agregarTexto("sw $ra, 0($sp)");
            }
        }
        for (int i = 0; i < paramsNombres.size(); i++) {
            String nombre = paramsNombres.get(i);
            String reg = paramsRegs.get(i);
            int off = frameActual.offset(nombre);
            if (reg.startsWith("$f")) {
                agregarTexto("s.s " + reg + ", " + off + "($sp)");
            } else if (!reg.equals("stack")) {
                agregarTexto("sw " + reg + ", " + off + "($sp)");
            }
        }
        stackAllocated = true;
    }

    private String cargarOperando(String op) {
        if (esTemporal(op)) {
            if (tempARegistroFloat.containsKey(op)) return tempARegistroFloat.get(op);
            if (tempARegistro.containsKey(op)) return tempARegistro.get(op);
            if (tablaSimbolos != null && tablaSimbolos.existeEnHistorial(op)
                    && tablaSimbolos.buscarEnHistorial(op).getTipo().equals("float"))
                return asignarRegistroFloat(op);
            return asignarRegistro(op);
        }

        if (frameActual != null && frameActual.tiene(op)) {
            String tipo = frameActual.tipo(op);
            boolean esFloat = tipo.equals("float");
            boolean esByte  = tipo.equals("char") || tipo.equals("bool");
            int off = frameActual.offset(op);
            if (!stackAllocated) asignarStackSiNecesario();
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
        }

        if (tablaSimbolos.existeEnHistorial(op)) {
            Simbolo s = tablaSimbolos.buscarEnHistorial(op);
            if (s.getTipo().equals("string")) throw new RuntimeException("No se puede cargar string como numérico");
            boolean esFloat = s.getTipo().equals("float");
            boolean esByte  = s.getTipo().equals("char") || s.getTipo().equals("bool");
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

        if (op.matches("\\d+//\\d+")) {
            String[] division = op.split("//");
            double num = Double.parseDouble(division[0]);
            double den = Double.parseDouble(division[1]);
            String aux = "_auxf_lit_" + (contadorAux++);
            String reg = asignarRegistroFloat(aux);
            ultimoUsoFloat.put(aux, lineaActual);
            agregarTexto("li.s " + reg + ", " + (num/den));
            return reg;
        }
        if (op.matches("\\d+[eE]\\d+")) {
            double val = Double.parseDouble(op);
            String aux = "_aux_lit_" + (contadorAux++);
            String reg = asignarRegistro(aux);
            ultimoUso.put(aux, lineaActual);
            agregarTexto("li " + reg + ", " + (int)val);
            return reg;
        }
        if ((op.startsWith("'") && op.endsWith("'") && op.length()==3) ||
            (op.length()==1 && !Character.isDigit(op.charAt(0)) && !op.equals("."))) {
            int ascii = op.startsWith("'") ? (int)op.charAt(1) : (int)op.charAt(0);
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
        if (frameActual != null && frameActual.tiene(nombre)) {
            String tipo = frameActual.tipo(nombre);
            if (tipo.equals("string")) return;
            boolean esFloat = tipo.equals("float");
            boolean esByte  = tipo.equals("char") || tipo.equals("bool");
            int off = frameActual.offset(nombre);
            if (!stackAllocated) asignarStackSiNecesario();
            if (esFloat) agregarTexto("s.s " + regFuente + ", " + off + "($sp)");
            else if (esByte) agregarTexto("sb " + regFuente + ", " + off + "($sp)");
            else agregarTexto("sw " + regFuente + ", " + off + "($sp)");
            return;
        }
        if (!tablaSimbolos.existeEnHistorial(nombre)) return;
        Simbolo s = tablaSimbolos.buscarEnHistorial(nombre);
        if (s.getTipo().equals("string")) return;
        boolean esFloat = s.getTipo().equals("float");
        boolean esByte  = s.getTipo().equals("char") || s.getTipo().equals("bool");
        String aux = (esFloat ? "_auxf_global_store_" : "_aux_global_store_") + (contadorAux++);
        String regDir = esFloat ? asignarRegistroFloat(aux) : asignarRegistro(aux);
        if (esFloat) ultimoUsoFloat.put(aux, lineaActual); else ultimoUso.put(aux, lineaActual);
        agregarTexto("la " + regDir + ", " + nombre);
        if (esFloat) agregarTexto("s.s " + regFuente + ", 0(" + regDir + ")");
        else if (esByte) agregarTexto("sb " + regFuente + ", 0(" + regDir + ")");
        else agregarTexto("sw " + regFuente + ", 0(" + regDir + ")");
    }

    private void emitirLlamada(String func) {
        int e = 0, f = 0, stackOff = 0;
        for (int i = 0; i < argsPendientes.size(); i++) {
            String reg = argsPendientes.get(i);
            boolean flt = argsFloatPendientes.get(i);
            if (!flt) {
                if (e < 4) { agregarTexto("move $a" + e + ", " + reg); e++; }
                else {
                    if (stackOff == 0) { agregarTexto("addi $sp, $sp, -4"); stackOff = 4; }
                    else { agregarTexto("addi $sp, $sp, -4"); stackOff += 4; }
                    agregarTexto("sw " + reg + ", 0($sp)");
                }
            } else {
                if (f < 2) { agregarTexto("mov.s $f" + (12 + f*2) + ", " + reg); f++; }
                else {
                    if (stackOff == 0) { agregarTexto("addi $sp, $sp, -4"); stackOff = 4; }
                    else { agregarTexto("addi $sp, $sp, -4"); stackOff += 4; }
                    agregarTexto("s.s " + reg + ", 0($sp)");
                }
            }
        }
        if (tablaSimbolos.existeEnHistorial(func))
            tipoRetornoLlamada = tablaSimbolos.buscarEnHistorial(func).getTipo();
        agregarTexto("jal " + func);
        if (stackOff > 0) agregarTexto("addi $sp, $sp, " + stackOff);
        argsPendientes.clear();
        argsFloatPendientes.clear();
    }

    private boolean esEnteroConstante(String token) {
        try { Integer.parseInt(token); return true; } catch (NumberFormatException e) { return false; }
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
        String regBase = asignarRegistro("_arr_base_" + (contadorAux++));
        String regFila = cargarOperando(filaToken);
        String regCol  = cargarOperando(colToken);
        String regColumnas = asignarRegistro("_arr_cols_" + (contadorAux++));
        ultimoUso.put("_arr_base_" + (contadorAux-2), lineaActual);
        ultimoUso.put("_arr_cols_" + (contadorAux-1), lineaActual);
        agregarTexto("la " + regBase + ", " + nombreArray);
        agregarTexto("li " + regColumnas + ", " + columnas);
        agregarTexto("mul " + regFila + ", " + regFila + ", " + regColumnas);
        agregarTexto("add " + regFila + ", " + regFila + ", " + regCol);
        agregarTexto("sll " + regFila + ", " + regFila + ", 2");
        agregarTexto("add " + regFila + ", " + regBase + ", " + regFila);
        liberarRegistroForzado(regBase); liberarRegistroForzado(regColumnas); liberarRegistroForzado(regCol);
        return regFila;
    }
    private void ejecutarArrStore(String nombreArray, String fila, String col, String valor) {
        String dirInfo = calcularDireccionArreglo(nombreArray, fila, col);
        String regValor = cargarOperando(valor);
        
        Simbolo arrSim = tablaSimbolos.buscarEnHistorial(nombreArray);
        boolean esFloat = arrSim != null && arrSim.getTipo().equals("float");
        
        if (esFloat && !regValor.startsWith("$f")) {
            String tempFloat = "_auxf_arrval_" + (contadorAux++);
            String freg = asignarRegistroFloat(tempFloat);
            ultimoUsoFloat.put(tempFloat, lineaActual);
            agregarTexto("mtc1 " + regValor + ", " + freg);
            agregarTexto("cvt.s.w " + freg + ", " + freg);
            regValor = freg;
        }
        
        if (dirInfo.startsWith("CONST:")) {
            String[] partes = dirInfo.split(":");
            String base = partes[1];
            int offset = Integer.parseInt(partes[2]);
            if (esFloat)
                agregarTexto("s.s " + regValor + ", " + offset + "(" + base + ")");
            else
                agregarTexto("sw " + regValor + ", " + offset + "(" + base + ")");
            liberarRegistroForzado(base);
        } else {
            if (esFloat)
                agregarTexto("s.s " + regValor + ", 0(" + dirInfo + ")");
            else
                agregarTexto("sw " + regValor + ", 0(" + dirInfo + ")");
            liberarRegistroForzado(dirInfo);
        }
        
        if (esTemporal(valor)) {
            liberarRegSiUltimoUso(valor);
        } else if (regValor.startsWith("$f")) {
            liberarRegFloatSiUltimoUso(valor);
        }
    }
    private void ejecutarArrLoad(String destino, String nombreArray, String fila, String col) {
        String dirInfo = calcularDireccionArreglo(nombreArray, fila, col);
        Simbolo arrSim = tablaSimbolos.buscarEnHistorial(nombreArray);
        boolean esFloat = arrSim != null && arrSim.getTipo().equals("float");
        
        String regDest;
        if (esFloat) {
            regDest = asignarRegistroFloat(destino);
        } else {
            regDest = asignarRegistro(destino);
        }
        
        if (dirInfo.startsWith("CONST:")) {
            String[] partes = dirInfo.split(":");
            String base = partes[1];
            int offset = Integer.parseInt(partes[2]);
            if (esFloat)
                agregarTexto("l.s " + regDest + ", " + offset + "(" + base + ")");
            else
                agregarTexto("lw " + regDest + ", " + offset + "(" + base + ")");
            liberarRegistroForzado(base);
        } else {
            if (esFloat)
                agregarTexto("l.s " + regDest + ", 0(" + dirInfo + ")");
            else
                agregarTexto("lw " + regDest + ", 0(" + dirInfo + ")");
            liberarRegistroForzado(dirInfo);
        }
    }

    public String generarCodigo() throws IOException {
        List<String> lineas = leerCodigo3D();
        registrosLibres.addAll(Arrays.asList("$t0","$t1","$t2","$t3","$t4","$t5","$t6","$t7","$t8","$t9"));
        for (int i=0; i<10; i++) registrosLibresFloat.add("$f"+i);
        calcularUltimoUso(lineas);

        for (lineaActual = 0; lineaActual < lineas.size(); lineaActual++) {
            String linea = lineas.get(lineaActual).trim();
            if (linea.isEmpty()) continue;
            String[] partes = linea.split(" ");

            if (partes.length == 1 && partes[0].startsWith("data_string_")) {
                String nombre = partes[0].substring("data_string_".length());
                stringVars.putIfAbsent(nombre, "");
                continue;
            }

            if (partes[0].equals("print_string")) {
                asignarStackSiNecesario();
                String argumento = linea.substring(linea.indexOf(' ') + 1);
                imprimirString(argumento);
                liberarAuxiliaresDeLineaActual();
                continue;
            }

            if (partes.length >= 3 && partes[1].equals("=") && tablaSimbolos.existeEnHistorial(partes[0])) {
                Simbolo s = tablaSimbolos.buscarEnHistorial(partes[0]);
                if (s.getTipo().equals("string")) {
                    String dest = partes[0];
                    String literal;
                    if (partes.length == 3 && tablaSimbolos.existeEnHistorial(partes[2])
                            && tablaSimbolos.buscarEnHistorial(partes[2]).getTipo().equals("string")) {
                        literal = stringVars.getOrDefault(partes[2], "");
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 2; i < partes.length; i++) {
                            if (i > 2) sb.append(" ");
                            sb.append(partes[i]);
                        }
                        literal = sb.toString();
                    }
                    stringVars.put(dest, literal);
                    liberarAuxiliaresDeLineaActual();
                    continue;
                }
            }

            if (partes.length == 1 && partes[0].endsWith(":")) {
                agregarTexto(partes[0]);
                if (!partes[0].startsWith("L")) {
                    nombreFuncionActual = partes[0].replace(":", "");
                    stackAllocated = false;
                    preScanFuncion(lineas, lineaActual);
                    funcionActualTieneRetorno = funcionContieneRetorno(lineas, lineaActual);
                }
                continue;
            }

            if (preScanHecho && partes.length == 1 && (partes[0].startsWith("data_") || partes[0].startsWith("param_")))
                continue;

            if (!stackAllocated && !partes[0].equals("prepareStack") && !partes[0].startsWith("data_")
                    && !partes[0].startsWith("param_") && !partes[0].endsWith(":"))
                asignarStackSiNecesario();

            if (partes.length == 2) {
                if (partes[0].equals("prepareStack")) { }
                else if (partes[0].equals("clearStack")) {
                    if (!funcionActualTieneRetorno || nombreFuncionActual.equals("main")) {
                        int total = frameActual.tamaño();
                        if (total > 0 && !nombreFuncionActual.equals("main")) agregarTexto("lw $ra, 0($sp)");
                        if (total > 0) agregarTexto("addi $sp, $sp, " + total);
                        if (nombreFuncionActual.equals("main")) { agregarTexto("li $v0, 10"); agregarTexto("syscall"); }
                    }
                } else if (partes[0].equals("call")) {
                    emitirLlamada(partes[1]);
                } else if (partes[0].equals("push")) {
                    String regVal = cargarOperando(partes[1]);
                    boolean esFloat = regVal.startsWith("$f");
                    argsPendientes.add(regVal); argsFloatPendientes.add(esFloat);
                } else if (partes[0].equals("goto")) {
                    agregarTexto("j " + partes[1]);
                } else if (partes[0].equals("print")) {
                    String op = partes[1];
                    if (tablaSimbolos.existeEnHistorial(op)) {
                        Simbolo s = tablaSimbolos.buscarEnHistorial(op);
                        switch (s.getTipo()) {
                            case "string": imprimirString(op); break;
                            case "float":
                                String regFloat = cargarOperando(op);
                                agregarTexto("mov.s $f12, " + regFloat);
                                agregarTexto("li $v0, 2"); agregarTexto("syscall"); break;
                            case "char":
                                String regChar = cargarOperando(op);
                                agregarTexto("move $a0, " + regChar);
                                agregarTexto("li $v0, 11"); agregarTexto("syscall"); break;
                            default:
                                String regInt = cargarOperando(op);
                                agregarTexto("move $a0, " + regInt);
                                agregarTexto("li $v0, 1"); agregarTexto("syscall");
                        }
                    } else {
                        if (esLiteralCaracter(op)) {
                            String reg = cargarOperando(op);
                            agregarTexto("move $a0, " + reg);
                            agregarTexto("li $v0, 11"); agregarTexto("syscall");
                        } else {
                            String reg = cargarOperando(op);
                            if (reg.startsWith("$f")) {
                                agregarTexto("mov.s $f12, " + reg);
                                agregarTexto("li $v0, 2"); agregarTexto("syscall");
                            } else {
                                agregarTexto("move $a0, " + reg);
                                agregarTexto("li $v0, 1"); agregarTexto("syscall");
                            }
                        }
                    }
                } else if (partes[0].equals("read")) {
                    // Soporte para lectura (cin → read)
                    String varName = partes[1];
                    Simbolo s = tablaSimbolos.buscarEnHistorial(varName);
                    if (s == null) {
                        throw new RuntimeException("Variable no declarada en read: " + varName);
                    }
                    String tipo = s.getTipo();
                    if (!tipo.equals("int") && !tipo.equals("float")) {
                        throw new RuntimeException("read solo soporta int y float, no " + tipo);
                    }

                    asignarStackSiNecesario();

                    if (tipo.equals("int")) {
                        agregarTexto("li $v0, 5");
                        agregarTexto("syscall");
                        almacenarEnVariable(varName, "$v0");
                    } else { // float
                        agregarTexto("li $v0, 6");
                        agregarTexto("syscall");
                        almacenarEnVariable(varName, "$f0");
                    }
                }
            }
            else if (partes.length == 3) {
                if (partes[1].equals("=")) {
                    String destino = partes[0], fuente = partes[2];
                    if (fuente.equals("retval")) {
                        if (tipoRetornoLlamada.equals("float")) {
                            String regDest = asignarRegistroFloat(destino); agregarTexto("mov.s " + regDest + ", $f0");
                        } else { String regDest = asignarRegistro(destino); agregarTexto("move " + regDest + ", $v0"); }
                    } else if (esTemporal(destino)) {
                        String regFuente = cargarOperando(fuente);
                        boolean esFloat = regFuente.startsWith("$f");
                        if (esFloat) {
                            String regDest = asignarRegistroFloat(destino); agregarTexto("mov.s " + regDest + ", " + regFuente);
                        } else { String regDest = asignarRegistro(destino); agregarTexto("move " + regDest + ", " + regFuente); }
                        if (esTemporal(fuente)) { if (esFloat) liberarRegFloatSiUltimoUso(fuente); else liberarRegSiUltimoUso(fuente); }
                        else liberarAuxiliarDespuesDeUso(regFuente);
                    } else if (frameActual != null && frameActual.tiene(destino)) {
                        String regFuente = cargarOperando(fuente);
                        almacenarEnVariable(destino, regFuente);
                        if (esTemporal(fuente)) { if (regFuente.startsWith("$f")) liberarRegFloatSiUltimoUso(fuente); else liberarRegSiUltimoUso(fuente); }
                        else liberarAuxiliarDespuesDeUso(regFuente);
                    } else if (tablaSimbolos.existeEnHistorial(destino)) {
                        Simbolo sd = tablaSimbolos.buscarEnHistorial(destino);
                        if (!sd.getTipo().equals("string")) {
                            String regFuente = cargarOperando(fuente);
                            almacenarEnVariable(destino, regFuente);
                        }
                    } else if (destino.equals("return")) {
                        String regVal = cargarOperando(fuente);
                        if (regVal.startsWith("$f")) agregarTexto("mov.s $f0, " + regVal); else agregarTexto("move $v0, " + regVal);
                        emitirEpilogo();
                    }
                } else if (partes[0].equals("return")) {
                    String regVal = cargarOperando(partes[1]);
                    if (regVal.startsWith("$f")) agregarTexto("mov.s $f0, " + regVal); else agregarTexto("move $v0, " + regVal);
                    emitirEpilogo();
                } else if (partes[0].endsWith(":") && partes[1].equals(".space")) {
                    if (datos.indexOf(partes[0]) == -1) { datos.append(".align 2\n"); agregarDato(linea); }
                }
            }
            else if (partes.length == 4) {
                if (partes[0].equals("if") && partes[2].equals("goto")) {
                    String condicion = partes[1], etiqueta = partes[3];
                    String operador = condicion.substring(0, condicion.indexOf("<"));
                    String interior = condicion.substring(condicion.indexOf("<|")+2, condicion.indexOf("|>"));
                    String[] operandos = interior.split(",");
                    String reg1 = cargarOperando(operandos[0]), reg2 = cargarOperando(operandos[1]);
                    boolean floatOp = reg1.startsWith("$f") || reg2.startsWith("$f");
                    if (floatOp) {
                        reg1 = convertirAFloat(reg1);
                        reg2 = convertirAFloat(reg2);
                        if (operador.equals("n_equal")) { agregarTexto("c.eq.s " + reg1 + ", " + reg2); agregarTexto("bc1f " + etiqueta); }
                        else {
                            String condMips;
                            switch (operador) {
                                case "equal": condMips = "c.eq.s"; break;
                                case "less_t": condMips = "c.lt.s"; break;
                                case "less_te": condMips = "c.le.s"; break;
                                case "greather_t": condMips = "c.lt.s"; String tmp=reg1; reg1=reg2; reg2=tmp; break;
                                case "greather_te": condMips = "c.le.s"; String tmp2=reg1; reg1=reg2; reg2=tmp2; break;
                                default: condMips = "c.eq.s";
                            }
                            agregarTexto(condMips + " " + reg1 + ", " + reg2); agregarTexto("bc1t " + etiqueta);
                        }
                    } else {
                        switch (operador) {
                            case "equal": agregarTexto("beq " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "n_equal": agregarTexto("bne " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "less_t": agregarTexto("blt " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "less_te": agregarTexto("ble " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "greather_t": agregarTexto("bgt " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "greather_te": agregarTexto("bge " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                        }
                    }
                    liberarAuxiliarDespuesDeUso(reg1); liberarAuxiliarDespuesDeUso(reg2);
                }
            }
            else if (partes.length == 5) {
                if (partes[1].equals("=") && esOperadorAritmetico(partes[3])) {
                    String dest = partes[0], op1 = partes[2], oper = partes[3], op2 = partes[4];
                    String regOp1 = cargarOperando(op1), regOp2 = cargarOperando(op2);
                    boolean floatOp = regOp1.startsWith("$f") || regOp2.startsWith("$f");
                    
                    if (floatOp) {
                        regOp1 = convertirAFloat(regOp1);
                        regOp2 = convertirAFloat(regOp2);
                    }
                    
                    if (oper.equals("%")) {
                        if (floatOp) {
                            String regDest = asignarRegistroFloat(dest);
                            String temp1 = "_auxf_mod1_" + (contadorAux++); String fTemp1 = asignarRegistroFloat(temp1);
                            String temp2 = "_auxf_mod2_" + (contadorAux++); String fTemp2 = asignarRegistroFloat(temp2);
                            agregarTexto("div.s " + fTemp1 + ", " + regOp1 + ", " + regOp2);
                            agregarTexto("trunc.w.s " + fTemp2 + ", " + fTemp1);
                            agregarTexto("cvt.s.w " + fTemp2 + ", " + fTemp2);
                            agregarTexto("mul.s " + regDest + ", " + fTemp2 + ", " + regOp2);
                            agregarTexto("sub.s " + regDest + ", " + regOp1 + ", " + regDest);
                            liberarAuxiliarDespuesDeUso(fTemp1); liberarAuxiliarDespuesDeUso(fTemp2);
                        } else { String regDest = asignarRegistro(dest); agregarTexto("div " + regOp1 + ", " + regOp2); agregarTexto("mfhi " + regDest); }
                    } else if (oper.equals("^")) {
                        if (floatOp) {
                            String regDest = asignarRegistroFloat(dest);
                            agregarTexto("mov.s $f12, " + regOp1); agregarTexto("mov.s $f14, " + regOp2);
                            agregarTexto("jal potenciaFloatMIPS"); agregarTexto("mov.s " + regDest + ", $f0"); potenciaFloatUsada = true;
                        } else {
                            String regDest = asignarRegistro(dest);
                            agregarTexto("move $a0, " + regOp1); agregarTexto("move $a1, " + regOp2);
                            agregarTexto("jal potenciaMIPS"); agregarTexto("move " + regDest + ", $v0"); potenciaUsada = true;
                        }
                    } else if (floatOp) {
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
                    if (esTemporal(op1)) liberarRegSiUltimoUso(op1); else liberarAuxiliarDespuesDeUso(regOp1);
                    if (esTemporal(op2)) liberarRegSiUltimoUso(op2); else liberarAuxiliarDespuesDeUso(regOp2);
                } else if (partes[0].equals("arr_store")) {
                    ejecutarArrStore(partes[1].replace(",",""), partes[2].replace(",",""), partes[3].replace(",",""), partes[4]);
                }
            }
            else if (partes.length == 6) {
                if (partes[1].equals("=") && partes[2].equals("arr_load")) {
                    ejecutarArrLoad(partes[0], partes[3].replace(",",""), partes[4].replace(",",""), partes[5]);
                } else if (partes[0].equals("if") && partes[4].equals("goto")) {
                    String op1 = partes[1], relop = partes[2], op2 = partes[3], etiqueta = partes[5];
                    String reg1 = cargarOperando(op1), reg2 = cargarOperando(op2);
                    boolean floatOp = reg1.startsWith("$f") || reg2.startsWith("$f");
                    if (floatOp) {
                        reg1 = convertirAFloat(reg1);
                        reg2 = convertirAFloat(reg2);
                        if (relop.equals("!=")) { agregarTexto("c.eq.s " + reg1 + ", " + reg2); agregarTexto("bc1f " + etiqueta); }
                        else {
                            String condMips;
                            switch (relop) {
                                case "==": condMips = "c.eq.s"; break;
                                case "<": condMips = "c.lt.s"; break;
                                case "<=": condMips = "c.le.s"; break;
                                case ">": condMips = "c.lt.s"; String tmp=reg1; reg1=reg2; reg2=tmp; break;
                                case ">=": condMips = "c.le.s"; String tmp2=reg1; reg1=reg2; reg2=tmp2; break;
                                default: condMips = "c.eq.s";
                            }
                            agregarTexto(condMips + " " + reg1 + ", " + reg2); agregarTexto("bc1t " + etiqueta);
                        }
                    } else {
                        switch (relop) {
                            case "==": agregarTexto("beq " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "!=": agregarTexto("bne " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "<": agregarTexto("blt " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case "<=": agregarTexto("ble " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case ">": agregarTexto("bgt " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                            case ">=": agregarTexto("bge " + reg1 + ", " + reg2 + ", " + etiqueta); break;
                        }
                    }
                    liberarAuxiliarDespuesDeUso(reg1); liberarAuxiliarDespuesDeUso(reg2);
                }
            }
            liberarAuxiliaresDeLineaActual();
        }

        if (potenciaUsada) {
            agregarTexto("potenciaMIPS:"); agregarTexto("li $v0, 1");
            agregarTexto("buclePotenciaEntero:"); agregarTexto("blez $a1, finPotenciaEntero");
            agregarTexto("mul $v0, $v0, $a0"); agregarTexto("addi $a1, $a1, -1");
            agregarTexto("j buclePotenciaEntero"); agregarTexto("finPotenciaEntero:"); agregarTexto("jr $ra");
        }
        if (potenciaFloatUsada) {
            agregarTexto("potenciaFloatMIPS:"); agregarTexto("li.s $f0, 1.0");
            agregarTexto("cvt.w.s $f14, $f14"); agregarTexto("mfc1 $a1, $f14");
            agregarTexto("buclePotenciaFlotante:"); agregarTexto("blez $a1, finPotenciaFlotante");
            agregarTexto("mul.s $f0, $f0, $f12"); agregarTexto("addi $a1, $a1, -1");
            agregarTexto("j buclePotenciaFlotante"); agregarTexto("finPotenciaFlotante:"); agregarTexto("jr $ra");
        }

        tempARegistro.clear(); registrosLibres.clear();
        tempARegistroFloat.clear(); registrosLibresFloat.clear();
        return generarPrograma();
    }

    private boolean funcionContieneRetorno(List<String> lineas, int inicio) {
        for (int i = inicio+1; i < lineas.size(); i++) {
            String l = lineas.get(i).trim();
            if (l.isEmpty()) continue;
            if (l.endsWith(":") && !l.startsWith("L")) break;
            if (l.startsWith("return") || l.matches(".*= return.*")) return true;
        }
        return false;
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
        String etiqueta = "_str_lit_" + (contStringLit++);
        datos.append(".align 2\n");
        agregarDato(etiqueta + ": .asciiz \"" + str + "\"");
        agregarTexto("la $a0, " + etiqueta);
        agregarTexto("li $v0, 4");
        agregarTexto("syscall");
    }



    private boolean esOperadorAritmetico(String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/")
            || token.equals("%") || token.equals("^");
    }

    private boolean esLiteralCaracter(String op) {
        return (op.startsWith("'") && op.endsWith("'") && op.length() == 3) ||
               (op.length() == 1 && !Character.isDigit(op.charAt(0)) && !op.equals("."));
    }

    public String generarPrograma() {
        for (Map.Entry<String, String> entry : stringVars.entrySet()) {
            String nombre = entry.getKey();
            String literal = entry.getValue();
            if (datos.indexOf(nombre + ":") == -1) {
                datos.append(".align 2\n");
                agregarDato(nombre + ": .asciiz \"" + literal + "\"");
            }
        }

        StringBuilder resultado = new StringBuilder();
        resultado.append(".data\n").append(datos).append("\n.text\n");
        resultado.append(".globl main\n");
        resultado.append(texto);
        resultado.append("li $v0, 10\n").append("syscall\n");
        return resultado.toString();
    }
}