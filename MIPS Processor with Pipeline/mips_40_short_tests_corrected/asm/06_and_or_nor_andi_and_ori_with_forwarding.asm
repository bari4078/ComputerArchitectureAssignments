addi $t0, $zero, 6
addi $t1, $zero, 3
and $t2, $t0, $t1
sw $t2, 2($zero)
or $t3, $t0, $t1
sw $t3, 3($zero)
nor $t4, $t0, $t1
sw $t4, 4($zero)
andi $t2, $t4, 3
sw $t2, 5($zero)
ori $t3, $t2, 5
sw $t3, 6($zero)
addi $zero, $zero, 0
addi $zero, $zero, 0
