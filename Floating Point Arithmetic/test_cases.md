## Assumptions Used

These test cases assume the assignment format:

- 16 bits: `$1$` sign bit, `$5$` exponent bits, `$10$` fraction bits
- Bias `$=15$`
- Normalized value:

$$
(-1)^S \times (1.F)_2 \times 2^{E-15}
$$

- Exponent `$00000$` is treated as **underflow / not a valid normalized result**
- Exponent `$11111$` is treated as **overflow / not a finite normalized result**
- Rounding mode assumed: **round to nearest, ties to even**
- Guard, round, sticky bits are written as `$G,R,S$`

---

## Useful Reference Values

- Minimum positive normalized value:

$$
1.0_2 \times 2^{-14} = 0.00006103515625
$$

Binary:

$$
0\ 00001\ 0000000000
$$

- Maximum positive finite value:

$$
1.1111111111_2 \times 2^{15} = 65504
$$

Binary:

$$
0\ 11110\ 1111111111
$$

---

# Comprehensive Floating Point Adder Test Cases

## Test Case 1: Simple Addition, Same Exponent

### Calculation

$$
1.5 + 1.25 = 2.75
$$

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| `$1.5$` | `$1.5$` | `$0$` | `$01111$` | `$1000000000$` | `$0\ 01111\ 1000000000$` |
| `$1.25$` | `$1.25$` | `$0$` | `$01111$` | `$0100000000$` | `$0\ 01111\ 0100000000$` |

### Work

$$
1.1000000000_2 \times 2^0 + 1.0100000000_2 \times 2^0
$$

$$
= 10.1100000000_2 \times 2^0
$$

Normalize:

$$
10.1100000000_2 \times 2^0 = 1.0110000000_2 \times 2^1
$$

### GRS Bits

No shifted-out bits:

$$
G=0,\ R=0,\ S=0
$$

### Expected Output

| Decimal Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---:|---:|---:|---:|---|---|---|
| `$2.75$` | `$0$` | `$10000$` | `$0110000000$` | `$0\ 10000\ 0110000000$` | OFF | OFF |

---

## Test Case 2: Addition with Exponent Alignment, No Rounding

### Calculation

$$
12.0 + 0.5 = 12.5
$$

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| `$12.0$` | `$12.0$` | `$0$` | `$10010$` | `$1000000000$` | `$0\ 10010\ 1000000000$` |
| `$0.5$` | `$0.5$` | `$0$` | `$01110$` | `$0000000000$` | `$0\ 01110\ 0000000000$` |

### Work

$$
12.0 = 1.1000000000_2 \times 2^3
$$

$$
0.5 = 1.0000000000_2 \times 2^{-1}
$$

Exponent difference:

$$
3 - (-1) = 4
$$

Shift the second mantissa right by `$4$`:

$$
1.1000000000_2 + 0.0001000000_2 = 1.1001000000_2
$$

### GRS Bits

The shifted-out bits are all zero:

$$
G=0,\ R=0,\ S=0
$$

### Expected Output

| Decimal Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---:|---:|---:|---:|---|---|---|
| `$12.5$` | `$0$` | `$10010$` | `$1001000000$` | `$0\ 10010\ 1001000000$` | OFF | OFF |

---

## Test Case 3: Addition Requiring Normalization

### Calculation

$$
15.0 + 1.0 = 16.0
$$

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| `$15.0$` | `$15.0$` | `$0$` | `$10010$` | `$1110000000$` | `$0\ 10010\ 1110000000$` |
| `$1.0$` | `$1.0$` | `$0$` | `$01111$` | `$0000000000$` | `$0\ 01111\ 0000000000$` |

### Work

$$
15.0 = 1.1110000000_2 \times 2^3
$$

$$
1.0 = 1.0000000000_2 \times 2^0
$$

Shift `$1.0$` right by `$3$`:

$$
1.1110000000_2 + 0.0010000000_2 = 10.0000000000_2
$$

Normalize:

$$
10.0000000000_2 \times 2^3 = 1.0000000000_2 \times 2^4
$$

### GRS Bits

$$
G=0,\ R=0,\ S=0
$$

### Expected Output

| Decimal Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---:|---:|---:|---:|---|---|---|
| `$16.0$` | `$0$` | `$10011$` | `$0000000000$` | `$0\ 10011\ 0000000000$` | OFF | OFF |

---

## Test Case 4: Positive Plus Negative, Exact Cancellation to Zero

### Calculation

