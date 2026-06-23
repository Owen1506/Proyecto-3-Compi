package scanner;

import parser.*;
import java_cup.runtime.Symbol;
import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

%%

// Configuracion general del lexer generado por JFlex.
// Se define el nombre de la clase, visibilidad publica, soporte Unicode,
// compatibilidad con CUP y seguimiento de linea y columna.
%class Lexer
%public
%unicode
%cup
%line
%column

%{

// TokenInfo es una estructura interna para guardar la informacion
// de cada token que se va reconociendo durante el analisis lexico.
// Almacena el tipo de token, el texto original, y su posicion en el archivo.
private static class TokenInfo {
    String token;
    String lexema;
    int linea;
    int columna;
    TokenInfo(String token, String lexema, int linea, int columna) {
        this.token = token;
        this.lexema = lexema;
        this.linea = linea;
        this.columna = columna;
    }
}

// Lista donde se van acumulando todos los tokens reconocidos durante el analisis.
private List<TokenInfo> tokenLog = new ArrayList<>();

// Permite que otras clases consulten la lista de tokens si lo necesitan.
public List<TokenInfo> getTokenLog() {
    return tokenLog;
}

// Registra un token que no tiene lexema relevante (como operadores o palabras reservadas).
// Entrada: nombre del token.
private void addToken(String token) {
    tokenLog.add(new TokenInfo(token, "-", yyline + 1, yycolumn));
}

// Registra un token junto con su lexema (como identificadores o literales).
// Entrada: nombre del token y el texto reconocido.
private void addToken(String token, String lexema) {
    tokenLog.add(new TokenInfo(token, lexema, yyline + 1, yycolumn));
}

// Escribe todos los tokens reconocidos en un archivo de texto con formato de tabla.
// Para los identificadores y MAIN, consulta la tabla de simbolos para incluir el scope.
// Entrada : ruta del archivo de salida y el manejador de la tabla de simbolos.
// Salida  : archivo tokens.txt con la lista completa de tokens.
public void exportarTokens(String ruta, tabla.TablaManagement tablaManager) {
    try (PrintWriter writer = new PrintWriter(new FileWriter(ruta))) {
        writer.println("           LISTA DE TOKENS            ");
        writer.printf("%-12s %-15s %-8s %-8s %-15s%n",
                "TOKEN", "LEXEMA", "LINEA", "COL", "SCOPE/INFO");
        for (TokenInfo t : tokenLog) {
            // Por defecto no hay informacion de scope disponible.
            String extraInfo = "N/A";

            // Si el token es un identificador o MAIN, se busca en la tabla de simbolos
            // para saber en que scope fue declarado.
            if (t.token.equals("ID") || t.token.equals("MAIN")) {
                if (tablaManager != null) {
                    tabla.Simbolo sim = tablaManager.buscarEnHistorial(t.lexema);
                    if (sim != null) {
                        extraInfo = "Scope " + sim.getScope();
                    } else {
                        // El identificador aparece pero aun no fue declarado formalmente.
                        extraInfo = "Scope (Pendiente)";
                    }
                }
            }
            writer.printf("%-12s %-15s %-8d %-8d %-15s%n",
                    t.token,
                    t.lexema,
                    t.linea,
                    t.columna,
                    extraInfo);
        }
        System.out.println("Tokens exportados correctamente a: " + ruta);
    } catch (IOException e) {
        System.err.println("Error al exportar tokens: " + e.getMessage());
    }
}

// Recorre la lista de tokens buscando los que fueron marcados como errores lexicos
// y los escribe en un archivo separado para facilitar su revision.
// Entrada : ruta del archivo de salida.
// Salida  : archivo con los errores lexicos encontrados, o un mensaje indicando que no hubo ninguno.
public void exportarErroresLexicos(String ruta) {
    try (PrintWriter writer = new PrintWriter(new FileWriter(ruta))) {
        boolean hayErrores = false;
        for (TokenInfo t : tokenLog) {
            if (t.token.equals("LEX_ERROR")) {
                writer.printf("ERROR LEXICO -> Linea %d, Columna %d, Caracter: '%s'%n", 
                        t.linea, t.columna, t.lexema);
                hayErrores = true;
            }
        }
        if (!hayErrores) {
            writer.println("No se detectaron errores lexicos.");
        }
        System.out.println("Errores lexicos exportados a: " + ruta);
    } catch (IOException e) {
        System.err.println("Error al exportar errores lexicos: " + e.getMessage());
    }
}

// Retorna el numero de linea actual donde esta leyendo el lexer.
// Se suma 1 porque JFlex comienza a contar desde 0.
public int getCurrentLine() {
    return yyline + 1;
}

// Retorna el numero de columna actual donde esta leyendo el lexer.
// Se suma 1 por la misma razon que getCurrentLine.
public int getCurrentColumn() {
    return yycolumn + 1;
}

%}

