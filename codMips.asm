.data
.align 2
_str_lit_2: .asciiz "\n"
.align 2
str1: .asciiz "Mi string~1\n"
.align 2
_str_lit_5: .asciiz "\n"
.align 2
_x50_: .space 8

.text
.globl main
main:
__main__:
addi $sp, $sp, -12
li $t0, 33
sb $t0, 0($sp)
lb $t1, 0($sp)
move $a0, $t1
li $v0, 11
syscall
la $a0, _str_lit_2
li $v0, 4
syscall
la $a0, str1
li $v0, 4
syscall
li $t2, 10
move $t3, $t2
sw $t3, 4($sp)
lw $t4, 4($sp)
move $a0, $t4
li $v0, 1
syscall
la $a0, _str_lit_5
li $v0, 4
syscall
li.s $f0, 6.7
mov.s $f1, $f0
li.s $f2, 8.9
mov.s $f3, $f2
c.eq.s $f1, $f3
bc1f L0
j L1
L0:
li $t5, 1
move $t6, $t5
j L2
L1:
li $t7, 0
move $t6, $t7
L2:
sb $t6, 8($sp)
li $t8, 4
move $t9, $t8
li $t0, 5
move $t1, $t0
la $t2, _x50_
sw $t9, 0($t2)
la $t3, _x50_
sw $t1, 4($t3)
li $t4, 0
move $t5, $t4
li $t7, 0
move $t6, $t7
la $t8, _x50_
li $t0, 0
mul $t5, $t5, $t0
add $t5, $t5, $t6
sll $t5, $t5, 2
add $t5, $t8, $t5
lw $t2, 0($t5)
move $a0, $t2
li $v0, 1
syscall
addi $sp, $sp, 12
__main___end:
li $v0, 10
syscall
