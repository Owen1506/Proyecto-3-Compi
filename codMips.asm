.data
.align 2
_str_lit_0: .asciiz "Entrando a sumar\n"
.align 2
_str_lit_1: .asciiz "Entrando a restar\n"
.align 2
_str_lit_2: .asciiz "Entrando a multiplicar\n"
.align 2
_str_lit_3: .asciiz "Entrando a siguiente\n"
.align 2
_str_lit_4: .asciiz "==========\n"
.align 2
_str_lit_5: .asciiz "\n"
.align 2
_str_lit_6: .asciiz "\n"
.align 2
_str_lit_7: .asciiz "\n"
.align 2
_str_lit_8: .asciiz "\n"
.align 2
_str_lit_9: .asciiz "Llamadas anidadas enteras\n"
.align 2
_str_lit_10: .asciiz "\n"
.align 2
_str_lit_11: .asciiz "==========\n"

.text
.globl main
sumar:
addi $sp, $sp, -12
sw $ra, 0($sp)
sw $a0, 4($sp)
sw $a1, 8($sp)
la $a0, _str_lit_0
li $v0, 4
syscall
lw $t0, 4($sp)
lw $t1, 8($sp)
add $t2, $t0, $t1
move $v0, $t2
lw $ra, 0($sp)
addi $sp, $sp, 12
jr $ra
j sumar_end
sumar_end:
restar:
addi $sp, $sp, -12
sw $ra, 0($sp)
sw $a0, 4($sp)
sw $a1, 8($sp)
la $a0, _str_lit_1
li $v0, 4
syscall
lw $t3, 4($sp)
lw $t4, 8($sp)
sub $t5, $t3, $t4
move $v0, $t5
lw $ra, 0($sp)
addi $sp, $sp, 12
jr $ra
j restar_end
restar_end:
multiplicar:
addi $sp, $sp, -12
sw $ra, 0($sp)
sw $a0, 4($sp)
sw $a1, 8($sp)
la $a0, _str_lit_2
li $v0, 4
syscall
lw $t6, 4($sp)
lw $t7, 8($sp)
mul $t8, $t6, $t7
move $v0, $t8
lw $ra, 0($sp)
addi $sp, $sp, 12
jr $ra
j multiplicar_end
multiplicar_end:
siguiente:
addi $sp, $sp, -8
sw $ra, 0($sp)
sw $a0, 4($sp)
la $a0, _str_lit_3
li $v0, 4
syscall
lb $t9, 4($sp)
move $v0, $t9
lw $ra, 0($sp)
addi $sp, $sp, 8
jr $ra
j siguiente_end
siguiente_end:
main:
addi $sp, $sp, -28
li $t0, 10
move $t1, $t0
sw $t1, 0($sp)
li $t3, 5
move $t4, $t3
sw $t4, 4($sp)
li $t6, 0
sw $t6, 8($sp)
li.s $f0, 8.0
mov.s $f1, $f0
s.s $f1, 12($sp)
li.s $f2, 4.0
mov.s $f3, $f2
s.s $f3, 16($sp)
li.s $f4, 0.0
s.s $f4, 20($sp)
li $t7, 65
sb $t7, 24($sp)
li $t9, 48
sb $t9, 25($sp)
la $a0, _str_lit_4
li $v0, 4
syscall
lw $t0, 0($sp)
lw $t1, 4($sp)
move $a0, $t0
move $a1, $t1
jal sumar
move $t3, $v0
sw $t3, 8($sp)
lw $t4, 8($sp)
move $a0, $t4
li $v0, 1
syscall
la $a0, _str_lit_5
li $v0, 4
syscall
lw $t6, 8($sp)
li $t7, 2
move $t9, $t7
move $a0, $t6
move $a1, $t9
jal restar
move $t0, $v0
sw $t0, 8($sp)
lw $t1, 8($sp)
move $a0, $t1
li $v0, 1
syscall
la $a0, _str_lit_6
li $v0, 4
syscall
lw $t3, 8($sp)
li $t4, 3
move $t6, $t4
move $a0, $t3
move $a1, $t6
jal multiplicar
move $t7, $v0
sw $t7, 8($sp)
lw $t0, 8($sp)
move $a0, $t0
li $v0, 1
syscall
la $a0, _str_lit_7
li $v0, 4
syscall
lb $t1, 24($sp)
move $a0, $t1
jal siguiente
move $t3, $v0
sb $t3, 25($sp)
lb $t4, 25($sp)
move $a0, $t4
li $v0, 11
syscall
la $a0, _str_lit_8
li $v0, 4
syscall
la $a0, _str_lit_9
li $v0, 4
syscall
li $t7, 2
move $t0, $t7
li $t1, 3
move $t3, $t1
move $a0, $t0
move $a1, $t3
jal sumar
move $t4, $v0
li $t7, 8
move $t1, $t7
li $t7, 4
move $t9, $t7
move $a0, $t4
move $a1, $t1
move $a2, $t9
jal restar
move $t7, $v0
move $a0, $t7
jal multiplicar
move $t6, $v0
sw $t6, 8($sp)
lw $t6, 8($sp)
move $a0, $t6
li $v0, 1
syscall
la $a0, _str_lit_10
li $v0, 4
syscall
la $a0, _str_lit_11
li $v0, 4
syscall
addi $sp, $sp, 28
li $v0, 10
syscall
main_end:
li $v0, 10
syscall
