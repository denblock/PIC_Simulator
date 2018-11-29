package simulator;

import java.util.Arrays;
import java.util.Stack;

public class PIC {
	private int W;
	private int[] Memory;
	private Register Reg;
	private Stack<Integer> Stack;
	private int Runtime;
	private int CyclesLeft;
	private int PC;
	private int Ende;

	private int[] LST_Offset;

	public PIC() {
		Reset();
	}

	public void Reset() {
		W = 0xFF;
		Stack = new Stack<Integer>();
		Reg = new Register(1024, this);
		Runtime = 0;
		CyclesLeft = Reg.GetPrescale();
		PC = 0;
	}

	public void ParseLST(String lst) {
		LST_Offset = new int[1024];
		Memory = new int[1024];
		Arrays.fill(Memory, 0xFFFF);
		PC = 0;
		Ende = 0;

		String[] data = lst.split(System.lineSeparator());

		for (int i = 0; i < data.length; i++) {
			if (data[i].charAt(0) == ' ') {
				continue;
			}
			
			

			String[] byteData = data[i].split(" ");

			Integer address = Integer.parseInt(byteData[0], 16);
			Integer command = Integer.parseInt(byteData[1], 16);

			Memory[address] = command;
			Ende = address;

			LST_Offset[address] = i;
		}
	}

	public void Run() {
		int run = 0;

		while (run == 0) {
			run = Step();
		}
	}

	public int Step() {
		if (PC >= Ende) {
			return -1;
		}

		do {
			Instruction instruction;

			if (!Reg.GetPD() || Reg.GetTO()) {
				instruction = new Instruction(Memory[PC]);

				PC++;
			} else {
				instruction = new Instruction(0);
			}

			String instName = instruction.GetName();
			int[] instArgs = instruction.GetArgs();
			int instCycles = instruction.GetCycles();

			switch (instName) {
			case "addlw":
			case "andlw":
			case "iorlw":
			case "movlw":
			case "sublw":
			case "xorlw":
				W = Calculate(instName, instArgs[0]);
				break;
			case "addwf":
			case "andwf":
			case "comf":
			case "decf":
			case "decfsz":
			case "incf":
			case "incfsz":
			case "iorwf":
			case "movf":
			case "rlf":
			case "rrf":
			case "subwf":
			case "swapf":
			case "xorwf":
				int result = Calculate(instName, Reg.Read(instArgs[0]));

				if (instArgs[1] == 1) {
					Reg.Write(instArgs[0], result);
				} else {
					W = result;
				}

				if ((instName == "decfsz" || instName == "incfsz") && result == 0) {
					instCycles++;
					PC++;
				}
				break;
			case "bcf":
			case "bsf":
				Reg.Write(instArgs[0], Calculate(instName, Reg.Read(instArgs[0]), instArgs[1]));
				break;
			case "btfsc":
			case "btfss":
				int bit = Reg.Read(instArgs[0]) & (1 << instArgs[1]);

				if ((instName == "btfsc" && bit == 0) || (instName == "btfss" && bit != 0)) {
					instCycles++;
					PC++;
				}
				break;
			case "clrw":
				W = 0;
				Reg.SetZ(true);
				break;
			case "clrf":
				Reg.Write(instArgs[0], 0);
				Reg.SetZ(true);
				break;
			case "movwf":
				Reg.Write(instArgs[0], W);
				break;
			case "goto":
				PC = instArgs[0];
				break;
			case "call":
				Stack.push(PC);
				PC = instArgs[0];
				break;
			case "return":
				PC = Stack.pop();
				break;
			case "retlw":
				W = instArgs[0];
				PC = Stack.pop();
				break;
			case "retfie":
				Reg.SetGIE(true);
				PC = Stack.pop();
				break;
			case "sleep":
				Reg.SetPD(true);
				Reg.SetTO(false);
				ClearWDT();
				break;
			case "clrwdt":
				Reg.SetPD(false);
				Reg.SetTO(false);
				ClearWDT();
				break;
			default:
				break;
			}

			if (!Reg.GetClockSource()) {
				CyclesLeft -= instCycles;
			}

			if (CyclesLeft <= 0) {
				Reg.IncrementTMR0();

				CyclesLeft += Reg.GetPrescale();
			}

			Runtime += instCycles;
			int time = 18 * 1000 * (Reg.GetPSA() ? Reg.GetPrescale() : 1);

			if (Runtime >= time) {
				if (!Reg.GetTO()) {
					Reg.SetTO(true);
				} else {
					Reg.Reset();
				}

				Runtime -= time;
			}

		} while (Reg.GetPD() && !Reg.GetTO());

		return 0;
	}