$$
5.5 + (-5.5) = 0
$$

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| `$5.5$` | `$5.5$` | `$0$` | `$10001$` | `$0110000000$` | `$0\ 10001\ 0110000000$` |
| `$-5.5$` | `$-5.5$` | `$1$` | `$10001$` | `$0110000000$` | `$1\ 10001\ 0110000000$` |

### Work

$$
1.0110000000_2 \times 2^2 - 1.0110000000_2 \times 2^2 = 0
$$

### GRS Bits

$$
G=0,\ R=0,\ S=0
$$

### Expected Output

Depending on your design, exact zero can be represented as all zeros:

| Decimal Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---:|---:|---:|---:|---|---|---|
| `$0.0$` | `$0$` | `$00000$` | `$0000000000$` | `$0\ 00000\ 0000000000$` | OFF | OFF |

---

## Test Case 5: Subtraction Result Requiring Left Normalization

### Calculation

$$
1.5 + (-1.25) = 0.25
$$

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| `$1.5$` | `$1.5$` | `$0$` | `$01111$` | `$1000000000$` | `$0\ 01111\ 1000000000$` |
| `$-1.25$` | `$-1.25$` | `$1$` | `$01111$` | `$0100000000$` | `$1\ 01111\ 0100000000$` |

### Work

$$
1.1000000000_2 \times 2^0 - 1.0100000000_2 \times 2^0
$$

$$
= 0.0100000000_2 \times 2^0
$$

Normalize by shifting left `$2$`:

$$
0.0100000000_2 \times 2^0 = 1.0000000000_2 \times 2^{-2}
$$

Biased exponent:

$$
-2 + 15 = 13 = 01101_2
$$

### GRS Bits

$$
G=0,\ R=0,\ S=0
$$

### Expected Output

| Decimal Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---:|---:|---:|---:|---|---|---|
| `$0.25$` | `$0$` | `$01101$` | `$0000000000$` | `$0\ 01101\ 0000000000$` | OFF | OFF |

---

## Test Case 6: Negative Result

### Calculation

$$
2.0 + (-5.0) = -3.0
$$

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| `$2.0$` | `$2.0$` | `$0$` | `$10000$` | `$0000000000$` | `$0\ 10000\ 0000000000$` |
| `$-5.0$` | `$-5.0$` | `$1$` | `$10001$` | `$0100000000$` | `$1\ 10001\ 0100000000$` |

### Work

$$
2.0 = 1.0000000000_2 \times 2^1
$$

$$
-5.0 = -1.0100000000_2 \times 2^2
$$

Align `$2.0$` to exponent `$2$`:

$$
0.1000000000_2 \times 2^2 - 1.0100000000_2 \times 2^2
$$

Magnitude difference:

$$
1.0100000000_2 - 0.1000000000_2 = 0.1100000000_2
$$

Normalize:

$$
0.1100000000_2 \times 2^2 = 1.1000000000_2 \times 2^1
$$

Result sign is negative.

### GRS Bits

$$
G=0,\ R=0,\ S=0
$$

### Expected Output

| Decimal Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---:|---:|---:|---:|---|---|---|
| `$-3.0$` | `$1$` | `$10000$` | `$1000000000$` | `$1\ 10000\ 1000000000$` | OFF | OFF |

---

## Test Case 7: Rounding Down Because Less Than Half LSB

### Calculation

$$
1.0 + 2^{-12} = 1.000244140625
$$

The spacing near `$1.0$` is:

$$
2^{-10} = 0.0009765625
$$

Since:

$$
2^{-12} = 0.25 \times 2^{-10}
$$

the result rounds back down to `$1.0$`.

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| `$1.0$` | `$1.0$` | `$0$` | `$01111$` | `$0000000000$` | `$0\ 01111\ 0000000000$` |
| `$2^{-12}$` | `$0.000244140625$` | `$0$` | `$00011$` | `$0000000000$` | `$0\ 00011\ 0000000000$` |

### Work

Exponent difference:

$$
0 - (-12) = 12
$$

The smaller mantissa is shifted right by `$12$`.

### GRS Bits

After alignment relative to the `$1.0$` mantissa:

$$
G=0,\ R=1,\ S=0
$$

Since guard bit is `$0$`, no round-up occurs.

### Expected Output

| Exact Decimal Sum | Rounded Decimal Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---:|---:|---:|---:|---:|---|---|---|
| `$1.000244140625$` | `$1.0$` | `$0$` | `$01111$` | `$0000000000$` | `$0\ 01111\ 0000000000$` | OFF | OFF |

