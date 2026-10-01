addi $t0, $zero, 3
addi $t3, $zero, 2
loop: subi $sp, $sp, 1
sw $t0, 0($sp)
lw $t1, 0($sp)
addi $sp, $sp, 1
sw $t1, 0($t3)
addi $t3, $t3, 1
subi $t0, $t0, 1
bneq $t0, $zero, loop
addi $t2, $t2, 1
addi $zero, $zero, 0
addi $zero, $zero, 0