	public int Calculate(String instructionName, int a, int b) {
		int result;
		int[] statusAffected = new int[] { 0, 0, 0 }; // { C, DC, Z }

		switch (instructionName) {
		case "movlw":
		case "movf":
			result = a;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "addwf":
		case "addlw":
			result = a + b;
			statusAffected = new int[] { 1, 1, 1 };
			break;
		case "subwf":
		case "sublw":
			result = a - b;
			statusAffected = new int[] { 1, 1, 1 };
			break;
		case "andwf":
		case "andlw":
			result = a & b;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "iorwf":
		case "iorlw":
			result = a | b;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "xorwf":
		case "xorlw":
			result = a ^ b;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "comf":
			result = ~a;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "decf":
			result = a - 1;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "decfsz":
			result = a - 1;
			break;
		case "incf":
			result = a + 1;
			statusAffected = new int[] { 0, 0, 1 };
			break;
		case "incfsz":
			result = a + 1;
			break;
		case "rlf":
			result = (a << 1) | (Reg.GetC() ? 1 : 0);
			Reg.SetC(((a & 0x80) >> 7) == 1);
			break;
		case "rrf":
			result = (a >> 1) | ((Reg.GetC() ? 1 : 0) << 7);
			Reg.SetC((a & 0x01) == 1);
			break;
		case "bcf":
			result = a & ~(1 << b);
			break;
		case "bsf":
			result = a | (1 << b);
			break;
		case "swapf":
			result = (a & 0x0F) << 4 | (a & 0xF0) >> 4;
			break;
		default:
			result = 0;
		}

		result = result & 0xFF;

		if (statusAffected[0] == 1) {
			Reg.SetC(result < a);
		}

		if (statusAffected[1] == 1) {
			Reg.SetDC((result & 0x0F) < (a & 0x0F));
		}

		if (statusAffected[2] == 1) {
			Reg.SetZ(result == 0);
		}

		return result;
	}

	public int Calculate(String instructionName, int a) {
		return Calculate(instructionName, a, W);
	}

	public void ResetCyclesLeft() {
		CyclesLeft = Reg.GetPrescale();
	}

	public void ClearWDT() {
		Runtime = 0;

		if (Reg.GetPSA()) {
			Reg.ClearPrescale();
		}
	}

	public void RA4_Invoked(boolean rising) {
		if (Reg.GetINTEDG() == rising) {
			CyclesLeft--;
		}
	}
	
	public void RB0_Invoked(boolean rising) {
		if(Reg.GetGIE() && Reg.GetINTE() && Reg.GetINTEDG() == rising) {
			Reg.SetINTF(true);
			Interrupt();
		}
	}
	
	public void RB_Changed(int port) {
		if(Reg.GetGIE() && Reg.GetRBIE() && Reg.GetTRISB(port)) {
			Reg.SetRBIF(true);
			Interrupt();
		}
	}

	public void Interrupt() {
		Reg.SetGIE(false);
		Stack.push(PC);
		PC = 4;
	}

	public Register GetRegister() {
		return Reg;
	}

	public int GetW() {
		return W;
	}

	public int GetPC() {
		return PC;
	}

	public int GetLSTOffset(int idx) {
		return LST_Offset[idx];
	}
}