---

## Test Case 8: Tie Case, Round to Even

### Calculation

$$
1.0 + 2^{-11} = 1.00048828125
$$

This is exactly halfway between:

$$
1.0
$$

and

$$
1.0009765625
$$

Because the lower result has even LSB `$0$`, it stays at `$1.0$`.

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| `$1.0$` | `$1.0$` | `$0$` | `$01111$` | `$0000000000$` | `$0\ 01111\ 0000000000$` |
| `$2^{-11}$` | `$0.00048828125$` | `$0$` | `$00100$` | `$0000000000$` | `$0\ 00100\ 0000000000$` |

### GRS Bits

$$
G=1,\ R=0,\ S=0
$$

This is exactly half. Since the current fraction LSB is `$0$`, round down.

### Expected Output

| Exact Decimal Sum | Rounded Decimal Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---:|---:|---:|---:|---:|---|---|---|
| `$1.00048828125$` | `$1.0$` | `$0$` | `$01111$` | `$0000000000$` | `$0\ 01111\ 0000000000$` | OFF | OFF |

---

## Test Case 9: Tie Case, Round to Even Upward

### Calculation

$$
1.0009765625 + 2^{-11}
$$

The first number has fraction LSB `$1$`. Adding exactly half an LSB causes round-to-even to round upward.

Exact sum:

$$
1.00146484375
$$

Rounded result:

$$
1.001953125
$$

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| `$1.0009765625$` | `$1.0009765625$` | `$0$` | `$01111$` | `$0000000001$` | `$0\ 01111\ 0000000001$` |
| `$2^{-11}$` | `$0.00048828125$` | `$0$` | `$00100$` | `$0000000000$` | `$0\ 00100\ 0000000000$` |

### GRS Bits

$$
G=1,\ R=0,\ S=0
$$

Tie case. The stored fraction LSB before rounding is `$1$`, so round upward to make the result even.

### Expected Output

| Exact Decimal Sum | Rounded Decimal Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---:|---:|---:|---:|---:|---|---|---|
| `$1.00146484375$` | `$1.001953125$` | `$0$` | `$01111$` | `$0000000010$` | `$0\ 01111\ 0000000010$` | OFF | OFF |

---

## Test Case 10: Rounding Up Because Greater Than Half LSB

### Calculation

$$
16.0 + 0.01171875 = 16.01171875
$$

Near `$16.0$`, the LSB spacing is:

$$
2^{4-10} = 2^{-6} = 0.015625
$$

The smaller operand is:

$$
0.01171875 = 0.75 \times 0.015625
$$

So the result rounds upward by one LSB.

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| `$16.0$` | `$16.0$` | `$0$` | `$10011$` | `$0000000000$` | `$0\ 10011\ 0000000000$` |
| `$0.01171875$` | `$0.01171875$` | `$0$` | `$01000$` | `$1000000000$` | `$0\ 01000\ 1000000000$` |

### Work

$$
16.0 = 1.0000000000_2 \times 2^4
$$

$$
0.01171875 = 1.1000000000_2 \times 2^{-7}
$$

Exponent difference:

$$
4 - (-7) = 11
$$

The smaller mantissa is shifted right by `$11$`.

### GRS Bits

The shifted smaller operand contributes more than half an LSB:

$$
G=1,\ R=1,\ S=0
$$

Therefore, round up.

### Expected Output

| Exact Decimal Sum | Rounded Decimal Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---:|---:|---:|---:|---:|---|---|---|
| `$16.01171875$` | `$16.015625$` | `$0$` | `$10011$` | `$0000000001$` | `$0\ 10011\ 0000000001$` | OFF | OFF |

---

## Test Case 11: Rounding Causes Mantissa Carry and Renormalization

### Calculation

$$
1.9990234375 + 0.000732421875
$$

Exact sum:

$$
1.999755859375
$$

This rounds to:

$$
2.0
$$

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| `$1.9990234375$` | `$1.9990234375$` | `$0$` | `$01111$` | `$1111111111$` | `$0\ 01111\ 1111111111$` |
| `$0.000732421875$` | `$0.000732421875$` | `$0$` | `$00100$` | `$1000000000$` | `$0\ 00100\ 1000000000$` |

### Work

The first operand is the largest number below `$2.0$` at exponent `$0$`.

Adding `$0.000732421875$` is greater than half of one LSB near `$1.0$`, so the fraction rounds upward.

The fraction `$1111111111$` plus rounding increment causes a carry:

