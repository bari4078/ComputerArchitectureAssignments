addi $t4, $zero, 3
sw $t4, 7($zero)
addi $t1, $zero, 6
addi $t0, $zero, 6
lw $t0, 7($zero)
beq $t0, $t1, finish
addi $t2, $zero, 2
finish: sw $t2, 2($zero)
addi $zero, $zero, 0
addi $zero, $zero, 0
