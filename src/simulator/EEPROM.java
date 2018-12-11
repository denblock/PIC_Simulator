package simulator;

class EEPROM {
	private PIC PIC;
	private int[] Data;

	EEPROM(PIC pic) {
		PIC = pic;
		Data = new int[64];
	}

	void Write(int address, int value) {
		if (Data[address] == value) {
			return;
		}

		Data[address] = value;

		if (PIC.EEPROM_Listener != null) {
			PIC.EEPROM_Listener.accept(address, value);
		}
	}

	int Read(int address) {
		return Data[address];
	}

	void Reset() {
		for (int i = 0; i < 64; i++) {
			Write(i, 0);
		}
	}
}
