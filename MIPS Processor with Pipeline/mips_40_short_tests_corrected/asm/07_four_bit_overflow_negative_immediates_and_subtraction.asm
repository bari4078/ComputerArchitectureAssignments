addi $t0, $zero, 7
addi $t1, $zero, 7
add $t2, $t0, $t1
sw $t2, 2($zero)
addi $t2, $t2, 3
sw $t2, 3($zero)
subi $t3, $zero, 1
sw $t3, 4($zero)
sub $t4, $t1, $t3
sw $t4, 5($zero)
addi $t0, $zero, -8
sw $t0, 6($zero)
subi $t1, $t0, -1
addi $zero, $zero, 0
addi $zero, $zero, 0