// 
// ===== MACROS =====
// Definiciones de patrones reutilizables para construir las reglas del lexer.

// Un digito del 0 al 9.
Digito = [0-9]

// Una letra (mayuscula, minuscula o guion bajo), base de los identificadores.
Letra = [a-zA-Z_]

// Un identificador: empieza con letra y puede continuar con letras o digitos.
Id = {Letra}({Letra}|{Digito})*

// Entero distinto de cero (para evitar ceros a la izquierda en literales).
EnteroNoCero = [1-9]

// Numero entero: puede ser 0 o un numero que no empieza con 0.
IntN = 0|-?{EnteroNoCero}{Digito}*

// Numero con punto decimal.
FloatN = {IntN}"."{Digito}+

// Numero entero con notacion exponencial (ej: 3e5).
ExpN = {IntN}[eE]{EnteroNoCero}{Digito}*

// Numero fraccionario en formato numerador/denominador (ej: 3/4).
FraccionarioN = -?{EnteroNoCero}{Digito}*"//"{EnteroNoCero}{Digito}*

// Cadena de texto entre comillas dobles, con soporte para secuencias de escape.
StringLiteral = \"([^\"\\\r\n]|\\.)*\"

// Caracter entre comillas simples, con soporte para secuencias de escape.
CharLiteral = \'([^\'\\\r\n]|\\.)\'

// Espacios, tabulaciones y saltos de linea que se ignoran durante el analisis.
WhiteSpace = [ \t\f\r\n]+

// Comentario de linea: comienza con !! y va hasta el final de la linea.
LineComment = "¡¡"[^\r\n]*

// Comentario de bloque: abre con {- y cierra con -}.
BlockComment = "{-"([^\-]|-+[^}])*"-}"

%%

// Si el archivo empieza con un BOM (marca de orden de bytes UTF-8), se ignora.
\uFEFF               { /* ignorar BOM */ }

// Tipos de datos primitivos del lenguaje.
"int"       { addToken("INT"); return new Symbol(sym.INT, yyline+1, yycolumn, yytext()); }
"float"     { addToken("FLOAT"); return new Symbol(sym.FLOAT, yyline+1, yycolumn, yytext()); }
"char"      { addToken("CHAR"); return new Symbol(sym.CHAR, yyline+1, yycolumn, yytext()); }
"string"    { addToken("STRING"); return new Symbol(sym.STRING, yyline+1, yycolumn, yytext()); }
"bool"      { addToken("BOOL"); return new Symbol(sym.BOOL, yyline+1, yycolumn, yytext()); }
"boolean"   { addToken("BOOL"); return new Symbol(sym.BOOL, yyline+1, yycolumn, yytext()); }

// Tipos especiales del lenguaje.
"empty"     { addToken("EMPTY"); return new Symbol(sym.EMPTY, yyline+1, yycolumn, yytext()); }
"expint"    { addToken("EXPINT_KW"); return new Symbol(sym.EXPINT_KW, yyline+1, yycolumn, yytext()); }
"frac"      { addToken("FRAC_KW"); return new Symbol(sym.FRAC_KW, yyline+1, yycolumn, yytext()); }

// Palabra reservada que marca el bloque principal del programa.
"__main__"  { addToken("MAIN", "__main__"); return new Symbol(sym.MAIN, yyline+1, yycolumn, yytext()); }

