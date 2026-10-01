addi $t0, $zero, 3
subi $sp, $sp, 1
sw $t0, 0($sp)
addi $t0, $zero, 6
subi $sp, $sp, 1
sw $t0, 0($sp)
lw $t1, 0($sp)
addi $sp, $sp, 1
lw $t2, 0($sp)
addi $sp, $sp, 1
sw $t1, 2($zero)
sw $t2, 3($zero)
addi $zero, $zero, 0
addi $zero, $zero, 0
