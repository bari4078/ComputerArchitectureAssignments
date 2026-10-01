addi $t0, $zero, 6
sw $t0, 4($zero)
beq $zero, $zero, taken
sw $zero, 4($zero)
taken: j landed
addi $t1, $zero, 7
landed: bneq $zero, $zero, finish
addi $t2, $zero, 3
finish: sw $t2, 5($zero)
addi $zero, $zero, 0
addi $zero, $zero, 0
