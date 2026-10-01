beq $zero, $zero, next_one
next_one: addi $t0, $t0, 1
bneq $t0, $zero, next_two
next_two: addi $t1, $t1, 2
j next_three
next_three: addi $t2, $t2, 3
sw $t0, 2($zero)
sw $t1, 3($zero)
sw $t2, 4($zero)
addi $zero, $zero, 0
addi $zero, $zero, 0
