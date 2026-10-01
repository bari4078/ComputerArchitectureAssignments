addi $zero, $zero, 6
add $t0, $zero, $zero
addi $t4, $zero, 6
sw $t4, 7($zero)
lw $zero, 7($zero)
sw $zero, 2($zero)
beq $zero, $zero, valid_zero
addi $t1, $zero, 7
valid_zero: bneq $zero, $zero, finish
addi $t2, $zero, 3
finish: sw $t2, 3($zero)
addi $zero, $zero, 0
addi $zero, $zero, 0
