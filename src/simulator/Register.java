package simulator;

public class Register {
	private int[] Register;
	private PIC PIC;
	private boolean EEPROM_Write_Sequence;

	public Register(PIC pic) {
		PIC = pic;

		Register = new int[1024];
		Reset();
	}

	public int Read(int pos) {
		return Register[GetAddress(pos)];
	}

	public void Write(int pos, int value) {
		int address = GetAddress(pos);
		int oldValue = Register[address];
		
		if(address == 0x88 && (value & 0x02) == 0x02 && !GetWREN()) {
			value = value & ~0x02;
		}
		
		DirectWrite(address, value);

		if (address == 0x01 || address == 0x81) {
			PIC.CyclesLeft = GetPrescale();
		} else if(address == 0x88 || address == 0x89) {
			if(address == 0x88 && GetRD()) {
				DirectWrite(0x08, PIC.EEPROM.Read(Register[0x09]));
				SetRD(false);
			}
			
			if(address == 0x89) {
				EEPROM_Write_Sequence = oldValue == 0x55 && value == 0xAA;
			}
			
			if(GetWR() && EEPROM_Write_Sequence) {
				PIC.EEPROM.Write(Register[0x09], Register[0x08]);
				SetWR(false);
				EEPROM_Write_Sequence = false;
				SetEEIF(true);
			}
		}
	}

	public void Reset() {
		for (int i = 0; i <= 0x8B; i++) {
			DirectWrite(i, 0);
			
			if(i == 0x2F) {
				i = 0x80;
			}
		}
		
		DirectWrite(0x03, 0x18);
		DirectWrite(0x81, 0xff);
		DirectWrite(0x85, 0x1f);
		DirectWrite(0x86, 0xff);
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

	public boolean GetC() {
		return GetBit(0x03, 0);
	}

	public void SetC(boolean c) {
		SetBit(0x03, 0, c);
	}

	public boolean GetDC() {
		return GetBit(0x03, 1);
	}

	public void SetDC(boolean dc) {
		SetBit(0x03, 1, dc);
	}

	public boolean GetZ() {
		return GetBit(0x03, 2);
	}

	public void SetZ(boolean z) {
		SetBit(0x03, 2, z);
	}

	public int GetBank() {
		return (Register[3] & 0x60) >> 5;
	}

	public boolean GetTO() {
		return !GetBit(0x03, 4);
	}

	public void SetTO(boolean to) {
		SetBit(0x03, 4, !to);
	}

	public boolean GetPD() {
		return !GetBit(0x03, 3);
	}

	public void SetPD(boolean pd) {
		SetBit(0x03, 3, !pd);
	}

	public boolean GetPSA() {
		return GetBit(0x81, 3);
	}

	public int GetPrescale() {
		int prescale = 0x01 << (Register[0x81] & 0x07);

		if (!GetPSA()) {
			prescale = prescale << 1;
		}

		return prescale;
	}

	public void ClearPrescale() {
		DirectWrite(0x81, Register[0x81] & 0xf8);
	}

	public boolean GetClockSource() {
		return GetBit(0x81, 4);
	}

	public boolean GetINTEDG() {
		return GetBit(0x81, 6);
	}

	public void IncrementTMR0() {
		int oldTMR0 = GetTMR0();

		DirectWrite(1, PIC.Calculate("incf", oldTMR0));

		if (GetGIE() && GetT0IE() && GetTMR0() < oldTMR0) {
			SetT0IF(true);
			PIC.Interrupt();
		}
	}

	public int GetTMR0() {
		return Register[1];
	}

	public void SetT0IF(boolean t0if) {
		SetBit(0x0B, 2, t0if);
	}

	public boolean GetT0IF() {
		return GetBit(0x0B, 2);
	}

	public void SetT0IE(boolean t0ie) {
		SetBit(0x0B, 5, t0ie);
	}

	public boolean GetT0IE() {
		return (Register[0x0B] & 0x20) == 0x20;
	}

	public void SetGIE(boolean gie) {
		SetBit(0x0B, 7, gie);
	}

	public boolean GetGIE() {
		return GetBit(0x0B, 7);
	}

	public boolean GetINTE() {
		return GetBit(0x0B, 4);
	}

	public void SetINTF(boolean intf) {
		SetBit(0x0B, 1, intf);
	}

	public boolean GetINTF() {
		return GetBit(0x0B, 1);
	}

	public boolean GetRBIE() {
		return GetBit(0x0B, 3);
	}

	public void SetRBIE(boolean rbie) {
		SetBit(0x0B, 3, rbie);
	}

	public boolean GetRBIF() {
		return GetBit(0x0B, 0);
	}

	public void SetRBIF(boolean rbif) {
		SetBit(0x0B, 0, rbif);
	}

	public boolean GetPortA(int port) {
		return GetBit(0x05, port);
	}
	
	public void SetPortA(int port, boolean set) {
		SetBit(0x05, port, set);
	}
	
	public boolean GetTRISA(int port) {
		return GetBit(0x85, port);
	}

	public boolean GetPortB(int port) {
		return GetBit(0x06, port);
	}
	
	public void SetPortB(int port, boolean set) {
		SetBit(0x06, port, set);
	}
	
	public boolean GetTRISB(int port) {
		return GetBit(0x86, port);
	}
	
	public void SetRD(boolean rd) {
		SetBit(0x88, 0, rd);
	}
	
	public boolean GetRD() {
		return GetBit(0x88, 0);
	}
	
	public void SetWR(boolean wr) {
		SetBit(0x88, 1, wr);
	}
	
	public boolean GetWR() {
		return GetBit(0x88, 1);
	}
	
	public boolean GetWREN() {
		return GetBit(0x88, 2);
	}
	
	public void SetWRERR(boolean wrerr) {
		SetBit(0x88, 3, wrerr);
	}
	
	public void SetEEIF(boolean eeif) {
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

	private void DirectWrite(int pos, int value) {
		if(Register[pos] == value) {
			return;
		}
		
		Register[pos] = value;

		if (PIC.Reg_Listener != null) {
			PIC.Reg_Listener.accept(pos, value);
		}
	}
}
