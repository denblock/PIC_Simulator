package simulator;
public class Register {
	private int[] Register;
	private PIC PIC;

	public Register(int size, PIC pic) {
		Register = new int[size];
		Register[0x02] = 0;
		Register[0x03] = 0x18;
		Register[0x81] = 0xff;
		Register[0x85] = 0x1f;
		Register[0x86] = 0xff;

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
		Register[0x81] = 0xff;
		Register[0x85] = 0x1f;
		Register[0x86] = 0xff;
	}
	
	public int[] GetRegister() {
		return Register;
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
		SetBit(0x81, 3, !pd);
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
		Register[0x81] = Register[0x81] & 0xf8;
	}

	public boolean GetClockSource() {
		return GetBit(0x81, 4);
	}
	
	public boolean GetINTEDG() {
		return GetBit(0x81, 6);
	}

	public void IncrementTMR0() {
		int oldTMR0 = GetTMR0();
		
		Register[1] = PIC.Calculate("incf", Register[1]);
		
		if(GetGIE() && GetT0IE() && GetTMR0() < oldTMR0) {
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
	
	public boolean GetTRISB(int port) {
		return GetBit(0x86, port);
	}
	
	public boolean GetPortB(int port) {
		return GetBit(0x06, port);
	}
	
	private void SetBit(int address, int bit, boolean set) {
		if(set) {
			Register[address] = Register[address] | (1 << bit);
		} else {
			Register[address] = Register[address] & ~(1 << bit);
		}
	}
	
	private boolean GetBit(int address, int bit) {
		int b = 1 << bit;
		
		return (Register[address] & b) == b;
	}
}
