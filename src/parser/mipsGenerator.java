package parser;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import tabla.Simbolo;
import tabla.TablaManagement;

public class mipsGenerator {

    private StringBuilder data = new StringBuilder();
    private StringBuilder text = new StringBuilder();
    private int globalStackPointer = 0;
    private TablaManagement tablaSimbolos;
    private Map<String, Integer> offsets = new HashMap<>();


    public void setTabla(TablaManagement t){
        
        this.tablaSimbolos = t;
    }
    public TablaManagement getTabla(){
        return this.tablaSimbolos;
    }
    public void agregarData(String linea) {
        data.append(linea).append("\n");
    }

    public void agregarText(String linea) {
        text.append(linea).append("\n");
    }

    public void declararVariable(String nombre, int size) {
        offsets.put(nombre, globalStackPointer);
        globalStackPointer += size;
    }
    
    public int getOffset(String nombre) {
        Integer offset = offsets.get(nombre);
        return offset != null ? offset : -1;
    }
    public List<String> leerCodigo3D() throws IOException {
        return Files.readAllLines(Paths.get("cod3D.txt"));
    }
    public String[] partes(String linea){
        return linea.split(" ");
    }

    public void imprimirOffsets() {
        System.out.println("\n=== OFFSETS DEL STACK ===");
        
        for (Map.Entry<String, Integer> entry : offsets.entrySet()) {
            System.out.println(
                "Variable: " + entry.getKey() +
                " | Offset: " + entry.getValue()
            );
        }

        System.out.println("Stack total reservado: " + globalStackPointer + " bytes");
        System.out.println("=========================\n");
    }

