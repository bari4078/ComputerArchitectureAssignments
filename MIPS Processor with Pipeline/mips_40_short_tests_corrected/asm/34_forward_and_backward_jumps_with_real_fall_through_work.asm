addi $t0, $zero, 3
j loop
addi $t2, $t2, 4
loop: addi $t1, $t1, 2
subi $t0, $t0, 1
beq $t0, $zero, done_loop
addi $t2, $t2, 1
j loop
addi $t2, $t2, 4
done_loop: sw $t1, 2($zero)
sw $t2, 3($zero)
addi $zero, $zero, 0
addi $zero, $zero, 0
