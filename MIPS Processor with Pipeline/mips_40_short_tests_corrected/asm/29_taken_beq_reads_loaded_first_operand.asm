addi $t4, $zero, 6
sw $t4, 7($zero)
addi $t0, $zero, 1
addi $t1, $zero, 6
lw $t0, 7($zero)
beq $t0, $t1, matched
sw $zero, 7($zero)
matched: addi $t2, $zero, 3
sw $t2, 2($zero)
addi $zero, $zero, 0
addi $zero, $zero, 0