$$
1.1111111111_2 \rightarrow 10.0000000000_2
$$

Normalize:

$$
10.0000000000_2 \times 2^0 = 1.0000000000_2 \times 2^1
$$

### GRS Bits

$$
G=1,\ R=1,\ S=0
$$

### Expected Output

| Exact Decimal Sum | Rounded Decimal Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---:|---:|---:|---:|---:|---|---|---|
| `$1.999755859375$` | `$2.0$` | `$0$` | `$10000$` | `$0000000000$` | `$0\ 10000\ 0000000000$` | OFF | OFF |

---

## Test Case 12: Large Exponent Difference, Smaller Operand Lost

### Calculation

$$
1024.0 + 0.25 = 1024.25
$$

Near `$1024.0$`, the LSB spacing is:

$$
2^{10-10} = 1
$$

Since `$0.25$` is less than half an LSB, it is lost after rounding.

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| `$1024.0$` | `$1024.0$` | `$0$` | `$11001$` | `$0000000000$` | `$0\ 11001\ 0000000000$` |
| `$0.25$` | `$0.25$` | `$0$` | `$01101$` | `$0000000000$` | `$0\ 01101\ 0000000000$` |

### Work

Exponent difference:

$$
10 - (-2) = 12
$$

The smaller operand is shifted right by `$12$`.

### GRS Bits

$$
G=0,\ R=1,\ S=0
$$

No round-up.

### Expected Output

| Exact Decimal Sum | Rounded Decimal Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---:|---:|---:|---:|---:|---|---|---|
| `$1024.25$` | `$1024.0$` | `$0$` | `$11001$` | `$0000000000$` | `$0\ 11001\ 0000000000$` | OFF | OFF |

---

## Test Case 13: Sticky Bit Test

### Calculation

$$
1.0 + 2^{-13} = 1.0001220703125
$$

The small operand is far below half of one LSB near `$1.0$`, but it should still set the sticky bit after alignment.

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| `$1.0$` | `$1.0$` | `$0$` | `$01111$` | `$0000000000$` | `$0\ 01111\ 0000000000$` |
| `$2^{-13}$` | `$0.0001220703125$` | `$0$` | `$00010$` | `$0000000000$` | `$0\ 00010\ 0000000000$` |

### Work

Exponent difference:

$$
0 - (-13) = 13
$$

After shifting right by `$13$`, the contribution falls below guard and round positions.

### GRS Bits

$$
G=0,\ R=0,\ S=1
$$

Sticky bit must be ON, but guard is `$0$`, so no round-up.

### Expected Output

| Exact Decimal Sum | Rounded Decimal Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---:|---:|---:|---:|---:|---|---|---|
| `$1.0001220703125$` | `$1.0$` | `$0$` | `$01111$` | `$0000000000$` | `$0\ 01111\ 0000000000$` | OFF | OFF |

---

## Test Case 14: Overflow by Adding Two Maximum Positive Numbers

### Calculation

$$
65504 + 65504 = 131008
$$

This exceeds the maximum representable finite value.

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| Max positive | `$65504$` | `$0$` | `$11110$` | `$1111111111$` | `$0\ 11110\ 1111111111$` |
| Max positive | `$65504$` | `$0$` | `$11110$` | `$1111111111$` | `$0\ 11110\ 1111111111$` |

### Work

$$
1.1111111111_2 \times 2^{15} + 1.1111111111_2 \times 2^{15}
$$

$$
= 11.1111111110_2 \times 2^{15}
$$

Normalize:

$$
1.1111111111_2 \times 2^{16}
$$

The true exponent becomes `$16$`, so the biased exponent would be:

$$
16 + 15 = 31 = 11111_2
$$

Exponent `$11111$` is reserved for overflow.

### Expected Output

| Exact Decimal Sum | Output | Overflow | Underflow |
|---:|---|---|---|
| `$131008$` | Overflow condition | ON | OFF |

---

## Test Case 15: Overflow Caused by Rounding

### Calculation

$$
65504 + 32 = 65536
$$

The exact result requires exponent `$16$`, which is not representable as a finite normalized number.

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| Max positive | `$65504$` | `$0$` | `$11110$` | `$1111111111$` | `$0\ 11110\ 1111111111$` |
| `$32.0$` | `$32.0$` | `$0$` | `$10100$` | `$0000000000$` | `$0\ 10100\ 0000000000$` |

### Work

Near exponent `$15$`, the spacing between numbers is:

$$
2^{15-10} = 32
$$

