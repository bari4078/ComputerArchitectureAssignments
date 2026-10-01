addi $t0, $zero, 1
sll $t1, $t0, 3
sw $t1, 2($zero)
srl $t2, $t1, 1
sw $t2, 3($zero)
sll $t3, $t1, 0
sw $t3, 4($zero)
srl $t4, $t3, 0
sw $t4, 5($zero)
sll $t3, $t1, 1
sw $t3, 6($zero)
srl $t4, $t1, 3
sw $t4, 7($zero)
addi $zero, $zero, 0
addi $zero, $zero, 0
