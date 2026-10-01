addi $t1, $zero, 3
addi $t0, $zero, 0
addi $t0, $zero, 3
beq $t0, $t1, matched
addi $t2, $zero, 7
matched: addi $t2, $t2, 1
sw $t2, 2($zero)
addi $zero, $zero, 0
addi $zero, $zero, 0
addi $zero, $zero, 0
