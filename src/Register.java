public class Register {
	private int[] Register;
	private PIC PIC;

	public Register(int size, PIC pic) {
		Register = new int[size];
		Register[0x03] = 0x18;
		Register[0x81] = 0xff;

		PIC = pic;
	}

	public int Read(int pos) {
		return Register[GetAddress(pos)];
	}

	public void Write(int pos, int value) {
		int address = GetAddress(pos);
		Register[address] = value;

		if (address == 1 || address == 0x81) {
			PIC.ResetCyclesLeft();
		}
	}

	public void Reset() {
		Register[2] = 0;
		Register[3] = Register[3] & 0x1f;
	}

	private int GetAddress(int pos) {
		int idx = pos;

		if (pos == 0) {
			idx = Register[4];
		}

		if (GetBank() == 1) {
			idx += 0x80;
		}

		if (idx == 0x83) {
			idx = 0x03;
		}

		return idx;
	}

	public boolean GetC() {
		return (Register[3] & 0x01) == 0x01;
	}

	public void SetC(boolean c) {
		if (c) {
			Register[3] = Register[3] | 0x01;
		} else {
			Register[3] = Register[3] & ~0x01;
		}
	}

	public boolean GetDC() {
		return (Register[3] & 0x02) == 0x02;
	}

	public void SetDC(boolean dc) {
		if (dc) {
			Register[3] = Register[3] | 0x02;
		} else {
			Register[3] = Register[3] & ~0x02;
		}
	}

	public boolean GetZ() {
		return (Register[3] & 0x04) == 0x04;
	}

	public void SetZ(boolean z) {
		if (z) {
			Register[3] = Register[3] | 0x04;
		} else {
			Register[3] = Register[3] & ~0x04;
		}
	}

	public int GetBank() {
		return (Register[3] & 0x60) >> 5;
	}

	public boolean GetTO() {
		return (Register[3] & 0x10) != 0x10;
	}

	public void SetTO(boolean to) {
		if (to) {
			Register[3] = Register[3] & ~0x10;
		} else {
			Register[3] = Register[3] | 0x10;
		}
	}

	public boolean GetPD() {
		return (Register[3] & 0x08) != 0x08;
	}
	
	public void SetPD(boolean pd) {
		if (pd) {
			Register[3] = Register[3] & ~0x08;
		} else {
			Register[3] = Register[3] | 0x08;
		}
	}

	public boolean GetPSA() {
		return (Register[0x81] & 0x08) == 0x01;
	}

	public int GetPrescale() {
		int prescale = 0x01 << (Register[0x81] & 0x07);

		if (!GetPSA()) {
			prescale = prescale << 1;
		}

		return prescale;
	}

	public boolean GetClockSource() {
		return (Register[0x81] & 0x10) == 0x10;
	}

	public void IncrementTMR0() {
		Register[1] = PIC.Calculate("incf", Register[1]);
	}

	public int GetTMR0() {
		return Register[1];
	}
}
