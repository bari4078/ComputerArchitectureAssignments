addi $t0, $zero, 7
sll $t0, $t0, 1
sll $t1, $t0, 4
sw $t1, 2($zero)
srl $t2, $t0, 4
sw $t2, 3($zero)
sll $t3, $t0, 15
sw $t3, 4($zero)
srl $t4, $t0, 15
sw $t4, 5($zero)
addi $zero, $zero, 0
addi $zero, $zero, 0
