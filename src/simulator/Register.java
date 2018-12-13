package simulator;

class Register {
	private int[] Register;
	private PIC PIC;
	private boolean EEPROM_Write_Sequence;
	int EEPROM_Time;

	Register(PIC pic) {
		PIC = pic;

		Register = new int[1024];
		Reset();
	}

	int Read(int pos) {
		return Register[GetAddress(pos)];
	}

	void Write(int pos, int value) {
		int address = GetAddress(pos);
		int oldValue = Register[address];

		if (address == 0x88 && (value & 0x02) == 0x02 && !GetWREN()) {
			value = value & ~0x02;
		}

		DirectWrite(address, value);

		if (address == 0x01 || address == 0x81) {
			PIC.CyclesLeft = GetPrescale();
		} else if (address == 0x88 || address == 0x89) {
			if (address == 0x88 && GetRD()) {
				DirectWrite(0x08, PIC.EEPROM.Read(Register[0x09]));
				SetRD(false);
			}

			if (address == 0x89) {
				EEPROM_Write_Sequence = oldValue == 0x55 && value == 0xAA;
			}

			if (GetWR() && EEPROM_Write_Sequence) {
				PIC.EEPROM.Write(Register[0x09], Register[0x08]);
				SetWR(false);
				EEPROM_Write_Sequence = false;
			}
		} else if (address == 0x02) {
			PIC.SetPC(Register[0x02] | ((Register[0x0A] & 0x1F) << 8));
		}
	}

	void Reset() {
		for (int i = 0x0C; i <= 0x2F; i++) {
			DirectWrite(i, 0);
		}

		DirectWrite(0x02, 0);
		DirectWrite(0x03, Register[0x03] | 0x18);
		DirectWrite(0x0A, 0);
		DirectWrite(0x0B, Register[0x0B] & 0x01);
		DirectWrite(0x81, 0xFF);
		DirectWrite(0x85, 0x1F);
		DirectWrite(0x86, 0xFF);
		DirectWrite(0x88, Register[0x88] & 0x08);
		
		EEPROM_Time = 1000;
		EEPROM_Write_Sequence = false;
	}

	private int GetAddress(int pos) {
		int idx = pos;

		if (pos == 0) {
			idx = Register[4];
		}

		if (GetBank() == 1) {
			idx += 0x80;
		}

		if (idx == 0x82 || idx == 0x83 || idx == 0x84 || idx == 0x8A || idx == 0x8B || (idx >= 0x8C && idx <= 0xAF)) {
			idx -= 0x80;
		}

		return idx;
	}

	boolean GetC() {
		return GetBit(0x03, 0);
	}

	void SetC(boolean c) {
		SetBit(0x03, 0, c);
	}

	void SetDC(boolean dc) {
		SetBit(0x03, 1, dc);
	}

	void SetZ(boolean z) {
		SetBit(0x03, 2, z);
	}

	int GetBank() {
		return (Register[3] & 0x60) >> 5;
	}

	boolean GetTO() {
		return !GetBit(0x03, 4);
	}

	void SetTO(boolean to) {
		SetBit(0x03, 4, !to);
	}

	boolean GetPD() {
		return !GetBit(0x03, 3);
	}

	void SetPD(boolean pd) {
		SetBit(0x03, 3, !pd);
	}

	boolean GetPSA() {
		return GetBit(0x81, 3);
	}

	int GetPrescale() {
		int prescale = 0x01 << (Register[0x81] & 0x07);

		if (!GetPSA()) {
			prescale = prescale << 1;
		}

		return prescale;
	}

	void ClearPrescale() {
		DirectWrite(0x81, Register[0x81] & 0xf8);
	}

	boolean GetClockSource() {
		return GetBit(0x81, 4);
	}

	boolean GetINTEDG() {
		return GetBit(0x81, 6);
	}

	void IncrementTMR0() {
		int oldTMR0 = GetTMR0();

		DirectWrite(1, PIC.Calculate("incf", oldTMR0));

		if (GetGIE() && GetT0IE() && GetTMR0() < oldTMR0) {
			SetT0IF(true);
			PIC.Interrupt();
		}
	}

	int GetTMR0() {
		return Register[1];
	}

	void SetT0IF(boolean t0if) {
		SetBit(0x0B, 2, t0if);
	}

	boolean GetT0IE() {
		return (Register[0x0B] & 0x20) == 0x20;
	}

	void SetGIE(boolean gie) {
		SetBit(0x0B, 7, gie);
	}

	boolean GetGIE() {
		return GetBit(0x0B, 7);
	}

	boolean GetINTE() {
		return GetBit(0x0B, 4);
	}

	void SetINTF(boolean intf) {
		SetBit(0x0B, 1, intf);
	}

	boolean GetRBIE() {
		return GetBit(0x0B, 3);
	}

	void SetRBIF(boolean rbif) {
		SetBit(0x0B, 0, rbif);
	}

	boolean GetPortA(int port) {
		return GetBit(0x05, port);
	}

	void SetPortA(int port, boolean set) {
		SetBit(0x05, port, set);
	}

	boolean GetPortB(int port) {
		return GetBit(0x06, port);
	}

	void SetPortB(int port, boolean set) {
		SetBit(0x06, port, set);
	}

	void SetRD(boolean rd) {
		SetBit(0x88, 0, rd);
	}

	boolean GetRD() {
		return GetBit(0x88, 0);
	}

	void SetWR(boolean wr) {
		SetBit(0x88, 1, wr);
	}

	boolean GetWR() {
		return GetBit(0x88, 1);
	}

	boolean GetWREN() {
		return GetBit(0x88, 2);
	}

	void SetWRERR(boolean wrerr) {
		SetBit(0x88, 3, wrerr);
	}

	void SetEEIF(boolean eeif) {
		SetBit(0x88, 4, eeif);
	}

	private void SetBit(int address, int bit, boolean set) {
		if (set) {
			DirectWrite(address, Register[address] | (1 << bit));
		} else {
			DirectWrite(address, Register[address] & ~(1 << bit));
		}
	}

	private boolean GetBit(int address, int bit) {
		int b = 1 << bit;

		return (Register[address] & b) == b;
	}

	void DirectWrite(int pos, int value) {
		if (Register[pos] == value) {
			return;
		}

		Register[pos] = value;

		if (PIC.Reg_Listener != null) {
			PIC.Reg_Listener.accept(pos, value);
		}
	}
}