    public String generarCodigo() throws IOException {
        try{
            List<String> lineas = leerCodigo3D();
            for(String linea : lineas){
                linea = linea.trim();
                if(linea.isEmpty()){
                    continue;
                }
                // Aca vienen ya todas las instrucciones de 3D, que se traducen a MIPS.
                String[] partes = partes(linea);

                if (partes.length == 1){
                    // Es una etiqueta, la agregamos al segmento .text
                    if (partes[0].endsWith(":")){
                        agregarText(partes[0]);
                    } else if (partes[0].length() >= 4 && partes[0].substring(0,4).equals("data")){
                        // Ejemplo: manejar declaraciones que comienzan con "data"
                        // Aca verdaderamente tendra que ser agregado al stack pointer.
                        if (partes[0].substring(5,9).equals("char")){
                            declararVariable(linea.split("_",3)[2], 1);
                           
                            
                        } else if (partes[0].substring(5,8).equals("int")){
                            declararVariable(linea.split("_",3)[2], 4);
                            

                        } else if (partes[0].substring(5,10).equals("float")){
                            declararVariable(linea.split("_",3)[2], 4);
                            

                        }  else if (partes[0].substring(5,11).equals("string")){
                            

                        }  else if (partes[0].substring(5,9).equals("bool")){
                            declararVariable(linea.split("_",3)[2], 1);
                            
                        }
                            
                        //imprimirOffsets();
                    } else if ((partes[0].length() >= 5 && partes[0].substring(0,5).equals("param"))){

                    }
                } else if (partes.length == 2){ // Instruccion con un solo operando, como prepareStack o clearStack, tambien goto's
                    if (partes[0].equals("prepareStack")){
                        // Instruccion para preparar el stack, se traduce a MIPS.
                        agregarText("addi $sp, $sp, -" + partes[1]);
                    } else if (partes[0].equals("clearStack")){
                        // Instruccion para restaurar el stack, se traduce a MIPS.
                        agregarText("addi $sp, $sp, " + partes[1]);
                    } else if (partes[0].equals("call")){

                    } else if (partes[0].equals("push")){
                        
                    } else if (partes[0].equals("goto")){
                        agregarText("j " + partes[1]);
                    } else if (partes[0].equals("print")){

                    } else if (partes[0].equals("print_string")){
                        
                    }
                } else if (partes.length == 3){ // Instrucciones de asignacion.
                    if (partes[0].contains("t") && !partes[0].equals("return") && partes[1].equals("=") && !(getTabla().existeEnHistorial(partes[0]))){
                            // Instruccion de asignacion, se traduce a MIPS.
                            agregarText("li $" + partes[0] + ", " + partes[2]); // Aca tengo que arreglar ya que no existe t30 en mips, pero bueno, es un ejemplo de como se traduciria una asignacion de constante a MIPS.
                        
                    } else if (true == (getTabla().existeEnHistorial(partes[0]))){
                        
                        Simbolo s = getTabla().buscarEnHistorial(partes[0]);
                        if (s != null){
                            
                            if (s.getTipo().equals("string")){
                                agregarData(partes[0] + ": .asciiz " + "\"" + partes[2] + "\"");
                            } else{
                                agregarText("move $t0, $" + partes[2]);
                                agregarText("sw $t0, " + getOffset(partes[0]) + "($sp)");
                            }
                            
                        }       
                    } else if (partes[0].equals("return")){

                    } else if (partes[0].endsWith(":") && partes[1].equals(".space") && !data.toString().contains(partes[1].replace(",", ""))){
                        agregarData(linea);
                    }
                   
                   
                } else if (partes.length == 4){
                    
                } else if (partes.length == 5){
                    // Por ejemplo load y stores de arreglos, operaciones aritmeticas
                    if (partes[3].equals("+")){
                        if (getTabla().existeEnHistorial(partes[2])){ //Debo cambiar esto y debo de buscar en la tabla de simbolos si esto es una variable
                            agregarText("lw $t0, " + getOffset(partes[2]) + "($sp)");
                            agregarText("add $" + partes[0] + ", $" +  "t0" + ", $" + partes[4]);
                        }
                        else if (getTabla().existeEnHistorial(partes[4])){ //Debo cambiar esto y debo de buscar en la tabla de simbolos si esto es una variable
                            agregarText("lw $t0, " + getOffset(partes[4]) + "($sp)");
                            agregarText("add $" + partes[0] + ", $" + partes[2] + ", $" + "t0");
                        } else {
                            agregarText("add $" + partes[0] + ", $" + partes[2] + ", $" + partes[4]);
                        }

                    } else if (partes[3].equals("-")){
                        if (getTabla().existeEnHistorial(partes[2])){ //Debo cambiar esto y debo de buscar en la tabla de simbolos si esto es una variable
                            agregarText("lw $t0, " + getOffset(partes[2]) + "($sp)");
                        }
                        if (getTabla().existeEnHistorial(partes[4])){ //Debo cambiar esto y debo de buscar en la tabla de simbolos si esto es una variable
                            agregarText("lw $t0, " + getOffset(partes[4]) + "($sp)");
                        }
                        agregarText("sub $" + partes[0] + ", $" + partes[2] + ", $" + partes[4]);

                    }  else if (partes[3].equals("*")){
                        if (getTabla().existeEnHistorial(partes[2])){ //Debo cambiar esto y debo de buscar en la tabla de simbolos si esto es una variable
                            agregarText("lw $t0, " + getOffset(partes[2]) + "($sp)");
                        }
                        if (getTabla().existeEnHistorial(partes[4])){ //Debo cambiar esto y debo de buscar en la tabla de simbolos si esto es una variable
                            agregarText("lw $t0, " + getOffset(partes[4]) + "($sp)");
                        }
                        agregarText("mul $" + partes[0] + ", $" + partes[2] + ", $" + partes[4]);

                    } else if (partes[3].equals("/")){
                        if (getTabla().existeEnHistorial(partes[2])){ //Debo cambiar esto y debo de buscar en la tabla de simbolos si esto es una variable
                            agregarText("lw $t0, " + getOffset(partes[2]) + "($sp)");
                        }
                        if (getTabla().existeEnHistorial(partes[4])){ //Debo cambiar esto y debo de buscar en la tabla de simbolos si esto es una variable
                            agregarText("lw $t0, " + getOffset(partes[4]) + "($sp)");
                        }
                        agregarText("div $" + partes[0] + ", $" + partes[2] + ", $" + partes[4]);

                    } else if (partes[0].equals("arrstore")){
                        int fila = Integer.parseInt(partes[2].replace(",", ""));
                        int columna = Integer.parseInt(partes[3].replace(",", ""));// Columna a la que se quiere accesar
                        String nombreArray = partes[1];
                        int columnas = getTabla().buscarEnHistorial(nombreArray).getColumnas(); // Cantidad de columnas que tiene ya definidas el array.
                        int posicion = (fila * columnas + columna) * 4;
                        agregarText("la $t0, " + nombreArray);
                        agregarText("lw $t0, " + posicion + "(" + "t0" + ")"); // Valor que dice posicion de fila   
                        
                    }

                } else if (partes.length == 6){
                    if (partes[1].equals("arrload")){
                        int fila = 0;//Integer.parseInt(partes[4].replace(",", ""));
                        int columna = 0;//Integer.parseInt(partes[5].replace(",", ""));// Columna a la que se quiere accesar
                        String nombreArray = partes[3].replace(",", "");
                        int columnas = getTabla().buscarEnHistorial(nombreArray).getColumnas(); // Cantidad de columnas que tiene ya definidas el array.
                        int posicion = (fila * columnas + columna) * 4;
                        agregarText("la $t0, " + nombreArray);
                        agregarText("sw $t0, " + posicion + "(" + "t0" + ")"); // Valor que dice posicion de fila       
                    
                    // if t83 != t84 goto L42
                    } else if(partes[0].equals("if")){
                        if (partes[2].equals("!=")){

                        }else if (partes[2].equals("==")){

                        
                        }else if (partes[2].equals("<")){

                        
                        }else if (partes[2].equals(">")){

                        
                        }else if (partes[2].equals(">=")){

                        
                        }else if (partes[2].equals("<=")){

                        
                        }
                    }
                    // Condicionales con goto por ejemplo if t3 != t4 goto L0
                }

            }
        }catch(IOException e){
            e.printStackTrace();
        }
        return text.toString();
    }


    public String generarPrograma() {
        StringBuilder resultado = new StringBuilder();

        resultado.append(".data\n");
        resultado.append(data);

        resultado.append("\n.text\n");
        resultado.append(".globl main\n");
        resultado.append(text);

        return resultado.toString();
    }
}