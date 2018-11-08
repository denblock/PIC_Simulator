public class Register {
	private int[] register;

	public Register(int size) {
		register = new int[size];
	}

	public int Read(int pos) {
		return register[GetAddress(pos)];
	}

	public void Write(int pos, int value) {
		register[GetAddress(pos)] = value;
	}
	
	private int GetAddress(int pos) {
		int idx = pos;

		if (pos == 0) {
			idx = register[4];
		}
		
		if(GetBank() == 1) {
			idx += 0x80;
		}
		
		return idx;
	}
	
	public boolean GetC() {
		return (register[3] & 0x01) == 0x01;
	}
	
	public void SetC(boolean c) {
		if(c) {
			register[3] = register[3] | 0x01;
		} else {
			register[3] = register[3] & ~0x01;
		}
	}

	public boolean GetDC() {
		return (register[3] & 0x02) == 0x02;
	}
	
	public void SetDC(boolean dc) {
		if(dc) {
			register[3] = register[3] | 0x02;
		} else {
			register[3] = register[3] & ~0x02;
		}
	}

	public boolean GetZ() {
		return (register[3] & 0x04) == 0x04;
	}
	
	public void SetZ(boolean z) {
		if(z) {
			register[3] = register[3] | 0x04;
		} else {
			register[3] = register[3] & ~0x04;
		}
	}
	
	public int GetBank() {
		return (register[3] & 0x7f) >> 5;
	}
}
