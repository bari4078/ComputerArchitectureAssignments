addi $t0, $zero, 2
addi $t1, $zero, 4
addi $t2, $zero, 6
sw $t0, -1($sp)
sw $t1, -2($sp)
sw $t2, -3($sp)
lw $t3, -3($sp)
lw $t4, -1($sp)
sw $t3, 2($zero)
sw $t4, 3($zero)
addi $zero, $zero, 0
addi $zero, $zero, 0
