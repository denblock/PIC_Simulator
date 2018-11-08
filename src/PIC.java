import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;;

public class PIC {
	private int W;
	private int[] Memory;
	private Register Reg;
	private Stack<Integer> Stack;
	private int Prescale;
	private int CyclesLeft;

	public void Simulate(String fileName) throws IOException {
		W = 0xFF;
		Stack = new Stack<Integer>();
		Memory = new int[1024];
		Reg = new Register(1024, this);
		Prescale = Reg.GetPrescale();
		CyclesLeft = Prescale;
		Arrays.fill(Memory, 0xFFFF);

		List<String> data = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);

		for (String line : data) {
			if (line.charAt(0) == ' ') {
				continue;
			}

			String[] byteData = line.split(" ");

			Integer address = Integer.parseInt(byteData[0], 16);
			Integer command = Integer.parseInt(byteData[1], 16);

			Memory[address] = command;
		}

		for (int i = 0; i < Memory.length; i++) {
			if (Memory[i] == 0xFFFF) {
				continue;
			}

			Instruction instruction = new Instruction(Memory[i]);
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
					i += 1;
					instCycles += 1;
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
					i += 1;
					instCycles += 1;
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
				i = instArgs[0] - 1;
				break;
			case "call":
				Stack.push(i + 1);
				i = instArgs[0] - 1;
				break;
			case "return":
				i = Stack.pop() - 1;
				break;
			case "retlw":
				W = instArgs[0];
				i = Stack.pop() - 1;
				break;
			default:
				break;
			}

			if (Reg.GetClockSource() == false) {
				int prescale = Reg.GetPrescale();

				if (Prescale != prescale) {
					Prescale = prescale;
					CyclesLeft = Prescale;
				} else {
					CyclesLeft -= instCycles;

					if (CyclesLeft <= 0) {
						Reg.IncrementTMR0();

						CyclesLeft += Prescale;
					}
				}
				
				
			}

			String output = "[" + i + "] " + instName + "(";

			for (int j = 0; j < instArgs.length; j++) {
				output += Integer.toHexString(instArgs[j]) + ", ";
			}

			output = output.substring(0, output.length() - (instArgs.length > 0 ? 2 : 0));
			output += "):\tW=" + Integer.toHexString(W) + ", FSR=" + Integer.toHexString(Reg.Read(4)) + ", wert1="
					+ Integer.toHexString(Reg.Read(0xC)) + ", wert2=" + Integer.toHexString(Reg.Read(0xD)) + ", ergeb="
					+ Integer.toHexString(Reg.Read(0xE)) + ", DC=" + (Reg.GetDC() ? 1 : 0) + ", C="
					+ (Reg.GetC() ? 1 : 0) + ", Z=" + (Reg.GetZ() ? 1 : 0) + ", CyclesLeft=" + CyclesLeft + ", TMR0="
					+ Reg.GetTMR0();

			System.out.println(output);

		}
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
		CyclesLeft = Prescale;
	}
}