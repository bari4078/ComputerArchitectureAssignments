addi $t0, $zero, 7
addi $t0, $t0, 7
sw $t0, 7($zero)
lw $sp, 7($zero)
beq $sp, $t0, restored
addi $t1, $zero, 7
restored: addi $t1, $t1, 1
addi $sp, $sp, 1
sw $t1, 2($zero)
addi $zero, $zero, 0
addi $zero, $zero, 0
