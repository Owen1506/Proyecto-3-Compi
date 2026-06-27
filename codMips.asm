.data
.align 2
_str_lit_0: .asciiz "=== Entrando a func1 ===\n"
.align 2
sss: .asciiz "Hola"
.align 2
apa: .asciiz """"
.align 2
apa: .asciiz "sss"
.align 2
_str_lit_6: .asciiz "numero inicial ="
.align 2
_str_lit_8: .asciiz "\n"
.align 2
_str_lit_16: .asciiz "g ="
.align 2
_str_lit_18: .asciiz "\n"
.align 2
_str_lit_24: .asciiz "h ="
.align 2
_str_lit_26: .asciiz "\n"
.align 2
_x50_: .space 8
.align 2
_x502_: .space 16
.align 2
_str_lit_50: .asciiz "Valor matriz int ="
.align 2
_str_lit_55: .asciiz "\n"
.align 2
_str_lit_63: .asciiz "ff ="
.align 2
_str_lit_65: .asciiz "\n"
.align 2
_x503_: .asciiz "Hola compilador\n"
.align 2
_str_lit_92: .asciiz "IF verdadero\n"
.align 2
_str_lit_106: .asciiz "IF interno verdadero\n"
.align 2
_str_lit_109: .asciiz "IF interno falso\n"
.align 2
_str_lit_110: .asciiz "ELSE\n"
.align 2
str2: .asciiz "switch\n"
.align 2
_str_lit_112: .asciiz "Iteracion do\n"
.align 2
_str_lit_116: .asciiz "CASE 45\n"
.align 2
_str_lit_118: .asciiz "\n"
.align 2
_str_lit_120: .asciiz "CASE 777\n"
.align 2
_str_lit_121: .asciiz "DEFAULT\n"
.align 2
_str_lit_124: .asciiz "=== Saliendo func1 ===\n"
.align 2
_str_lit_126: .asciiz "Entrando a func2\n"
.align 2
_str_lit_128: .asciiz "\n"
.align 2
_str_lit_130: .asciiz "Entrando a func3\n"
.align 2
_str_lit_132: .asciiz "===== MAIN =====\n"
.align 2
str1: .asciiz "Mi string 1\n"
.align 2
_str_lit_144: .asciiz "Llamando func1\n"
.align 2
_str_lit_153: .asciiz "Resultado final ="
.align 2
_str_lit_155: .asciiz "\n===== FIN =====\n"

.text
.globl main
func1:
addi $sp, $sp, -76
sw $ra, 0($sp)
sw $a0, 4($sp)
sw $a1, 8($sp)
la $a0, _str_lit_0
li $v0, 4
syscall
li $t0, 20
move $t1, $t0
li $t2, 5
move $t3, $t2
add $t4, $t1, $t3
sw $t4, 20($sp)
li $t5, 0
sw $t5, 24($sp)
li $t6, 97
sb $t6, 28($sp)
li.s $f0, 0.0
s.s $f0, 32($sp)
la $a0, _str_lit_6
li $v0, 4
syscall
lw $t7, 20($sp)
move $a0, $t7
li $v0, 1
syscall
la $a0, _str_lit_8
li $v0, 4
syscall
lw $t8, 20($sp)
li $t9, 1
add $t0, $t8, $t9
sw $t0, 20($sp)
li $t2, 4
move $t1, $t2
mul $t3, $t0, $t1
li $t4, 2
move $t5, $t4
li $t6, 3
move $t7, $t6
move $a0, $t5
move $a1, $t7
jal potenciaMIPS
move $t8, $v0
div $t9, $t3, $t8
lw $t2, 20($sp)
li $t0, 1
sub $t1, $t2, $t0
sw $t1, 20($sp)
div $t9, $t1
mfhi $t4
sw $t4, 24($sp)
la $a0, _str_lit_16
li $v0, 4
syscall
lw $t6, 24($sp)
move $a0, $t6
li $v0, 1
syscall
la $a0, _str_lit_18
li $v0, 4
syscall
li.s $f1, 4.0
mov.s $f2, $f1
li.s $f3, 2.0
mov.s $f4, $f3
mul.s $f5, $f2, $f4
li.s $f6, 2.0
mov.s $f7, $f6
div.s $f8, $f5, $f7
li.s $f9, 6.0
mov.s $f0, $f9
li.s $f1, 3.0
mov.s $f3, $f1
div.s $f6, $f0, $f3
sub.s $f9, $f8, $f6
s.s $f9, 32($sp)
la $a0, _str_lit_24
li $v0, 4
syscall
l.s $f1, 32($sp)
mov.s $f12, $f1
li $v0, 2
syscall
la $a0, _str_lit_26
li $v0, 4
syscall
li.s $f1, 1.0
mov.s $f9, $f1
s.s $f9, 36($sp)
li $t5, 33
sb $t5, 40($sp)
li $t7, -1
move $t3, $t7
sw $t3, 44($sp)
li $t8, 0
move $t2, $t8
sb $t2, 48($sp)
li $t0, 4
move $t9, $t0
li $t1, 5
move $t4, $t1
la $t6, _x50_
sw $t9, 0($t6)
la $t5, _x50_
sw $t4, 4($t5)
li $t7, 0
move $t3, $t7
li $t8, 1
move $t2, $t8
mul $t0, $t3, $t2
li $t1, 1
move $t6, $t1
li $t9, 0
move $t5, $t9
add $t4, $t6, $t5
li $t7, 99
move $t8, $t7
la $t3, _x50_
li $t2, 0
mul $t0, $t0, $t2
add $t0, $t0, $t4
sll $t0, $t0, 2
add $t0, $t3, $t0
sw $t8, 0($t0)
li.s $f1, 1.5
mov.s $f6, $f1
li.s $f1, 2.5
mov.s $f6, $f1
li.s $f1, 3.5
mov.s $f9, $f1
li.s $f1, 4.5
mov.s $f4, $f1
la $t1, _x502_
sw $t9, 0($t1)
la $t6, _x502_
sw $f6, 4($t6)
la $t5, _x502_
sw $f9, 0($t5)
la $t7, _x502_
sw $f4, 4($t7)
la $a0, _str_lit_50
li $v0, 4
syscall
li $t3, 0
move $t2, $t3
li $t4, 1
move $t0, $t4
la $t8, _x50_
li $t1, 0
mul $t2, $t2, $t1
add $t2, $t2, $t0
sll $t2, $t2, 2
add $t2, $t8, $t2
lw $t9, 0($t2)
move $a0, $t9
li $v0, 1
syscall
la $a0, _str_lit_55
li $v0, 4
syscall
li $t6, 2
move $t5, $t6
lw $t7, 44($sp)
add $t3, $t7, $t5
li $t4, 2
move $t8, $t4
lw $t1, 44($sp)
sub $t0, $t1, $t8
la $t2, _x502_
li $t6, 0
mul $t3, $t3, $t6
add $t3, $t3, $t0
sll $t3, $t3, 2
add $t3, $t2, $t3
lw $t7, 0($t3)
li.s $f1, 2.0
mov.s $f9, $f1
mul.s $f1, $t7, $f9
s.s $f1, 36($sp)
la $a0, _str_lit_63
li $v0, 4
syscall
l.s $f1, 36($sp)
mov.s $f12, $f1
li $v0, 2
syscall
la $a0, _str_lit_65
li $v0, 4
syscall
la $a0, _x503_
li $v0, 4
syscall
li $t5, 30
move $t4, $t5
lw $t1, 20($sp)
ble $t1, $t4, L0
j L1
L0:
li $t8, 1
move $t2, $t8
j L2
L1:
li $t6, 0
move $t2, $t6
L2:
li.s $f1, 5.0
mov.s $f2, $f1
l.s $f1, 36($sp)
c.lt.s $f1, $f2
bc1t L3
j L4
L3:
li $t0, 1
move $t3, $t0
j L5
L4:
li $t7, 0
move $t3, $t7
L5:
li $t5, 0
beq $t2, $t5, L6
move $t1, $t3
j L7
L6:
li $t8, 0
move $t1, $t8
L7:
li $t6, 0
move $t0, $t6
li $t7, 0
move $t5, $t7
beq $t0, $t5, L8
j L9
L8:
li $t3, 1
move $t8, $t3
j L10
L9:
li $t6, 0
move $t8, $t6
L10:
lb $t7, 48($sp)
li $t3, 0
beq $t7, $t3, L11
move $t6, $t8
j L12
L11:
li $t7, 0
move $t6, $t7
L12:
li $t3, 0
move $t8, $t3
li $t7, 1
move $t3, $t7
bne $t8, $t3, L13
j L14
L13:
li $t7, 1
move $t9, $t7
j L15
L14:
li $t7, 0
move $t9, $t7
L15:
li $t7, 1
beq $t6, $t7, L16
move $t7, $t9
j L17
L16:
li $t9, 1
move $t7, $t9
L17:
li $t9, 1
beq $t1, $t9, L18
move $t9, $t7
j L19
L18:
li $t7, 1
move $t9, $t7
L19:
li $t7, 0
beq $t9, $t7, L20
la $a0, _str_lit_92
li $v0, 4
syscall
li $t7, 0
sw $t7, 56($sp)
li.s $f1, 5.0
mov.s $f6, $f1
l.s $f1, 36($sp)
c.lt.s $f6, $f1
bc1t L22
j L23
L22:
li $t7, 1
move $t1, $t7
j L24
L23:
li $t7, 0
move $t1, $t7
L24:
li $t7, 30
move $t5, $t7
lw $t7, 20($sp)
bgt $t7, $t5, L25
j L26
L25:
li $t7, 1
move $t0, $t7
j L27
L26:
li $t7, 0
move $t0, $t7
L27:
li $t7, 1
beq $t1, $t7, L28
move $t7, $t0
j L29
L28:
li $t0, 1
move $t7, $t0
L29:
li $t0, 1
sub $t1, $t0, $t7
li $t0, 0
beq $t1, $t0, L30
la $a0, _str_lit_106
li $v0, 4
syscall
li $t7, 0
sw $t7, 60($sp)
li $t0, 97
sb $t0, 64($sp)
j L31
L30:
la $a0, _str_lit_109
li $v0, 4
syscall
L31:
j L21
L20:
la $a0, _str_lit_110
li $v0, 4
syscall
li $t7, 45
move $t0, $t7
sw $t0, 68($sp)
L32:
la $a0, _str_lit_112
li $v0, 4
syscall
lw $t7, 68($sp)
move $t0, $t7
li $t7, 45
move $t5, $t7
li $t7, 777
move $t6, $t7
beq $t0, $t6, L37
beq $t0, $t5, L36
j L35
L36:
la $a0, _str_lit_116
li $v0, 4
syscall
lw $t7, 24($sp)
move $a0, $t7
li $v0, 1
syscall
la $a0, _str_lit_118
li $v0, 4
syscall
li $t7, 200
move $t1, $t7
sw $t1, 24($sp)
j L34
L37:
la $a0, _str_lit_120
li $v0, 4
syscall
L35:
la $a0, _str_lit_121
li $v0, 4
syscall
L34:
li $t7, 0
move $t1, $t7
li $t7, 0
bne $t1, $t7, L32
L33:
L21:
la $a0, _str_lit_124
li $v0, 4
syscall
li.s $f1, 1.5
mov.s $f6, $f1
mov.s $f0, $f6
lw $ra, 0($sp)
addi $sp, $sp, 76
jr $ra
j func1_end
func1_end:
_func2_:
addi $sp, $sp, -12
sw $ra, 0($sp)
s.s $f12, 4($sp)
sw $a0, 8($sp)
la $a0, _str_lit_126
li $v0, 4
syscall
lw $t7, 8($sp)
move $a0, $t7
li $v0, 1
syscall
la $a0, _str_lit_128
li $v0, 4
syscall
lw $t7, 8($sp)
move $v0, $t7
lw $ra, 0($sp)
addi $sp, $sp, 12
jr $ra
j _func2__end
_func2__end:
_func3_:
addi $sp, $sp, -4
sw $ra, 0($sp)
la $a0, _str_lit_130
li $v0, 4
syscall
li $t7, 1
move $t1, $t7
move $v0, $t1
lw $ra, 0($sp)
addi $sp, $sp, 4
jr $ra
j _func3__end
_func3__end:
main:
addi $sp, $sp, -20
la $a0, _str_lit_132
li $v0, 4
syscall
li $t7, 33
sb $t7, 0($sp)
li.s $f1, 5.5
mov.s $f6, $f1
s.s $f6, 8($sp)
li $t7, 10
move $t1, $t7
sw $t1, 12($sp)
li.s $f1, 6.0
mov.s $f6, $f1
li.s $f1, 8.0
mov.s $f6, $f1
c.eq.s $t7, $f6
bc1f L38
j L39
L38:
li $t1, 1
move $t7, $t1
j L40
L39:
li $t1, 0
move $t7, $t1
L40:
sb $t7, 16($sp)
li $t1, 4
move $t7, $t1
li $t1, 5
move $t3, $t1
la $t1, _x50_
sw $t7, 0($t1)
la $t1, _x50_
sw $t3, 4($t1)
la $a0, _str_lit_144
li $v0, 4
syscall
lb $t7, 0($sp)
li $t1, 101
move $a0, $t7
move $a1, $t1
jal func1
mov.s $f1, $f0
li.s $f9, 2.0
mov.s $f6, $f9
mul.s $f9, $f1, $f6
li $t3, 0
move $t7, $t3
li $t1, 0
move $t3, $t1
la $t1, _x50_
li $t0, 0
mul $t7, $t7, $t0
add $t7, $t7, $t3
sll $t7, $t7, 2
add $t7, $t1, $t7
lw $t1, 0($t7)
lw $t0, 12($sp)
div $t3, $t0, $t1
mov.s $f12, $f9
move $a0, $t3
jal _func2_
move $t7, $v0
sw $t7, 12($sp)
la $a0, _str_lit_153
li $v0, 4
syscall
lw $t0, 12($sp)
move $a0, $t0
li $v0, 1
syscall
la $a0, _str_lit_155
li $v0, 4
syscall
addi $sp, $sp, 20
li $v0, 10
syscall
main_end:
potenciaMIPS:
li $v0, 1
buclePotenciaEntero:
blez $a1, finPotenciaEntero
mul $v0, $v0, $a0
addi $a1, $a1, -1
j buclePotenciaEntero
finPotenciaEntero:
jr $ra
li $v0, 10
syscall