Adding `$32$` to the largest finite number steps beyond the finite range.

### Expected Output

| Exact Decimal Sum | Output | Overflow | Underflow |
|---:|---|---|---|
| `$65536$` | Overflow condition | ON | OFF |

---

## Test Case 16: Underflow by Catastrophic Cancellation

### Calculation

Let:

$$
A = 1.0000000001_2 \times 2^{-5}
$$

and:

$$
B = 1.0000000000_2 \times 2^{-5}
$$

Then:

$$
A + (-B)
$$

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| `$A$` | `$0.031280517578125$` | `$0$` | `$01010$` | `$0000000001$` | `$0\ 01010\ 0000000001$` |
| `$-B$` | `$-0.03125$` | `$1$` | `$01010$` | `$0000000000$` | `$1\ 01010\ 0000000000$` |

### Work

$$
A - B = 0.0000000001_2 \times 2^{-5}
$$

Normalize by shifting left `$10$`:

$$
0.0000000001_2 \times 2^{-5}
=
1.0000000000_2 \times 2^{-15}
$$

Biased exponent:

$$
-15 + 15 = 0 = 00000_2
$$

Exponent `$00000$` is below the normalized range.

### Expected Output

| Exact Decimal Result | Output | Overflow | Underflow |
|---:|---|---|---|
| `$0.000030517578125$` | Underflow condition | OFF | ON |

---

## Test Case 17: Underflow at Minimum Normal Minus Half Minimum Normal

### Calculation

$$
2^{-14} + (-2^{-15}) = 2^{-15}
$$

The result is smaller than the minimum normalized value.

### Inputs

| Value | Decimal | Sign | Exponent | Fraction | Full 16-bit |
|---|---:|---:|---:|---:|---|
| `$2^{-14}$` | `$0.00006103515625$` | `$0$` | `$00001$` | `$0000000000$` | `$0\ 00001\ 0000000000$` |
| `$-2^{-15}$` | `$-0.000030517578125$` | `$1$` | `$00000$` or external test value | `$-$` | Not normalized |

### Note

Because `$2^{-15}$` cannot be represented as a normalized input in this format, this test is only useful if your simulator allows raw internal values or denormal-style testing.

### Expected Output

| Exact Decimal Result | Output | Overflow | Underflow |
|---:|---|---|---|
| `$0.000030517578125$` | Underflow condition | OFF | ON |

---

# Compact Test Vector Table

The following table is convenient for directly checking your circuit.

| Case | Input A | Decimal A | Input B | Decimal B | Expected Output | Decimal Output | Overflow | Underflow | GRS |
|---:|---|---:|---|---:|---|---:|---|---|---|
| 1 | `$0\ 01111\ 1000000000$` | `$1.5$` | `$0\ 01111\ 0100000000$` | `$1.25$` | `$0\ 10000\ 0110000000$` | `$2.75$` | OFF | OFF | `$000$` |
| 2 | `$0\ 10010\ 1000000000$` | `$12.0$` | `$0\ 01110\ 0000000000$` | `$0.5$` | `$0\ 10010\ 1001000000$` | `$12.5$` | OFF | OFF | `$000$` |
| 3 | `$0\ 10010\ 1110000000$` | `$15.0$` | `$0\ 01111\ 0000000000$` | `$1.0$` | `$0\ 10011\ 0000000000$` | `$16.0$` | OFF | OFF | `$000$` |
| 4 | `$0\ 10001\ 0110000000$` | `$5.5$` | `$1\ 10001\ 0110000000$` | `$-5.5$` | `$0\ 00000\ 0000000000$` | `$0.0$` | OFF | OFF | `$000$` |
| 5 | `$0\ 01111\ 1000000000$` | `$1.5$` | `$1\ 01111\ 0100000000$` | `$-1.25$` | `$0\ 01101\ 0000000000$` | `$0.25$` | OFF | OFF | `$000$` |
| 6 | `$0\ 10000\ 0000000000$` | `$2.0$` | `$1\ 10001\ 0100000000$` | `$-5.0$` | `$1\ 10000\ 1000000000$` | `$-3.0$` | OFF | OFF | `$000$` |
| 7 | `$0\ 01111\ 0000000000$` | `$1.0$` | `$0\ 00011\ 0000000000$` | `$0.000244140625$` | `$0\ 01111\ 0000000000$` | `$1.0$` | OFF | OFF | `$010$` |
| 8 | `$0\ 01111\ 0000000000$` | `$1.