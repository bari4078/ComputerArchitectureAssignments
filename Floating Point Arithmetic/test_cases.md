# Comprehensive Floating-Point Adder Test Cases (16-bit)

> **Verification note:** every calculation below — bit encodings, alignment shifts, GRS bits, rounding decisions, and overflow/underflow conclusions — was independently re-derived and checked. All 17 test cases are numerically correct as stated.

---

## Format Assumptions

- **16 bits total:** 1 sign bit `S`, 5 exponent bits `E`, 10 fraction bits `F`
- **Bias:** 15
- **Normalized value:**

$$
(-1)^S \times (1.F)_2 \times 2^{E-15}
$$

- Exponent `00000` → **underflow** (not a valid normalized result)
- Exponent `11111` → **overflow** (not a finite normalized result)
- **Rounding mode:** round to nearest, ties to even
- `G, R, S` = guard, round, sticky bits (the three bits immediately following the 10 stored fraction bits after alignment)

### Reference Values

| Quantity | Decimal | Binary Encoding |
|---|---|---|
| Minimum positive normalized value | `1.0₂ × 2⁻¹⁴ = 0.00006103515625` | `0 00001 0000000000` |
| Maximum positive finite value | `1.1111111111₂ × 2¹⁵ = 65504` | `0 11110 1111111111` |

---

## Test Case 1 — Simple Addition, Same Exponent

**Calculation:** `1.5 + 1.25 = 2.75`

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| 1.5 | 0 | 01111 | 1000000000 | `0 01111 1000000000` |
| 1.25 | 0 | 01111 | 0100000000 | `0 01111 0100000000` |

**Work:**
$$
1.1000000000_2 \times 2^0 + 1.0100000000_2 \times 2^0 = 10.1100000000_2 \times 2^0
$$
Normalize: $10.1100000000_2 \times 2^0 = 1.0110000000_2 \times 2^1$

**GRS:** `G=0, R=0, S=0` (no bits shifted out)

**Expected Output**

| Decimal | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---|---|---|---|---|---|---|
| 2.75 | 0 | 10000 | 0110000000 | `0 10000 0110000000` | OFF | OFF |

---

## Test Case 2 — Exponent Alignment, No Rounding

**Calculation:** `12.0 + 0.5 = 12.5`

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| 12.0 | 0 | 10010 | 1000000000 | `0 10010 1000000000` |
| 0.5 | 0 | 01110 | 0000000000 | `0 01110 0000000000` |

**Work:** $12.0 = 1.1000000000_2 \times 2^3$, $0.5 = 1.0000000000_2 \times 2^{-1}$

Exponent difference $= 3-(-1) = 4$. Shift 0.5's mantissa right by 4:
$$
1.1000000000_2 + 0.0001000000_2 = 1.1001000000_2
$$

**GRS:** `G=0, R=0, S=0` (all shifted-out bits are zero)

**Expected Output**

| Decimal | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---|---|---|---|---|---|---|
| 12.5 | 0 | 10010 | 1001000000 | `0 10010 1001000000` | OFF | OFF |

---

## Test Case 3 — Addition Requiring Normalization

**Calculation:** `15.0 + 1.0 = 16.0`

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| 15.0 | 0 | 10010 | 1110000000 | `0 10010 1110000000` |
| 1.0 | 0 | 01111 | 0000000000 | `0 01111 0000000000` |

**Work:** $15.0 = 1.1110000000_2 \times 2^3$, $1.0 = 1.0000000000_2 \times 2^0$

Shift 1.0 right by 3: $1.1110000000_2 + 0.0010000000_2 = 10.0000000000_2$
Normalize: $10.0000000000_2 \times 2^3 = 1.0000000000_2 \times 2^4$

**GRS:** `G=0, R=0, S=0`

**Expected Output**

| Decimal | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---|---|---|---|---|---|---|
| 16.0 | 0 | 10011 | 0000000000 | `0 10011 0000000000` | OFF | OFF |

---

## Test Case 4 — Exact Cancellation to Zero

**Calculation:** `5.5 + (-5.5) = 0`

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| 5.5 | 0 | 10001 | 0110000000 | `0 10001 0110000000` |
| −5.5 | 1 | 10001 | 0110000000 | `1 10001 0110000000` |

**Work:** $1.0110000000_2 \times 2^2 - 1.0110000000_2 \times 2^2 = 0$

**GRS:** `G=0, R=0, S=0`

**Expected Output** (exact zero, sign convention: positive zero)

| Decimal | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---|---|---|---|---|---|---|
| 0.0 | 0 | 00000 | 0000000000 | `0 00000 0000000000` | OFF | OFF |

---

## Test Case 5 — Subtraction Requiring Left Normalization

**Calculation:** `1.5 + (-1.25) = 0.25`

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| 1.5 | 0 | 01111 | 1000000000 | `0 01111 1000000000` |
| −1.25 | 1 | 01111 | 0100000000 | `1 01111 0100000000` |

**Work:** $1.1000000000_2 \times 2^0 - 1.0100000000_2 \times 2^0 = 0.0100000000_2 \times 2^0$

Normalize by shifting left 2: $0.0100000000_2 \times 2^0 = 1.0000000000_2 \times 2^{-2}$
Biased exponent: $-2+15 = 13 = 01101_2$

**GRS:** `G=0, R=0, S=0`

**Expected Output**

| Decimal | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---|---|---|---|---|---|---|
| 0.25 | 0 | 01101 | 0000000000 | `0 01101 0000000000` | OFF | OFF |

---

## Test Case 6 — Negative Result

**Calculation:** `2.0 + (-5.0) = -3.0`

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| 2.0 | 0 | 10000 | 0000000000 | `0 10000 0000000000` |
| −5.0 | 1 | 10001 | 0100000000 | `1 10001 0100000000` |

**Work:** Align 2.0 to exponent 2: $0.1000000000_2 \times 2^2$. Subtract from $1.0100000000_2 \times 2^2$:
$$
1.0100000000_2 - 0.1000000000_2 = 0.1100000000_2
$$
Normalize: $0.1100000000_2 \times 2^2 = 1.1000000000_2 \times 2^1$. Result sign is negative.

**GRS:** `G=0, R=0, S=0`

**Expected Output**

| Decimal | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---|---|---|---|---|---|---|
| −3.0 | 1 | 10000 | 1000000000 | `1 10000 1000000000` | OFF | OFF |

---

## Test Case 7 — Rounding Down (< half LSB)

**Calculation:** $1.0 + 2^{-12} = 1.000244140625$

LSB spacing near 1.0 is $2^{-10} = 0.0009765625$. Since $2^{-12} = 0.25 \times 2^{-10}$, the result rounds back down to 1.0.

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| 1.0 | 0 | 01111 | 0000000000 | `0 01111 0000000000` |
| $2^{-12}$ (0.000244140625) | 0 | 00011 | 0000000000 | `0 00011 0000000000` |

**Work:** Exponent difference $= 0-(-12)=12$; smaller mantissa shifted right 12. Its leading 1 lands exactly on the **round** bit position.

**GRS:** `G=0, R=1, S=0` → guard bit is 0, so no round-up.

**Expected Output**

| Exact Sum | Rounded Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---|---|---|---|---|---|---|---|
| 1.000244140625 | 1.0 | 0 | 01111 | 0000000000 | `0 01111 0000000000` | OFF | OFF |

---

## Test Case 8 — Tie, Round to Even (stays down)

**Calculation:** $1.0 + 2^{-11} = 1.00048828125$ — exactly halfway between 1.0 and 1.0009765625. Since the lower result has even LSB (0), it stays at 1.0.

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| 1.0 | 0 | 01111 | 0000000000 | `0 01111 0000000000` |
| $2^{-11}$ (0.00048828125) | 0 | 00100 | 0000000000 | `0 00100 0000000000` |

**Work:** Exponent difference = 11; smaller mantissa's leading 1 lands exactly on the **guard** bit.

**GRS:** `G=1, R=0, S=0` (exact tie). Current fraction LSB is 0 (even) → round down.

**Expected Output**

| Exact Sum | Rounded Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---|---|---|---|---|---|---|---|
| 1.00048828125 | 1.0 | 0 | 01111 | 0000000000 | `0 01111 0000000000` | OFF | OFF |

---

## Test Case 9 — Tie, Round to Even (rounds up)

**Calculation:** $1.0009765625 + 2^{-11}$. First operand's fraction LSB is 1. Adding exactly half an LSB rounds up to make the result even.
Exact sum: $1.00146484375$ → rounded: $1.001953125$

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| 1.0009765625 | 0 | 01111 | 0000000001 | `0 01111 0000000001` |
| $2^{-11}$ (0.00048828125) | 0 | 00100 | 0000000000 | `0 00100 0000000000` |

**GRS:** `G=1, R=0, S=0` (exact tie). Stored fraction LSB before rounding is 1 (odd) → round up to even.

**Expected Output**

| Exact Sum | Rounded Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---|---|---|---|---|---|---|---|
| 1.00146484375 | 1.001953125 | 0 | 01111 | 0000000010 | `0 01111 0000000010` | OFF | OFF |

---

## Test Case 10 — Rounding Up (> half LSB)

**Calculation:** $16.0 + 0.01171875 = 16.01171875$. Near 16.0, LSB spacing is $2^{-6}=0.015625$; the smaller operand is $0.75$ of one LSB, so the result rounds up.

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| 16.0 | 0 | 10011 | 0000000000 | `0 10011 0000000000` |
| 0.01171875 | 0 | 01000 | 1000000000 | `0 01000 1000000000` |

**Work:** $16.0=1.0000000000_2\times2^4$, $0.01171875=1.1000000000_2\times2^{-7}$. Exponent difference = 11.

**GRS:** `G=1, R=1, S=0` → round up.

**Expected Output**

| Exact Sum | Rounded Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---|---|---|---|---|---|---|---|
| 16.01171875 | 16.015625 | 0 | 10011 | 0000000001 | `0 10011 0000000001` | OFF | OFF |

---

## Test Case 11 — Rounding Causes Mantissa Carry / Renormalization

**Calculation:** $1.9990234375 + 0.000732421875$ → exact sum $1.999755859375$ → rounds to $2.0$

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| 1.9990234375 | 0 | 01111 | 1111111111 | `0 01111 1111111111` |
| 0.000732421875 | 0 | 00100 | 1000000000 | `0 00100 1000000000` |

**Work:** The first operand is the largest value below 2.0 at exponent 0. The addition rounds the fraction up; `1111111111 + 1` carries: $1.1111111111_2 \rightarrow 10.0000000000_2$. Normalize: $10.0000000000_2 \times 2^0 = 1.0000000000_2 \times 2^1$.

**GRS:** `G=1, R=1, S=0`

**Expected Output**

| Exact Sum | Rounded Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---|---|---|---|---|---|---|---|
| 1.999755859375 | 2.0 | 0 | 10000 | 0000000000 | `0 10000 0000000000` | OFF | OFF |

---

## Test Case 12 — Large Exponent Difference, Smaller Operand Lost

**Calculation:** $1024.0 + 0.25 = 1024.25$. Near 1024.0 the LSB spacing is $2^{0}=1$; 0.25 is less than half an LSB, so it's lost after rounding.

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| 1024.0 | 0 | 11001 | 0000000000 | `0 11001 0000000000` |
| 0.25 | 0 | 01101 | 0000000000 | `0 01101 0000000000` |

**Work:** Exponent difference $= 10-(-2) = 12$; smaller operand's single 1-bit lands exactly on the **round** bit.

**GRS:** `G=0, R=1, S=0` → no round-up.

**Expected Output**

| Exact Sum | Rounded Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---|---|---|---|---|---|---|---|
| 1024.25 | 1024.0 | 0 | 11001 | 0000000000 | `0 11001 0000000000` | OFF | OFF |

---

## Test Case 13 — Sticky Bit Test

**Calculation:** $1.0 + 2^{-13} = 1.0001220703125$. The small operand falls below both guard and round positions but must still set the sticky bit.

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| 1.0 | 0 | 01111 | 0000000000 | `0 01111 0000000000` |
| $2^{-13}$ (0.0001220703125) | 0 | 00010 | 0000000000 | `0 00010 0000000000` |

