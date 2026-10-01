addi $t1, $zero, 1 
addi $t2, $zero, 3 
add $t0, $t1, $t2 
add $t3, $t0, $t2 
add $t4, $t0, $t1 
sw $t1, 3($t2) 
sll $t1, $t1, 2 
beq $t0, $t1, label1 
j end
label1:
sub $t4, $t3, $t0 
subi $t3, $t3, 1 
srl $t3, $t3, 1 
lw $t1, 3($t2) 
and $t0, $t1, $t3 
or $t1, $t3, $t4 
j label2
label3:
subi $sp, $sp, 2 
sw $t0, 1($sp)
sw $t1, 0($sp)
ori $t0, $t0, 4 
lw $t0, 0($sp) 
addi $sp, $sp, 1
andi $t2, $t2, 0 
lw $t2, 0($sp)
addi $sp, $sp, 1 
nor $t2, $t2, $t2 
j end
label2:
bneq $t0, $t2, label4
label4: 
j label3 
end: