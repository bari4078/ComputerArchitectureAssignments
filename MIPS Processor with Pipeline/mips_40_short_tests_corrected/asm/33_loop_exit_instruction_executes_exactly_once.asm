addi $t0, $zero, 3
loop: addi $t1, $t1, 2
subi $t0, $t0, 1
bneq $t0, $zero, loop
addi $t2, $t2, 1
sw $t1, 2($zero)
sw $t2, 3($zero)
addi $zero, $zero, 0
addi $zero, $zero, 0
addi $zero, $zero, 0