// Estructuras de control de flujo.
"if"        { addToken("IF"); return new Symbol(sym.IF, yyline+1, yycolumn, yytext()); }
"else"      { addToken("ELSE"); return new Symbol(sym.ELSE, yyline+1, yycolumn, yytext()); }
"break"     { addToken("BREAK"); return new Symbol(sym.BREAK, yyline+1, yycolumn, yytext()); }
"return"    { addToken("RETURN"); return new Symbol(sym.RETURN, yyline+1, yycolumn, yytext()); }
"case"      { addToken("CASE"); return new Symbol(sym.CASE, yyline+1, yycolumn, yytext()); }
"default"   { addToken("DEFAULT"); return new Symbol(sym.DEFAULT, yyline+1, yycolumn, yytext()); }
"switch"    { addToken("SWITCH"); return new Symbol(sym.SWITCH, yyline+1, yycolumn, yytext()); }
"while"     { addToken("WHILE"); return new Symbol(sym.WHILE, yyline+1, yycolumn, yytext()); }
"do"        { addToken("DO"); return new Symbol(sym.DO, yyline+1, yycolumn, yytext()); }

// Instrucciones de entrada y salida.
"cin"       { addToken("CIN"); return new Symbol(sym.CIN, yyline+1, yycolumn, yytext()); }
"cout"      { addToken("COUT"); return new Symbol(sym.COUT, yyline+1, yycolumn, yytext()); }

// Operadores de comparacion escritos como palabras en este lenguaje.
"equal"       { addToken("EQUAL"); return new Symbol(sym.EQUAL, yyline+1, yycolumn, yytext()); }
"n_equal"     { addToken("N_EQUAL"); return new Symbol(sym.N_EQUAL, yyline+1, yycolumn, yytext()); }
"less_t"      { addToken("LESS_T"); return new Symbol(sym.LESS_T, yyline+1, yycolumn, yytext()); }
"less_te"     { addToken("LESS_TE"); return new Symbol(sym.LESS_TE, yyline+1, yycolumn, yytext()); }
"greather_t"  { addToken("GREATER_T"); return new Symbol(sym.GREATER_T, yyline+1, yycolumn, yytext()); }
"greather_te" { addToken("GREATER_TE"); return new Symbol(sym.GREATER_TE, yyline+1, yycolumn, yytext()); }

// Literales booleanos del lenguaje.
"True"  { addToken("TRUE"); return new Symbol(sym.TRUE, yyline+1, yycolumn, yytext()); }
"False" { addToken("FALSE"); return new Symbol(sym.FALSE, yyline+1, yycolumn, yytext()); }

// Delimitadores de agrupacion: parentesis y llaves con sintaxis propia del lenguaje.
"<|" { addToken("PAR_I"); return new Symbol(sym.PAR_I, yyline+1, yycolumn, yytext()); }
"|>" { addToken("PAR_D"); return new Symbol(sym.PAR_D, yyline+1, yycolumn, yytext()); }
"|:" { addToken("LLAVE_I"); return new Symbol(sym.LLAVE_I, yyline+1, yycolumn, yytext()); }
":|" { addToken("LLAVE_D"); return new Symbol(sym.LLAVE_D, yyline+1, yycolumn, yytext()); }

// Operadores de flujo para cin y cout.
"<<" { addToken("ARR_I"); return new Symbol(sym.ARR_I, yyline+1, yycolumn, yytext()); }
">>" { addToken("ARR_D"); return new Symbol(sym.ARR_D, yyline+1, yycolumn, yytext()); }

// Operador de asignacion del lenguaje.
"<-" { addToken("ASSIGN"); return new Symbol(sym.ASSIGN, yyline+1, yycolumn, yytext()); }

// Separador de dimensiones en arreglos.
"~"  { addToken("SEP"); return new Symbol(sym.SEP, yyline+1, yycolumn, yytext()); }

// Fin de sentencia (equivale al ; en otros lenguajes).
"!"  { addToken("PYC"); return new Symbol(sym.PYC, yyline+1, yycolumn, yytext()); }

