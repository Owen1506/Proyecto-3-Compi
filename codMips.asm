.data
str1_str: .asciiz "Mi string 1"
_x50_: .space 8

.text
.globl main
__main__:
addi $sp, $sp, -96
li $t0, 33
sb $t0, 0($sp)
la $t0, str1_str
sw $t0, 4($sp)
li $t0, 1113744998
mtc1 $t0, $f0
swc1 $f0, 12($sp)
lwc1 $f0, 12($sp)
swc1 $f0, 8($sp)
li $t0, 10
sw $t0, 20($sp)
li $t0, 20
sw $t0, 24($sp)
lw $t0, 20($sp)
lw $t1, 24($sp)
add $t0, $t0, $t1
sw $t0, 28($sp)
li $t0, 30
sw $t0, 32($sp)
lw $t0, 28($sp)
lw $t1, 32($sp)
add $t0, $t0, $t1
sw $t0, 36($sp)
lw $t0, 36($sp)
sw $t0, 16($sp)
li $t0, 1087792742
mtc1 $t0, $f0
swc1 $f0, 44($sp)
li $t0, 1091462758
mtc1 $t0, $f0
swc1 $f0, 48($sp)
lwc1 $f0, 44($sp)
lwc1 $f2, 48($sp)
c.eq.s $f0, $f2
bc1f L0
j L1
L0:
li $t0, 1
sw $t0, 52($sp)
j L2
L1:
li $t0, 0
sw $t0, 52($sp)
L2:
lw $t0, 52($sp)
sw $t0, 40($sp)
L3:
li $t0, 5
sw $t0, 60($sp)
lw $t0, 60($sp)
sw $t0, 56($sp)
li $t0, 1
sw $t0, 64($sp)
li $t0, 0
sw $t0, 68($sp)
li $t0, 1
sw $t0, 72($sp)
lw $t0, 68($sp)
li $t1, 0
beq $t0, $t1, L5
lw $t0, 72($sp)
sw $t0, 76($sp)
j L6
L5:
li $t0, 0
sw $t0, 76($sp)
L6:
lw $t0, 64($sp)
li $t1, 1
beq $t0, $t1, L7
lw $t0, 76($sp)
sw $t0, 80($sp)
j L8
L7:
li $t0, 1
sw $t0, 80($sp)
L8:
lw $t0, 80($sp)
li $t1, 0
bne $t0, $t1, L3
L4:
li $t0, 4
sw $t0, 88($sp)
li $t0, 5
sw $t0, 92($sp)
addi $sp, $sp, 96
__main___end:
