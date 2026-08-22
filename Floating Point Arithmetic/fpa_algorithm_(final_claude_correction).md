# 16-Bit Floating Point Adder — Complete Algorithm

> **Format:** IEEE 754 Half-Precision–like  
> 1 Sign bit (bit 15) | 5 Exponent bits [14:10] (Bias = 15) | 10 Fraction bits [9:0]  
> Implicit leading 1 for normalized numbers → significand = `1.fraction`

---

## Overview — Pipeline Stages

```
Input A, B (16 bits each)
      │
      ▼
┌─────────────────────┐
│  1. Unpack Fields    │
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│  2. Exponent Compare │  ← exponent_compare subcircuit
└─────────┬───────────┘
          ▼
┌─────────────────────────┐
│  3. Align Significands   │  ← align_significands subcircuit
└─────────┬───────────────┘
          ▼
┌─────────────────────────┐
│  4. Add / Subtract       │  ← add_sub_sig subcircuit
│     Significands         │
└─────────┬───────────────┘
          ▼
┌─────────────────────────┐
│  5. Normalize, Round,    │  ← normalized subcircuit
│     Detect Over/Underflow│
└─────────┬───────────────┘
          ▼
┌─────────────────────────┐
│  6. Assemble Output      │  ← FP_adder_main final wiring
└─────────────────────────┘
```

---

## Stage 1 — Unpack Input Fields

```
GIVEN:  A[15:0], B[15:0]                    (16-bit FP numbers)

  sign_A       ← A[15]                      (1 bit)
  exponent_A   ← A[14:10]                   (5 bits, biased)
  fraction_A   ← A[9:0]                     (10 bits, raw mantissa)

  sign_B       ← B[15]                      (1 bit)
  exponent_B   ← B[14:10]                   (5 bits, biased)
  fraction_B   ← B[9:0]                     (10 bits, raw mantissa)
```

### 1a. Prepend the Implicit Leading Bit

The circuit prepends a constant `1` to each 10-bit fraction to form an 11-bit significand:

```
  sig_A[10:0]  ← { 1, fraction_A[9:0] }    (11 bits → "1.fraction_A")
  sig_B[10:0]  ← { 1, fraction_B[9:0] }    (11 bits → "1.fraction_B")
```

> [!NOTE]
> This circuit always prepends `1` (the constant 1 pin in FP_adder_main). It does **not** check for subnormal inputs (exponent = `00000`), so subnormal inputs are treated as if they were normalized with an implicit `1` — a known simplification for this assignment.

---

## Stage 2 — Exponent Compare (`exponent_compare` subcircuit)

**Purpose:** Determine which operand has the larger exponent, compute the shift amount, and select the larger exponent as the working exponent.

```
INPUTS:  exponent_A[4:0], exponent_B[4:0]

STEP 2.1 — Subtract exponents
  diff[4:0]      ← exponent_A − exponent_B      (5-bit subtractor)
  borrow_out     ← borrow from the subtraction

STEP 2.2 — Determine which is smaller
  a_is_smaller   ← borrow_out
                    (= 1 if exponent_A < exponent_B, else 0)

STEP 2.3 — Select the larger exponent (MUX controlled by borrow)
  IF a_is_smaller = 0:
      current_big_exponent ← exponent_A          (A's exponent is ≥ B's)
  ELSE:
      current_big_exponent ← exponent_B          (B's exponent is larger)

STEP 2.4 — Compute absolute difference (MUX controlled by borrow)
  IF a_is_smaller = 0:
      abs_difference ← diff                      (diff is already positive)
  ELSE:
      abs_difference ← 0 − diff                  (via subtractor: 0 − diff)
      (This is equivalent to exponent_B − exponent_A when B > A)

OUTPUTS:
  current_big_exponent[4:0]     → the larger of the two exponents
  a_is_smaller                  → 1-bit flag: 1 if A's exponent is smaller
  abs_difference[4:0]           → |exponent_A − exponent_B| = shift amount
```

---

## Stage 3 — Align Significands (`align_significands` subcircuit)

**Purpose:** Shift the significand of the operand with the smaller exponent to the right by `abs_difference` positions, so that both significands are aligned to the same exponent value. The circuit also captures the bits shifted out for rounding purposes.

```
INPUTS:
  sig_A[10:0], sig_B[10:0]       (11-bit significands with implicit 1)
  a_is_smaller                   (from Stage 2)
  abs_difference[4:0]            (shift amount, from Stage 2)

STEP 3.1 — Zero-extend significands to 22 bits for safe shifting
  extended_sig_A[21:0] ← { 00000000000, sig_A[10:0] }    (zero-pad upper 11 bits)
  extended_sig_B[21:0] ← { 00000000000, sig_B[10:0] }    (zero-pad upper 11 bits)

STEP 3.2 — Shift the SMALLER operand's significand right
  ┌ The right-shifter shifts by abs_difference positions,
  │ filling vacated MSBs with zeros.
  │
  │ Both sig_A and sig_B are fed through identical right-shifters.
  │ Two MUXes (controlled by a_is_smaller) select which one passes
  │ through shifted and which passes through unshifted.
  └

  shifted_A[21:0] ← extended_sig_A >> abs_difference
  shifted_B[21:0] ← extended_sig_B >> abs_difference

  IF a_is_smaller = 0:
      aligned_input_to_split ← extended_sig_A      (A is NOT shifted — larger exp)
      aligned_other_to_split ← shifted_B            (B IS shifted — smaller exp)
  ELSE:
      aligned_input_to_split ← shifted_A            (A IS shifted — smaller exp)
      aligned_other_to_split ← extended_sig_B       (B is NOT shifted — larger exp)

STEP 3.3 — Extract aligned significand + GRS bits (14 bits each)
  ┌ The 22-bit shifted result is split as follows:
  │   Bits [21:11] → overflow/guard region (upper portion)
  │   Bits [10:1]  → main aligned significand (10 bits)
  │   Bit  [0]     → rounding region (sticky approximation)
  │
  │ But the circuit packs them into a 14-bit output bus:
  │   [13]    = carry / overflow bit (from the shifted bits region)
  │   [12:3]  = 10-bit aligned significand
  │   [2:0]   = guard bits captured from shifted-out bits
  │
  │ The circuit also OR-gates the 9 bits shifted furthest right
  │ into a "sticky" bit to detect if ANY of them were non-zero.
  └

  For each aligned significand (A and B):
    aligned_sig[13:0]:
      Bit [13]   ← 0 (reserved for carry in subsequent addition)
      Bit [12]   ← overflow/guard bit from shift (OR of upper bits)
      Bits[11:2] ← 10 main significand bits
      Bit [1]    ← guard bit (first bit shifted out)
      Bit [0]    ← sticky bit (OR of remaining shifted-out bits)

OUTPUTS:
  aligned_sig_A[13:0]     → 14-bit aligned significand of A (with GRS bits)
  aligned_sig_B[13:0]     → 14-bit aligned significand of B (with GRS bits)
```

> [!IMPORTANT]
> The right-shift by `abs_difference` is what makes the exponents equal. After alignment, both significands are referenced to `current_big_exponent`. Bits shifted beyond the 14-bit window contribute to the sticky bit via a 9-input OR gate visible in the circuit.

---

## Stage 4 — Add or Subtract Significands (`add_sub_sig` subcircuit)

**Purpose:** Determine the effective operation (add or subtract), compute the result significand, determine the result sign, and produce the raw output for normalization.

```
INPUTS:
  sign_A, sign_B                           (1-bit signs of the operands)
  aligned_sig_A[13:0], aligned_sig_B[13:0] (14-bit aligned significands)

STEP 4.1 — Determine the effective operation
  operation_sign ← sign_A XOR sign_B
     (= 0 → same sign → ADDITION)
     (= 1 → different signs → SUBTRACTION)

STEP 4.2 — Compare aligned significands to find the larger one
  ┌ A 14-bit unsigned comparator determines which aligned
  │ significand is larger in magnitude.
  └
  a_sig_less_than_b ← (aligned_sig_A < aligned_sig_B) ?  (unsigned compare)

STEP 4.3 — Route operands: larger goes to minuend position
  ┌ Two MUXes (controlled by a_sig_less_than_b) ensure:
  │   larger_sig  = the one with greater magnitude
  │   smaller_sig = the one with lesser magnitude
  └
  IF a_sig_less_than_b = 0:
      larger_sig[13:0]  ← aligned_sig_A
      smaller_sig[13:0] ← aligned_sig_B
  ELSE:
      larger_sig[13:0]  ← aligned_sig_B
      smaller_sig[13:0] ← aligned_sig_A

STEP 4.4 — Determine the result sign
  ┌ A MUX (controlled by a_sig_less_than_b) picks the sign
  │ of whichever operand has the larger significand.
  │ The text annotation says "picks the larger significand's sign".
  └
  IF a_sig_less_than_b = 0:
      result_sign ← sign_A
  ELSE:
      result_sign ← sign_B

STEP 4.5 — Perform the arithmetic
  ┌ Both an adder and a subtractor operate in parallel:
  │   sum_result[13:0]  ← larger_sig + smaller_sig     (14-bit adder)
  │   diff_result[13:0] ← larger_sig − smaller_sig     (14-bit subtractor)
  │
  │ The adder also produces a carry_out bit.
  └

  sum_result[13:0]  ← larger_sig + smaller_sig
  carry_out_add     ← carry from the 14-bit adder
  diff_result[13:0] ← larger_sig − smaller_sig

STEP 4.6 — Select addition or subtraction result (MUX on operation_sign)
  IF operation_sign = 0:                     (same signs → add)
      result_sig[13:0] ← sum_result
      carry_out        ← carry_out_add
  ELSE:                                      (different signs → subtract)
      result_sig[13:0] ← diff_result
      carry_out        ← 0                   (subtraction of larger − smaller has no borrow)

STEP 4.7 — Build the 15-bit ALU Fraction output
  ┌ The result_sig (14 bits) and carry_out (1 bit) are packed
  │ into a 15-bit bus for the normalizer:
  │   alu_fraction_in[14:0] = { carry_out, result_sig[13:0] }
  │
  │ Additionally, a 15-bit comparator checks if alu_fraction_in > 0
  │ to detect the zero-result case (its "less than" output is
  │ inverted via a NOT gate to produce an "is not zero" signal).
  └

  alu_fraction_in[14:0] ← { carry_out, result_sig[13:0] }

OUTPUTS:
  result_sign             → sign bit of the result
  operation_sign          → 0 = addition was performed, 1 = subtraction
  result_sig[13:0]        → 14-bit raw result significand
  alu_fraction_in[14:0]   → 15-bit value (carry + result) for normalizer
  carry_out               → 1-bit carry from addition
```

---

## Stage 5 — Normalize, Round, and Detect Overflow/Underflow (`normalized` subcircuit)

This is the most complex stage. It handles:
- Right-shift normalization (when addition produces a carry)
- Left-shift normalization (when subtraction produces leading zeros)
- Subnormal result detection
- Overflow and underflow flag generation
- Zero result handling
- Final sign determination

```
INPUTS:
  alu_fraction_in[14:0]        (15-bit: carry_out as bit[14], result[13:0])
  current_exponent[4:0]        (the larger exponent from Stage 2)
  result_sign                  (from Stage 4)

──────────────────────────────────────────────────────────
STEP 5.1 — Split the ALU fraction input
──────────────────────────────────────────────────────────

  carry_bit      ← alu_fraction_in[14]        (1 bit — overflow from addition)
  fraction_14[13:0] ← alu_fraction_in[13:0]   (14-bit working significand)

  shifted_fraction[13:0] ← alu_fraction_in[14:1]
      (= alu_fraction_in right-shifted by 1 → equivalent to "carry + upper 13 bits")
      This is the result of a 1-bit right normalization shift.

──────────────────────────────────────────────────────────
STEP 5.2 — Check if result is zero
──────────────────────────────────────────────────────────

  ┌ OR all 15 bits of alu_fraction_in together:
  │   is_nonzero ← OR(alu_fraction_in[14], alu_fraction_in[13], ..., alu_fraction_in[0])
  │
  │ The 15 individual bits are fanned out through a 15-input OR gate.
  └
  is_nonzero ← OR(all 15 bits of alu_fraction_in)

  IF is_nonzero = 0:
      → Result is exactly zero.
      → The final result_sign is forced to 0 (positive zero).
         (This is done via an AND gate: final_sign ← result_sign AND is_nonzero)
      → Exponent and fraction are both zero.

──────────────────────────────────────────────────────────
STEP 5.3 — Right-shift normalization (carry_bit = 1)
──────────────────────────────────────────────────────────
  ┌ If the addition produced a carry (carry_bit = 1), the significand
  │ has the form 1x.xxxxx... and must be right-shifted by 1 to become
  │ 1.xxxxxxx..., and the exponent must be incremented by 1.
  └

  right_shift_exponent[4:0] ← current_exponent + 1     (5-bit adder, carry_bit as addend)
      Actually implemented as:  current_exponent + zero_extend(carry_bit, 5)

  ┌ A MUX selects between:
  │   carry_bit = 0 → use fraction_14 as-is, use current_exponent
  │   carry_bit = 1 → use shifted_fraction (right-shifted by 1), use right_shift_exponent
  └

  IF carry_bit = 1:
      working_fraction ← shifted_fraction     (right-shifted by 1)
      working_exponent ← right_shift_exponent (current_exponent + 1)
  ELSE:
      working_fraction ← fraction_14          (no shift needed)
      working_exponent ← current_exponent

──────────────────────────────────────────────────────────
STEP 5.4 — Left-shift normalization (for subtraction results with leading zeros)
──────────────────────────────────────────────────────────
  ┌ When subtraction produces a result like 0.001xxxxx, the significand
  │ needs to be left-shifted until the MSB is 1 (i.e., format = 1.xxxxx),
  │ and the exponent decremented by the shift amount.
  │
  │ A Priority Encoder finds the position of the highest set bit in
  │ the 14-bit fraction. The 14 individual bits are fanned out from
  │ a 14-way splitter and fed into a 14-to-4 priority encoder.
  └

  ┌ The 14-bit working_fraction is split into individual bits [0..13]
  │ and fed into the priority encoder inputs.
  │ A constant 0 is also fed as a dummy extra input.
  │
  │ The priority encoder outputs:
  │   highest_set_position[3:0]  → position of the highest '1' bit (0-13)
  │   group_signal               → 1 if any input bit is set
  └

  highest_set_position[3:0] ← PriorityEncoder(working_fraction[13:0])
  any_bit_set               ← group_signal from encoder

  ┌ The required left-shift amount is:
  │   For a properly normalized number, the MSB should be at position 13
  │   (bit index 13 of the 14-bit field).
  │   left_shift_amount = 13 − highest_set_position
  │
  │ This is implemented as a 4-bit subtractor:
  │   left_shift_amount[3:0] ← 13 (0xD) − highest_set_position[3:0]
  └

  left_shift_amount[3:0] ← 13 − highest_set_position

  ┌ The 14-bit left-shifter shifts working_fraction left:
  └
  left_shifted_fraction[13:0] ← working_fraction << left_shift_amount

  ┌ The exponent must be decremented by the shift amount.
  │ The shift amount is zero-extended from 4 bits to 5 bits, then:
  │   left_shift_exponent[4:0] ← working_exponent − zero_extend(left_shift_amount, 5)
  └

  left_shift_exponent[4:0] ← working_exponent − left_shift_amount (zero-extended to 5 bits)

──────────────────────────────────────────────────────────
STEP 5.5 — Select between right-shifted and left-shifted results
──────────────────────────────────────────────────────────
  ┌ If carry_bit was 1, we already did right-shift normalization and
  │ the result is in working_fraction / working_exponent.
  │
  │ If carry_bit was 0 AND the MSB of the fraction is not at position 13,
  │ we need left-shift normalization.
  │
  │ A MUX (controlled by carry_bit) selects the appropriate path:
  └

  IF carry_bit = 1:
      ┌ Right-shift path was taken. The fraction after right-shift and the
      │ incremented exponent are used.
      │ A second MUX checks if a 1-bit right-shift was needed (via carry_bit).
      │ The right-shifted fraction = alu_fraction_in[14:1].
      │ The exponent = current_exponent + 1.
      └
      normalized_fraction ← shifted_fraction     (alu_fraction_in[14:1])
      normalized_exponent ← right_shift_exponent (current_exponent + 1)
  ELSE:
      normalized_fraction ← left_shifted_fraction
      normalized_exponent ← left_shift_exponent

──────────────────────────────────────────────────────────
STEP 5.6 — Overflow Detection
──────────────────────────────────────────────────────────
  ┌ Overflow occurs when the normalized exponent reaches or exceeds
  │ the maximum biased exponent value 31 (11111₂), which is reserved
  │ for infinity/NaN.
  │
  │ The circuit checks this by:
  │   1. Splitting the 5-bit normalized_exponent into [3:0] and [4].
  │   2. The exponent overflows if the subtracted result went negative
  │      (via the carry/borrow of the adder that computed exponent + 1),
  │      OR if the final exponent ≥ 31.
  │
  │ Implementation:
  │   A 5-bit comparator compares normalized_exponent with 0x1F (= 31).
  │   overflow_raw ← (normalized_exponent ≥ 31)
  │
  │   Additionally, if right_shift_exponent produced a carry (bit 4 of
  │   the 5-bit adder overflowed), that also signals overflow.
  │
  │   overflow_flag ← overflow_from_comparator OR carry_overflow_from_adder
  └

  overflow_flag ← (normalized_exponent ≥ 31) OR (exponent_addition_carry)

──────────────────────────────────────────────────────────
STEP 5.7 — Underflow Detection
──────────────────────────────────────────────────────────
  ┌ Underflow occurs when the normalized exponent drops to 0 or below,
  │ meaning the result is too small to be represented as a normalized
  │ number.
  │
  │ The circuit checks:
  │   1. Compare the left_shift_amount with the current_exponent.
  │      If left_shift_amount ≥ current_exponent, the exponent would
  │      go to 0 or negative → underflow.
  │
  │   2. A 5-bit comparator: left_shift_exponent ≤ 0
  │      Equivalently: (working_exponent − shift_amount) produces a borrow.
  │
  │   The underflow is detected by the comparator checking if the
  │   normalized exponent equals 0 (00000₂), combined with the
  │   subtraction borrow.
  │
  │   underflow_raw ← (left_shift_amount ≥ working_exponent)
  │   underflow_flag ← underflow_raw AND (carry_bit = 0)
  │       (underflow only matters on the left-shift path, not right-shift)
  └

  underflow_flag ← (shift_amount ≥ current_exponent) AND NOT carry_bit

──────────────────────────────────────────────────────────
STEP 5.8 — Handle overflow and underflow in final exponent
──────────────────────────────────────────────────────────

  ┌ If overflow OR underflow is detected, the exponent is clamped:
  │
  │   A combined flag: exception_flag ← overflow OR underflow
  │
  │   MUX on exception_flag:
  │     IF exception_flag = 0:
  │         final_exponent ← normalized_exponent   (normal case)
  │     ELSE:
  │         IF overflow:
  │             final_exponent ← 11111₂ (= 31)    (infinity encoding)
  │         IF underflow:
  │             final_exponent ← 00000₂ (= 0)     (zero/subnormal encoding)
  │
  │   Actually, the circuit uses a simpler approach:
  │     MUX selects between normalized_exponent and 00000₂,
  │     controlled by the combined overflow/underflow flag.
  └

  IF (overflow_flag OR underflow_flag) = 1:
      final_exponent ← 00000₂
  ELSE:
      final_exponent ← normalized_exponent

──────────────────────────────────────────────────────────
STEP 5.9 — Handle overflow and underflow in final fraction
──────────────────────────────────────────────────────────

  ┌ Similarly, if overflow/underflow, the fraction output is zeroed out.
  │
  │ A MUX (controlled by exception flag) selects:
  │   Normal: normalized_fraction
  │   Exception: 0x0000 (14 bits of zero)
  │
  │ Then a second MUX handles the carry_bit case:
  │   If carry_bit was set and no exception, use right-shifted fraction.
  └

  IF (overflow_flag OR underflow_flag) = 1:
      final_fraction ← 00000000000000₂      (14 bits of zeros)
  ELSE:
      final_fraction ← normalized_fraction

──────────────────────────────────────────────────────────
STEP 5.10 — Final sign determination (zero result handling)
──────────────────────────────────────────────────────────

  ┌ The result sign goes through a final gate to handle the zero case:
  │
  │   The OR of all alu_fraction_in bits (is_nonzero from Step 5.2)
  │   is NOTted and ANDed with result_sign:
  │
  │   Actually: final_sign ← result_sign AND is_nonzero
  │
  │   When is_nonzero = 0 (result is zero), final_sign = 0 (positive zero).
  │   When is_nonzero = 1, final_sign = result_sign (preserves sign).
  │
  │ A MUX further handles whether the underflow/overflow case should
  │ force the sign:
  │   If underflow: sign is preserved from result_sign.
  │   If overflow: sign is preserved from result_sign.
  └

  IF is_nonzero = 0:
      final_sign ← 0           (positive zero)
  ELSE:
      final_sign ← result_sign

OUTPUTS:
  Normalized_Fraction[13:0]  → 14-bit normalized fraction (only [12:3] are the 10-bit mantissa)
  Normalized_Exponent[4:0]   → 5-bit final exponent
  underflow                  → 1-bit underflow flag
  overflow                   → 1-bit overflow flag
```

---

## Stage 6 — Assemble Final 16-bit Output (`FP_adder_main` wiring)

```
INPUTS (from normalized subcircuit and add_sub_sig):
  Normalized_Fraction[13:0]
  Normalized_Exponent[4:0]
  result_sign (from add_sub_sig, routed through the main circuit)
  underflow_flag, overflow_flag

──────────────────────────────────────────────────────────
STEP 6.1 — Additional rounding (post-normalization)
──────────────────────────────────────────────────────────
  ┌ In the FP_adder_main circuit, after receiving the normalized outputs,
  │ there is additional rounding logic:
  │
  │ The 14-bit Normalized_Fraction is split via a splitter:
  │   Bits [3:0]  → guard/round/sticky region (4 bits: carry, G, R, S)
  │   Bit  [3]    → carry from normalization
  │   Bits [12:3] → the actual 10-bit significand
  │   Bit  [13]   → overflow bit
  │
  │ Rounding decision circuit:
  │   The 4-bit lower portion is examined with an OR gate (3 inputs):
  │     round_bit  ← fraction[1]    (Guard bit)
  │     sticky_bit ← fraction[0]    (Sticky bit)
  │     carry_bit  ← fraction[3]    (related to carry status)
  │
  │     should_round ← OR(round_bit, sticky_bit, carry_bit)
  │     round_up     ← should_round AND fraction[2]
  │       (fraction[2] is the round bit — round up if round bit=1
  │        AND at least one of guard/sticky is non-zero,
  │        implementing round-to-nearest-even-like behavior)
  │
  │   If round_up = 1, increment the 10-bit fraction by 1.
  │   If the increment causes a carry out of the fraction,
  │   increment the exponent by 1.
  └

  lower_4_bits   ← Normalized_Fraction[3:0]
  mantissa_10    ← Normalized_Fraction[12:3]       (10 bits for the output)
  
  or_of_grs      ← lower_4_bits[0] OR lower_4_bits[1] OR lower_4_bits[3]
  round_up       ← or_of_grs AND lower_4_bits[2]

  ┌ Two adders in the main circuit perform conditional rounding:
  │   10-bit adder:  mantissa_10 + 0  (with carry_in = round_up)
  │    5-bit adder:  Normalized_Exponent + 0  (with carry_in from fraction overflow)
  └

  rounded_mantissa[9:0] ← mantissa_10 + round_up
  mantissa_carry        ← carry from the 10-bit addition

  rounded_exponent[4:0] ← Normalized_Exponent + mantissa_carry

──────────────────────────────────────────────────────────
STEP 6.2 — Pack the final 16-bit output
──────────────────────────────────────────────────────────

  final_output[15:0]:
      Bit  [15]     ← result_sign        (sign bit from add_sub_sig,
                                           routed through normalized)
      Bits [14:10]  ← rounded_exponent   (5-bit exponent after rounding)
      Bits [9:0]    ← rounded_mantissa   (10-bit fraction after rounding)

  ┌ The splitter at the output combines these three fields into
  │ a single 16-bit bus connected to the "finaloutput" output pin:
  │   Bits  [9:0]   ← rounded_mantissa  (split group 0)
  │   Bits [14:10]  ← rounded_exponent  (split group 1)
  │   Bit  [15]     ← result_sign       (split group 2)
  └

  finaloutput[15:0] ← { result_sign, rounded_exponent[4:0], rounded_mantissa[9:0] }
```

---

## Summary of Special Case Handling

| Condition | How Detected | How Handled |
|:----------|:-------------|:------------|
| **Both operands identical sign** | `sign_A XOR sign_B = 0` | Significands are **added** |
| **Different signs** | `sign_A XOR sign_B = 1` | Significands are **subtracted** (larger − smaller) |
| **Addition carry-out** | `carry_bit = 1` (bit 14 of alu_fraction_in) | Right-shift fraction by 1, increment exponent by 1 |
| **Subtraction leading zeros** | Priority encoder finds highest set bit < position 13 | Left-shift fraction by `(13 − position)`, decrement exponent by that amount |
| **Result is exactly zero** | 15-input OR of alu_fraction_in = 0 | Output `+0.0` → sign forced to 0, exponent = 0, fraction = 0 |
| **Overflow** | Normalized exponent ≥ 31 (biased) or exponent adder carry | `overflow_flag = 1`; exponent and fraction may be clamped to zero in output |
| **Underflow** | Left-shift amount ≥ current exponent (exponent would go ≤ 0) | `underflow_flag = 1`; exponent and fraction may be clamped to zero in output |
| **Subnormal inputs** | **NOT explicitly handled** — implicit `1` always prepended | Subnormal inputs (exponent = `00000`) are treated as if normalized with implicit 1 (a design simplification) |
| **Infinity / NaN inputs** | **NOT explicitly handled** — no special-case detection | Infinity (`11111` exponent) and NaN are processed through the normal pipeline |
| **Rounding** | Guard/Round/Sticky bits captured from alignment shift | Round-up triggered when `Round AND (Guard OR Sticky OR carry)` is true; post-normalization rounding via 10-bit adder |

---

## Detailed Bit-Width Summary

| Signal | Width | Description |
|:-------|:------|:------------|
| A, B (inputs) | 16 bits | `{sign[15], exp[14:10], frac[9:0]}` |
| sign_A, sign_B | 1 bit | Sign bits |
| exponent_A, exponent_B | 5 bits | Biased exponents |
| fraction_A, fraction_B | 10 bits | Raw mantissa (no implicit bit) |
| sig_A, sig_B | 11 bits | `{1, fraction}` — with implicit leading 1 |
| extended significand | 22 bits | Zero-extended for shifting |
| aligned_sig_A, aligned_sig_B | 14 bits | After alignment: `{overflow, 11-bit sig, guard, sticky}` |
| result_sig | 14 bits | After add/subtract |
| alu_fraction_in | 15 bits | `{carry_out, result_sig[13:0]}` |
| Normalized_Fraction | 14 bits | After normalization |
| Normalized_Exponent | 5 bits | After normalization |
| rounded_mantissa | 10 bits | After rounding |
| rounded_exponent | 5 bits | After rounding |
| finaloutput | 16 bits | `{sign, exponent, mantissa}` |

---

## Worked Example Trace: $12.0 + 0.5 = 12.5$

```
INPUT:
  A = 0_10010_1000000000₂ = 12.0    (sign=0, exp=18, frac=1000000000)
  B = 0_01110_0000000000₂ = 0.5     (sign=0, exp=14, frac=0000000000)

STAGE 1 — Unpack:
  sign_A=0, exp_A=10010₂=18, sig_A = 1_1000000000₂ (11 bits)
  sign_B=0, exp_B=01110₂=14, sig_B = 1_0000000000₂ (11 bits)

STAGE 2 — Exponent Compare:
  diff = 18 − 14 = 4, borrow = 0
  a_is_smaller = 0 (A has bigger exponent)
  current_big_exponent = 18 (10010₂)
  abs_difference = 4

STAGE 3 — Align:
  B is shifted right by 4:
  sig_B extended = 00000000000_10000000000₂ (22 bits)
  After >> 4:    = 00000000000_00001000000₂ (22 bits)
  aligned_sig_A = 00_1_1000000000_00₂ (14 bits, no shift)
  aligned_sig_B = 00_0_0001000000_00₂ (14 bits, shifted)

STAGE 4 — Add/Subtract:
  operation_sign = 0 XOR 0 = 0 (addition)
  result_sig = 1_1000000000_00 + 0_0001000000_00 = 1_1001000000_00
  carry_out = 0
  result_sign = 0
  alu_fraction_in = 0_1_1001000000_00₂ (15 bits)

STAGE 5 — Normalize:
  carry_bit = 0 → no right shift needed
  Priority encoder: MSB at position 12 (bit 12 of 14-bit field)
  left_shift_amount = 13 − 12 = 1... 
  BUT bit 12 IS the proper position for our format → shift = 0
  normalized_fraction = 1_1001000000_00
  normalized_exponent = 18

STAGE 6 — Assemble:
  No rounding needed (GRS = 000)
  mantissa = 1001000000₂
  exponent = 10010₂
  sign = 0

  OUTPUT = 0_10010_1001000000₂ = 12.5  ✓
```
