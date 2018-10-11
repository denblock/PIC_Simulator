
public class Instruction {
	private String Name;
	private int[] Args;

	public Instruction(int instruction)
	{
		if (instruction == 0xFFFF) {
			return;
		}

		int opCode = instruction & 0x3F00;
		int arg1 = instruction & 0x07FF; // 00 0kkk kkkk kkkk
		int arg2 = instruction & 0x00FF; // 00 0000 kkkk kkkk
		int arg3 = (instruction & 0x0380) >> 7; // 00 00bb b000 0000
		int arg4 = instruction & 0x007F; // 00 0000 0fff ffff
		int arg5 = (instruction & 0x0080) >> 7; // 00 0000 d000 0000

		switch (opCode) {
		case 0x3000:
		case 0x3100:
		case 0x3200:
		case 0x3300:
			Name = "movlw";
			Args =  new int[] { arg2 };
			break;
		case 0x3E00:
		case 0x3F00:
			Name = "addlw";
			Args =  new int[] { arg2 };
			break;
		case 0x3900:
			Name = "andlw";
			Args =  new int[] { arg2 };
			break;
		case 0x3800:
			Name = "iorlw";
			Args =  new int[] { arg2 };
			break;
		case 0x3A00:
			Name = "xorlw";
			Args =  new int[] { arg2 };
			break;
		case 0x3C00:
		case 0x3D00:
			Name = "sublw";
			Args =  new int[] { arg2 };
			break;
		case 0x0000:
			Name = "movwf";
			Args =  new int[] { arg4 };
			break;
		case 0x0800:
			Name = "movf";
			Args =  new int[] { arg4, arg5 };
			break;
		case 0x0700:
			Name = "addwf";
			Args =  new int[] { arg4, arg5 };
			break;
		case 0x0200:
			Name = "subwf";
			Args =  new int[] { arg4, arg5 };
			break;
		default:
			break;
		}

		opCode = instruction & 0x3C00;

		switch (opCode) {
		case 0x1000:
			Name = "bcf";
			Args =  new int[] { arg4, arg3 };
			break;
		case 0x1400:
			Name = "bsf";
			Args =  new int[] { arg4, arg3 };
			break;
		case 0x1800:
			Name = "btfsc";
			Args =  new int[] { arg4, arg3 };
			break;
		case 0x1C00:
			Name = "btfss";
			Args =  new int[] { arg4, arg3 };
			break;
		default:
			break;
		}

		opCode = instruction & 0x3800;

		switch (opCode) {
		case 0x2000:
			Name = "call";
			Args =  new int[] { arg1 };
			break;
		case 0x2800:
			Name = "goto";
			Args =  new int[] { arg1 };
			break;
		default:
			break;
		}
	}
	
	public String GetName()
	{
		return Name;
	}
	
	public int[] GetArgs()
	{
		return Args;
	}
}