**Work:** Exponent difference = 13; the shifted-in bit lands beyond the round position.

**GRS:** `G=0, R=0, S=1` → guard is 0, so no round-up (sticky alone doesn't force rounding here).

**Expected Output**

| Exact Sum | Rounded Result | Sign | Exponent | Fraction | Full 16-bit | Overflow | Underflow |
|---|---|---|---|---|---|---|---|
| 1.0001220703125 | 1.0 | 0 | 01111 | 0000000000 | `0 01111 0000000000` | OFF | OFF |

---

## Test Case 14 — Overflow: Max + Max

**Calculation:** `65504 + 65504 = 131008`

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| Max positive (65504) | 0 | 11110 | 1111111111 | `0 11110 1111111111` |
| Max positive (65504) | 0 | 11110 | 1111111111 | `0 11110 1111111111` |

**Work:** $1.1111111111_2\times2^{15} + 1.1111111111_2\times2^{15} = 1.1111111111_2\times2^{16}$ (doubling ⇒ exponent +1). True exponent 16 → biased $16+15=31=11111_2$, which is the reserved overflow code.

**Expected Output**

| Exact Sum | Output | Overflow | Underflow |
|---|---|---|---|
| 131008 | Overflow condition | ON | OFF |

---

## Test Case 15 — Overflow Caused by Rounding/Carry

**Calculation:** `65504 + 32 = 65536`

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| Max positive (65504) | 0 | 11110 | 1111111111 | `0 11110 1111111111` |
| 32.0 | 0 | 10100 | 0000000000 | `0 10100 0000000000` |

**Work:** Exponent difference $=15-5=10$; the smaller operand's single 1-bit lands exactly on the fraction LSB position (position 10), causing an exact addable carry chain: `1111111111 + 0000000001 = 10000000000`. Renormalizing bumps the true exponent to 16 → biased 31 = `11111`, the reserved overflow code. (65504 + 32 = 65536 = $2^{16}$, outside the finite range.)

**Expected Output**

| Exact Sum | Output | Overflow | Underflow |
|---|---|---|---|
| 65536 | Overflow condition | ON | OFF |

---

## Test Case 16 — Underflow via Catastrophic Cancellation

**Calculation:** Let $A = 1.0000000001_2 \times 2^{-5}$ and $B = 1.0000000000_2 \times 2^{-5}$. Compute $A + (-B)$.

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| A (0.031280517578125) | 0 | 01010 | 0000000001 | `0 01010 0000000001` |
| −B (−0.03125) | 1 | 01010 | 0000000000 | `1 01010 0000000000` |

**Work:** $A-B = 0.0000000001_2 \times 2^{-5}$. Normalize by shifting left 10: $1.0000000000_2 \times 2^{-15}$. Biased exponent $= -15+15 = 0 = 00000_2$ — below the normalized range, i.e. underflow.

**Expected Output**

| Exact Result | Output | Overflow | Underflow |
|---|---|---|---|
| 0.000030517578125 | Underflow condition | OFF | ON |

---

## Test Case 17 — Underflow at the Minimum-Normal Boundary

**Calculation:** $2^{-14} + (-2^{-15}) = 2^{-15}$ — smaller than the minimum normalized value.

| Value | Sign | Exponent | Fraction | Full 16-bit |
|---|---|---|---|---|
| $2^{-14}$ (0.00006103515625) | 0 | 00001 | 0000000000 | `0 00001 0000000000` |
| $-2^{-15}$ (−0.000030517578125) | 1 | — | — | not representable as a normalized input |

> **Caveat (carried over from the source material):** $2^{-15}$ cannot itself be encoded as a normalized value in this format, so this case only applies if your testbench can inject raw/denormal-style internal values rather than only normalized inputs.

**Expected Output**

| Exact Result | Output | Overflow | Underflow |
|---|---|---|---|
| 0.000030517578125 | Underflow condition | OFF | ON |

---

# Compact Test Vector Table

| # | Input A | Decimal A | Input B | Decimal B | Expected Output | Decimal Out | Ovf | Unf | GRS |
|--:|---|---:|---|---:|---|---:|:-:|:-:|:-:|
| 1 | `0 01111 1000000000` | 1.5 | `0 01111 0100000000` | 1.25 | `0 10000 0110000000` | 2.75 | OFF | OFF | 000 |
| 2 | `0 10010 1000000000` | 12.0 | `0 01110 0000000000` | 0.5 | `0 10010 1001000000` | 12.5 | OFF | OFF | 000 |
| 3 | `0 10010 1110000000` | 15.0 | `0 01111 0000000000` | 1.0 | `0 10011 0000000000` | 16.0 | OFF | OFF | 000 |
| 4 | `0 10001 0110000000` | 5.5 | `1 10001 0110000000` | −5.5 | `0 00000 0000000000` | 0.0 | OFF | OFF | 000 |
| 5 | `0 01111 1000000000` | 1.5 | `1 01111 0100000000` | −1.25 | `0 01101 0000000000` | 0.25 | OFF | OFF | 000 |
| 6 | `0 10000 0000000000` | 2.0 | `1 10001 0100000000` | −5.0 | `1 10000 1000000000` | −3.0 | OFF | OFF | 000 |
| 7 | `0 01111 0000000000` | 1.0 | `0 00011 0000000000` | 0.000244140625 | `0 01111 0000000000` | 1.0 | OFF | OFF | 010 |
| 8 | `0 01111 0000000000` | 1.0 | `0 00100 0000000000` | 0.00048828125 | `0 01111 0000000000` | 1.0 | OFF | OFF | 100 |
| 9 | `0 01111 0000000001` | 1.0009765625 | `0 00100 0000000000` | 0.00048828125 | `0 01111 0000000010` | 1.001953125 | OFF | OFF | 100 |
| 10 | `0 10011 0000000000` | 16.0 | `0 01000 1000000000` | 0.01171875 | `0 10011 0000000001` | 16.015625 | OFF | OFF | 110 |
| 11 | `0 01111 1111111111` | 1.9990234375 | `0 00100 1000000000` | 0.000732421875 | `0 10000 0000000000` | 2.0 | OFF | OFF | 110 |
| 12 | `0 11001 0000000000` | 1024.0 | `0 01101 0000000000` | 0.25 | `0 11001 0000000000` | 1024.0 | OFF | OFF | 010 |
| 13 | `0 01111 0000000000` | 1.0 | `0 00010 0000000000` | 0.0001220703125 | `0 01111 0000000000` | 1.0 | OFF | OFF | 001 |
| 14 | `0 11110 1111111111` | 65504 | `0 11110 1111111111` | 65504 | Overflow | 131008 | ON | OFF | — |
| 15 | `0 11110 1111111111` | 65504 | `0 10100 0000000000` | 32.0 | Overflow | 65536 | ON | OFF | — |
| 16 | `0 01010 0000000001` | 0.031280517578125 | `1 01010 0000000000` | −0.03125 | Underflow | 0.000030517578125 | OFF | ON | — |
| 17 | `0 00001 0000000000` | 0.00006103515625 | not representable | −0.000030517578125 | Underflow | 0.000030517578125 | OFF | ON | — |

---

## Verification Summary

Every test case was re-derived independently:
- **Encodings** — all sign/exponent/fraction fields correctly represent the stated decimal values under the 1-5-10 format with bias 15.
- **Alignment shifts and GRS bits** — recomputed bit-by-bit for each case (positions 11/12/13+ relative to the aligned binary point); all match the stated `G, R, S` values.
- **Rounding decisions** — each round-up/round-down/round-to-even call is consistent with round-to-nearest-ties-to-even given the derived GRS bits.
- **Carry/renormalization** — cases 11, 14, and 15 (mantissa carry out of the top bit) were checked to correctly bump the exponent by 1.
- **Overflow/underflow** — cases 14–17 correctly land on biased exponent 31 (`11111`) or 0 (`00000`) respectively.

No numerical errors were found in the original test set. One structural issue was fixed: the original **compact test vector table cut off after row 8** — it's completed here through row 17.