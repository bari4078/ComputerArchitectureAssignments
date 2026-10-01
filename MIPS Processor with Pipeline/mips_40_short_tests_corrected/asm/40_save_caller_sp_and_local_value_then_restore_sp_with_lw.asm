addi $t0, $sp, 0
subi $sp, $sp, 1
sw $t0, 0($sp)
subi $sp, $sp, 1
addi $t1, $zero, 6
sw $t1, 0($sp)
lw $t2, 0($sp)
addi $sp, $sp, 1
lw $sp, 0($sp)
sw $sp, 2($zero)
sw $t2, 3($zero)
addi $zero, $zero, 0
addi $zero, $zero, 0
