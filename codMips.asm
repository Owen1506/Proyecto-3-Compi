.data
.align 2
_str_lit_0: .asciiz "Entrando a sumar\n"
.align 2
_str_lit_3: .asciiz "Entrando a restar\n"
.align 2
_str_lit_6: .asciiz "Entrando a multiplicar\n"
.align 2
_str_lit_9: .asciiz "Entrando a promedio\n"
.align 2
_str_lit_14: .asciiz "Entrando a siguiente\n"
.align 2
_str_lit_24: .asciiz "==========\n"
.align 2
_str_lit_28: .asciiz "\n"
.align 2
_str_lit_32: .asciiz "\n"
.align 2
_str_lit_36: .asciiz "\n"
.align 2
_str_lit_40: .asciiz "\n"
.align 2
_str_lit_43: .asciiz "\n"
.align 2
_str_lit_44: .asciiz "Llamadas anidadas enteras\n"
.align 2
_str_lit_50: .asciiz "\n"
.align 2
_str_lit_51: .asciiz "Llamadas anidadas flotantes\n"
.align 2
_str_lit_57: .asciiz "\n"
.align 2
_str_lit_58: .asciiz "==========\n"

.text
.globl main
sumar:
addi $sp, $sp, -12
sw $ra, 8($sp)
sw $a0, 0($sp)
sw $a1, 4($sp)
la $a0, _str_lit_0
li $v0, 4
syscall
lw $t0, 0($sp)
lw $t1, 4($sp)
add $t2, $t0, $t1
move $v0, $t2
lw $ra, 8($sp)
addi $sp, $sp, 12
jr $ra
j sumar_end
sumar_end:
restar:
addi $sp, $sp, -12
sw $ra, 8($sp)
sw $a0, 0($sp)
sw $a1, 4($sp)
la $a0, _str_lit_3
li $v0, 4
syscall
lw $t3, 0($sp)
lw $t4, 4($sp)
sub $t5, $t3, $t4
move $v0, $t5
lw $ra, 8($sp)
addi $sp, $sp, 12
jr $ra
j restar_end
restar_end:
multiplicar:
addi $sp, $sp, -12
sw $ra, 8($sp)
sw $a0, 0($sp)
sw $a1, 4($sp)
la $a0, _str_lit_6
li $v0, 4
syscall
lw $t6, 0($sp)
lw $t7, 4($sp)
mul $t8, $t6, $t7
move $v0, $t8
lw $ra, 8($sp)
addi $sp, $sp, 12
jr $ra
j multiplicar_end
multiplicar_end:
promedio:
addi $sp, $sp, -12
sw $ra, 8($sp)
s.s $f12, 0($sp)
s.s $f14, 4($sp)
la $a0, _str_lit_9
li $v0, 4
syscall
li.s $f0, 2.0
mov.s $f1, $f0
s.s $f1, 8($sp)
lw $t9, 4($sp)
l.s $f2, 8($sp)
div.s $f3, $t9, $f2
lw $t0, 0($sp)
add.s $f4, $t0, $f3
mov.s $f0, $f4
lw $ra, 12($sp)
addi $sp, $sp, 16
jr $ra
j promedio_end
promedio_end:
siguiente:
addi $sp, $sp, -8
sw $ra, 4($sp)
sw $a0, 0($sp)
la $a0, _str_lit_14
li $v0, 4
syscall
lb $t1, 0($sp)
move $v0, $t1
lw $ra, 4($sp)
addi $sp, $sp, 8
jr $ra
j siguiente_end
siguiente_end:
main:
addi $sp, $sp, -28
li $t3, 10
move $t4, $t3
sw $t4, 0($sp)
li $t6, 5
move $t7, $t6
sw $t7, 4($sp)
li $t9, 0
sw $t9, 8($sp)
li.s $f5, 8.0
mov.s $f6, $f5
s.s $f6, 12($sp)
li.s $f7, 4.0
mov.s $f8, $f7
s.s $f8, 16($sp)
li.s $f9, 0.0
s.s $f9, 20($sp)
li $t0, 65
sb $t0, 24($sp)
li $t1, 48
sb $t1, 25($sp)
la $a0, _str_lit_24
li $v0, 4
syscall
lw $t3, 0($sp)
move $a0, $t3
lw $t4, 4($sp)
move $a1, $t4
jal sumar
move $t6, $v0
sw $t6, 8($sp)
lw $t7, 8($sp)
move $a0, $t7
li $v0, 1
syscall
la $a0, _str_lit_28
li $v0, 4
syscall
lw $t9, 8($sp)
move $a0, $t9
li $t0, 2
move $t1, $t0
move $a1, $t1
jal restar
move $t3, $v0
sw $t3, 8($sp)
lw $t4, 8($sp)
move $a0, $t4
li $v0, 1
syscall
la $a0, _str_lit_32
li $v0, 4
syscall
lw $t6, 8($sp)
move $a0, $t6
li $t7, 3
move $t9, $t7
move $a1, $t9
jal multiplicar
move $t0, $v0
sw $t0, 8($sp)
lw $t3, 8($sp)
move $a0, $t3
li $v0, 1
syscall
la $a0, _str_lit_36
li $v0, 4
syscall
l.s $f0, 12($sp)
mov.s $f12, $f0
l.s $f2, 16($sp)
mov.s $f14, $f2
jal promedio
mov.s $f5, $f0
s.s $f5, 20($sp)
l.s $f7, 20($sp)
mov.s $f12, $f7
li $v0, 2
syscall
la $a0, _str_lit_40
li $v0, 4
syscall
lb $t4, 24($sp)
move $a0, $t4
jal siguiente
move $t6, $v0
sb $t6, 25($sp)
lb $t7, 25($sp)
move $a0, $t7
li $v0, 11
syscall
la $a0, _str_lit_43
li $v0, 4
syscall
la $a0, _str_lit_44
li $v0, 4
syscall
li $t0, 2
move $t3, $t0
move $a0, $t3
li $t4, 3
move $t6, $t4
move $a1, $t6
jal sumar
move $t7, $v0
move $a0, $t7
li $t0, 8
move $t4, $t0
move $a1, $t4
li $t0, 4
move $t4, $t0
move $a2, $t4
jal restar
move $t0, $v0
move $a0, $t0
jal multiplicar
move $t7, $v0
sw $t7, 8($sp)
lw $t7, 8($sp)
move $a0, $t7
li $v0, 1
syscall
la $a0, _str_lit_50
li $v0, 4
syscall
la $a0, _str_lit_51
li $v0, 4
syscall
li.s $f9, 8.0
mov.s $f0, $f9
mov.s $f12, $f0
li.s $f2, 4.0
mov.s $f7, $f2
mov.s $f14, $f7
jal promedio
mov.s $f9, $f0
mov.s $f12, $f9
li.s $f2, 10.0
mov.s $f1, $f2
mov.s $f14, $f1
li.s $f2, 2.0
mov.s $f3, $f2
s.s $f3, 0($sp)
addi $sp, $sp, -4
jal promedio
addi $sp, $sp, 4
mov.s $f2, $f0
mov.s $f12, $f2
jal promedio
mov.s $f2, $f0
s.s $f2, 20($sp)
l.s $f4, 20($sp)
mov.s $f12, $f4
li $v0, 2
syscall
la $a0, _str_lit_57
li $v0, 4
syscall
la $a0, _str_lit_58
li $v0, 4
syscall
addi $sp, $sp, 28
li $v0, 10
syscall
main_end:
li $v0, 10
syscall
