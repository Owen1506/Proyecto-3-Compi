package main;

import scanner.Lexer;
import parser.mipsGenerator;
import parser.parser;
import parser.parser.SemanticErrors;

import java.io.*;

 // Clase principal del compilador.
 // Es el punto de entrada del programa. Su trabajo es coordinar todo el proceso
 // de analisis: recibe el archivo fuente, lo pasa por el analizador lexico y
 // sintactico, y al final guarda los resultados en archivos de texto para que
 // el usuario pueda revisarlos.
public class Main {
     // Metodo principal.
     // Entrada  : args[0] - ruta al archivo fuente que se quiere analizar.
     // Salidas  : tokens.txt, lexical_errors.txt, syntax_errors.txt,
     //            tabla_simbolos.txt  (se generan siempre, haya errores o no).
     // Restricciones:
     //   - Si no se pasa el archivo como argumento el programa termina de inmediato.
     //   - Si el archivo no existe tambien se detiene antes de intentar leerlo.
     //   - El archivo fuente se cierra siempre al terminar, sin importar que ocurra.
    public static void main(String[] args) {
        // Si el usuario no indico un archivo, se le muestra como usar el programa y se sale.
        if (args.length < 1) {
            System.err.println("Uso: java main.Main <archivo_fuente>");
            System.exit(1);
        }

        String sourceFile = args[0];

        try {
            // Antes de intentar abrir el archivo, se verifica que realmente exista.
            File inputFile = new File(sourceFile);
            if (!inputFile.exists()) {
                System.err.println("Error: No se puede encontrar el archivo: " + sourceFile);
                System.exit(1);
            }

             // Se abre el archivo y se preparan el lexer y el parser.
             // Importante: solo se crea una instancia de cada uno;
             // crear mas de una causaria problemas en el analisis.

            FileReader fileReader = new FileReader(inputFile);
            Lexer lexer = new Lexer(fileReader);
            parser p = new parser(lexer); // SOLO UNO

         //   System.out.println("Iniciando analisis...");
            System.out.println("Archivo de entrada: " + sourceFile);

            try {
                 // Se lanza el analisis. Si algo sale mal durante el proceso
                 // se muestra el error, pero el programa sigue adelante para
                 // poder exportar lo que se haya logrado analizar hasta ese punto.
                try {
                    p.parse(); // SOLO UNA VEZ
                } catch (Exception e) {
                    System.err.println("\n Error durante el analisis: " + e.getMessage());
                }
                 // Se guardan los tokens reconocidos y los errores lexicos.
                 // Esto se hace siempre, incluso si hubo errores, para que
                 // el usuario pueda ver hasta donde llego el analisis.
                lexer.exportarTokens("tokens.txt", p.getTabla());
                lexer.exportarErroresLexicos("lexical_errors.txt");
                 // Se guardan los errores sintacticos que el parser fue
                 // acumulando durante el analisis.
                 // Si por alguna razon no se puede escribir el archivo,
                 // se avisa en consola pero el programa no se detiene.
                try (java.io.PrintWriter se = new java.io.PrintWriter(new java.io.FileWriter("syntax_errors.txt"))) {
                    for (String sErr : parser.getSyntaxErrors()) {
                        se.println(sErr);
                    }
                } catch (Exception e) {
                    System.err.println("No se pudo exportar syntax_errors.txt: " + e.getMessage());
                }

                p.getTabla().exportarTXT("tabla_simbolos.txt");    
                String semantico = SemanticErrors.getSemanticErrors();

                try (PrintWriter out = new PrintWriter(new FileWriter("semantic_errors.txt"))) {
                    out.print(semantico);
                }

                System.out.println("Errores semanticos guardados en semantic_errors.txt");

                // Si no hay errores semánticos se genera/exporta el código 3D
                boolean hayErroresSemanticos =
                        semantico != null && !semantico.trim().isEmpty();

                try (PrintWriter out = new PrintWriter(new FileWriter("cod3D.txt"))) {

                    if (!hayErroresSemanticos) {
                        String codigo = parser.generador.getCodigo();
                        out.print(codigo);
                        System.out.println("Codigo intermedio guardado en cod3D.txt");

                        
                    } else {
                        out.println("No se genero codigo 3D debido a errores semanticos en el codigo fuente.");
                        System.out.println("No se genero codigo 3D debido a errores semanticos.");
                    }
                }
                mipsGenerator gen = new mipsGenerator(); 
                gen.setTabla(p.getTabla());
                gen.generarCodigo();
                String programaMips = gen.generarPrograma();
                System.out.println(programaMips);
                
                //gen.getTabla().imprimirHistorial();
               

            } finally {
                // El archivo fuente se cierra aqui pase lo que pase, para no dejar recursos abiertos.
                try { fileReader.close(); } catch (Exception ex) {}
            }

        } catch (Exception e) {
            // Cualquier error inesperado que no se haya manejado antes llega aqui.
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
        //-----------//


    }

}