// Dos puntos, usado en switch/case y otros contextos.
":"  { addToken("DOSPUNTOS"); return new Symbol(sym.DOSPUNTOS, yyline+1, yycolumn, yytext()); }

// Separador de argumentos o elementos.
","  { addToken("COMMA"); return new Symbol(sym.COMMA, yyline+1, yycolumn, yytext()); }

// Operadores de incremento y decremento.
"++" { addToken("INC"); return new Symbol(sym.INC, yyline+1, yycolumn, yytext()); }
"--" { addToken("DEC"); return new Symbol(sym.DEC, yyline+1, yycolumn, yytext()); }

// Operadores aritmeticos basicos.
"+"  { addToken("PLUS"); return new Symbol(sym.PLUS, yyline+1, yycolumn, yytext()); }
"-"  { addToken("MINUS"); return new Symbol(sym.MINUS, yyline+1, yycolumn, yytext()); }
"*"  { addToken("MULT"); return new Symbol(sym.MULT, yyline+1, yycolumn, yytext()); }
"/"  { addToken("DIV"); return new Symbol(sym.DIV, yyline+1, yycolumn, yytext()); }
"%"  { addToken("MOD"); return new Symbol(sym.MOD, yyline+1, yycolumn, yytext()); }
"^"  { addToken("POW"); return new Symbol(sym.POW, yyline+1, yycolumn, yytext()); }

// Operadores logicos con simbolos propios del lenguaje.
"@"  { addToken("AND"); return new Symbol(sym.AND, yyline+1, yycolumn, yytext()); }
"#"  { addToken("OR"); return new Symbol(sym.OR, yyline+1, yycolumn, yytext()); }
"$"  { addToken("NOT"); return new Symbol(sym.NOT, yyline+1, yycolumn, yytext()); }

// Literales numericos en sus distintos formatos.
// El orden importa: se evaluan de mas especifico a mas general para evitar conflictos.
{FraccionarioN} { addToken("FRAC_LIT", yytext()); return new Symbol(sym.FRAC_LIT, yyline+1, yycolumn, yytext()); }
{ExpN}          { addToken("EXPINT_LIT", yytext()); return new Symbol(sym.EXPINT_LIT, yyline+1, yycolumn, yytext()); }
{FloatN}        { addToken("FLOAT_LIT", yytext()); return new Symbol(sym.FLOAT_LIT, yyline+1, yycolumn, yytext()); }
{IntN}          { addToken("INT_LIT", yytext()); return new Symbol(sym.INT_LIT, yyline+1, yycolumn, yytext()); }

// Literales de texto: se eliminan las comillas antes de guardar el contenido real.
{StringLiteral} { 
    String sinComillas = yytext().substring(1, yytext().length()-1);
    addToken("STRING_LIT", sinComillas);
    return new Symbol(sym.STRING_LIT, yyline+1, yycolumn, sinComillas);
}
{CharLiteral}   {
    String sinComillas = yytext().substring(1, yytext().length()-1);
    addToken("CHAR_LIT", sinComillas);
    return new Symbol(sym.CHAR_LIT, yyline+1, yycolumn, sinComillas);
}

// Identificadores: cualquier nombre que no haya sido reconocido como palabra reservada antes.
{Id}            { addToken("ID", yytext()); return new Symbol(sym.ID, yyline+1, yycolumn, yytext()); }

// Espacios y comentarios se ignoran por completo y no generan tokens.
{WhiteSpace}   { /* ignorar */ }
{LineComment}  { /* ignorar */ }
{BlockComment} { /* ignorar */ }

// Cualquier caracter que no encaje en ninguna regla anterior se considera un error lexico.
// Se registra, se reporta en consola y se retorna un token de error para que el parser lo maneje.
. {
    addToken("LEX_ERROR", yytext());
    System.err.println("ERROR LEXICO -> Linea " + (yyline+1) +
        ", Columna " + yycolumn +
        ", Caracter: '" + yytext() + "'");
    return new Symbol(sym.LEX_ERROR, yyline+1, yycolumn, yytext());
}

// Cuando se llega al final del archivo se retorna el token EOF para indicarle al parser que termino.
<<EOF>> { return new Symbol(sym.EOF); }