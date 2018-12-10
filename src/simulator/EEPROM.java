package simulator;

public class EEPROM {
	PIC PIC;
	private int[] Data;

	public EEPROM(PIC pic) {
		PIC = pic;
		Data = new int[64];
	}

	public void Write(int address, int value) {
		if (Data[address] == value) {
			return;
		}

		Data[address] = value;

		if (PIC.EEPROM_Listener != null) {
			PIC.EEPROM_Listener.accept(address, value);
		}
	}

	public int Read(int address) {
		return Data[address];
	}

	public void Reset() {
		for (int i = 0; i < 64; i++) {
			Write(i, 0);
		}
	}
}
