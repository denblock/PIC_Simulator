package simulator;

public class Instruction {
	private String Name;
	private int[] Args;
	private int Cycles;

	public Instruction(int instruction) {
		if (instruction == 0xFFFF) {
			return;
		}

		Args = new int[0];
		Cycles = 1;

		int opCode = instruction & 0x3FFF;

		switch (opCode) {
		case 0x0000:
		case 0x0020:
		case 0x0040:
		case 0x0060:
			Name = "nop";
			return;
		case 0x0064:
			Name = "clrwdt";
			return;
		case 0x0009:
			Name = "retfie";
			Cycles = 2;
			return;
		case 0x0008:
			Name = "return";
			Cycles = 2;
			return;
		case 0x0063:
			Name = "sleep";
			return;
		default:
			break;
		}

		opCode = (instruction & 0x3FFF) >> 7;
		int arg1 = instruction & 0x07FF; // 00 0kkk kkkk kkkk
		int arg2 = instruction & 0x00FF; // 00 0000 kkkk kkkk
		int arg3 = (instruction & 0x0380) >> 7; // 00 00bb b000 0000
		int arg4 = instruction & 0x007F; // 00 0000 0fff ffff
		int arg5 = (instruction & 0x0080) >> 7; // 00 0000 d000 0000

		switch (opCode) {
		case 0x02:
			Name = "clrw";
			return;
		case 0x03:
			Name = "clrf";
			Args = new int[] { arg4 };
			return;
		default:
			break;
		}

		opCode = instruction & 0x3F00;

		switch (opCode) {
		// BYTE-ORIENTED FILE REGISTER OPERATIONS
		case 0x0700:
			Name = "addwf";
			Args = new int[] { arg4, arg5 };
			return;
		case 0x0500:
			Name = "andwf";
			Args = new int[] { arg4, arg5 };
			return;
		case 0x0900:
			Name = "comf";
			Args = new int[] { arg4, arg5 };
			return;
		case 0x0300:
			Name = "decf";
			Args = new int[] { arg4, arg5 };
			return;
		case 0x0B00:
			Name = "decfsz";
			Args = new int[] { arg4, arg5 };
			return;
		case 0x0A00:
			Name = "incf";
			Args = new int[] { arg4, arg5 };
			return;
		case 0x0F00:
			Name = "incfsz";
			Args = new int[] { arg4, arg5 };
			return;
		case 0x0400:
			Name = "iorwf";
			Args = new int[] { arg4, arg5 };
			return;
		case 0x0800:
			Name = "movf";
			Args = new int[] { arg4, arg5 };
			return;
		case 0x0000:
			Name = "movwf";
			Args = new int[] { arg4 };
			return;
		case 0x0D00:
			Name = "rlf";
			Args = new int[] { arg4, arg5 };
			return;
		case 0x0C00:
			Name = "rrf";
			Args = new int[] { arg4, arg5 };
			return;
		case 0x0200:
			Name = "subwf";
			Args = new int[] { arg4, arg5 };
			return;
		case 0x0E00:
			Name = "swapf";
			Args = new int[] { arg4, arg5 };
			return;
		case 0x0600:
			Name = "xorwf";
			Args = new int[] { arg4, arg5 };
			return;

		// LITERAL AND CONTOL OPERATIONS
		case 0x3E00:
		case 0x3F00:
			Name = "addlw";
			Args = new int[] { arg2 };
			return;
		case 0x3900:
			Name = "andlw";
			Args = new int[] { arg2 };
			return;
		case 0x3800:
			Name = "iorlw";
			Args = new int[] { arg2 };
			return;
		case 0x3000:
		case 0x3100:
		case 0x3200:
		case 0x3300:
			Name = "movlw";
			Args = new int[] { arg2 };
			return;
		case 0x3400:
		case 0x3500:
		case 0x3600:
		case 0x3700:
			Name = "retlw";
			Args = new int[] { arg2 };
			Cycles = 2;
			return;
		case 0x3C00:
		case 0x3D00:
			Name = "sublw";
			Args = new int[] { arg2 };
			return;
		case 0x3A00:
			Name = "xorlw";
			Args = new int[] { arg2 };
			return;
		default:
			break;
		}

		opCode = instruction & 0x3C00;

		switch (opCode) {
		case 0x1000:
			Name = "bcf";
			Args = new int[] { arg4, arg3 };
			return;
		case 0x1400:
			Name = "bsf";
			Args = new int[] { arg4, arg3 };
			return;
		case 0x1800:
			Name = "btfsc";
			Args = new int[] { arg4, arg3 };
			return;
		case 0x1C00:
			Name = "btfss";
			Args = new int[] { arg4, arg3 };
			return;
		default:
			break;
		}

		opCode = instruction & 0x3800;

		switch (opCode) {
		case 0x2000:
			Name = "call";
			Args = new int[] { arg1 };
			Cycles = 2;
			return;
		case 0x2800:
			Name = "goto";
			Args = new int[] { arg1 };
			Cycles = 2;
			return;
		default:
			break;
		}
	}

	public String GetName() {
		return Name;
	}

	public int[] GetArgs() {
		return Args;
	}
	
	public int GetCycles() {
		return Cycles;
	}
}
