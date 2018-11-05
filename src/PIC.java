import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;;

public class PIC {
	int W;
	Boolean Z;
	Boolean C;
	Boolean DC;
	int[] Memory;
	int[] Reg;
	Stack<Integer> Stack;

	public void Simulate(String fileName) throws IOException {
		W = 0xFF;
		Z = false;
		C = false;
		DC = false;
		Stack = new Stack<Integer>();
		Memory = new int[1024];
		Reg = new int[1024];
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
			int result;

			switch (instName) {
			case "movlw":
				W = instArgs[0];
				break;
			case "addlw":
				W = ALU(CALC_TYPE.ADD, instArgs[0], W);
				break;
			case "sublw":
				W = ALU(CALC_TYPE.SUBSTRACT, instArgs[0], W);
				break;
			case "andlw":
				W = ALU(CALC_TYPE.AND, instArgs[0], W);
				break;
			case "iorlw":
				W = ALU(CALC_TYPE.IOR, instArgs[0], W);
				break;
			case "xorlw":
				W = ALU(CALC_TYPE.XOR, instArgs[0], W);
				break;
			case "movwf":
				Reg[instArgs[0]] = W;
				break;
			case "addwf":
				result = ALU(CALC_TYPE.ADD, Reg[instArgs[0]], W);

				if (instArgs[1] == 1) {
					Reg[instArgs[0]] = result;
				} else {
					W = result;
				}
				break;
			case "andwf":
				result = ALU(CALC_TYPE.AND, Reg[instArgs[0]], W);

				if (instArgs[1] == 1) {
					Reg[instArgs[0]] = result;
				} else {
					W = result;
				}
				break;
			case "clrf":
				Reg[instArgs[0]] = 0;
				Z = true;
				break;
			case "comf":
				result = ALU(CALC_TYPE.COMPLEMENT, Reg[instArgs[0]]);

				if (instArgs[1] == 1) {
					Reg[instArgs[0]] = result;
				} else {
					W = result;
				}
				break;
			case "decf":
				result = ALU(CALC_TYPE.DECREMENT, Reg[instArgs[0]]);

				if (instArgs[1] == 1) {
					Reg[instArgs[0]] = result;
				} else {
					W = result;
				}
				break;
			case "incf":
				result = ALU(CALC_TYPE.INCREMENT, Reg[instArgs[0]]);

				if (instArgs[1] == 1) {
					Reg[instArgs[0]] = result;
				} else {
					W = result;
				}
				break;
			case "movf":
				result = Reg[instArgs[0]];
				Z = result == 0;

				if (instArgs[1] == 1) {
					Reg[instArgs[0]] = result;
				} else {
					W = result;
				}
				break;
			case "iorwf":
				result = ALU(CALC_TYPE.IOR, Reg[instArgs[0]], W);

				if (instArgs[1] == 1) {
					Reg[instArgs[0]] = result;
				} else {
					W = result;
				}
				break;
			case "subwf":
				result = ALU(CALC_TYPE.SUBSTRACT, Reg[instArgs[0]], W);

				if (instArgs[1] == 1) {
					Reg[instArgs[0]] = result;
				} else {
					W = result;
				}
				break;
			case "swapf":
				result = (Reg[instArgs[0]] & 0x0F) << 4 | (Reg[instArgs[0]] & 0xF0) >> 4;

				if (instArgs[1] == 1) {
					Reg[instArgs[0]] = result;
				} else {
					W = result;
				}
				break;
			case "xorwf":
				result = Reg[instArgs[0]] ^ W;

				if (instArgs[1] == 1) {
					Reg[instArgs[0]] = result;
				} else {
					W = result;
				}
				break;
			case "rlf":
				result = (Reg[instArgs[0]] << 1) & 0xFF;

				if (instArgs[1] == 1) {
					Reg[instArgs[0]] = result;
				} else {
					W = result;
				}
				break;
			case "rrf":
				result = Reg[instArgs[0]] >> 1;

				if (instArgs[1] == 1) {
					Reg[instArgs[0]] = result;
				} else {
					W = result;
				}
				break;
			case "decfsz":
				result = (Reg[instArgs[0]] - 1) & 0xFF;

				if (instArgs[1] == 1) {
					Reg[instArgs[0]] = result;
				} else {
					W = result;
				}

				if (result == 0) {
					i = i + 1;
				}
				break;
			case "incfsz":
				result = (Reg[instArgs[0]] + 1) & 0xFF;

				if (instArgs[1] == 1) {
					Reg[instArgs[0]] = result;
				} else {
					W = result;
				}

				if (result == 0) {
					i = i + 1;
				}
				break;
			case "clrw":
				W = 0;
				break;
			case "goto":
				// i = instArgs[0] - 1;
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
			case "bsf":
				Reg[instArgs[0]] = Reg[instArgs[0]] | (1 << instArgs[1]);
				break;
			case "bcf":
				Reg[instArgs[0]] = Reg[instArgs[0]] & ~(1 << instArgs[1]);
				break;
			case "btfsc":
				if ((Reg[instArgs[0]] & (1 << instArgs[1])) == 0) {
					i = i + 1;
				}
				break;
			case "btfss":
				if ((Reg[instArgs[0]] & (1 << instArgs[1])) != 0) {
					i = i + 1;
				}
				break;
			default:
				break;
			}

			System.out.println(instName + ":\tW=" + Integer.toHexString(W) + ", FSR=" + Integer.toHexString(Reg[4])
					+ ", wert1=" + Integer.toHexString(Reg[0xC]) + ", wert2=" + Integer.toHexString(Reg[0xD])
					+ ", ergeb=" + Integer.toHexString(Reg[0xE]) + ", DC=" + (DC ? 1 : 0) + ", C=" + (C ? 1 : 0)
					+ ", Z=" + (Z ? 1 : 0));

		}
	}

	public int ALU(String instructionName, int a, int b) {
		int result;
		
		switch (instructionName) {
		case "addwf":
		case "addlw":
			result = a + b;
			break;
		case "subwf":
		case "sublw":
			result = a - b;
			break;
		case "andwf":
		case "andlw":
			result = a & b;
			break;
		case "iorwf":
		case "iorlw":
			result = a | b;
			break;
		case "xorwf":
		case "xorlw":
			result = a ^ b;
			break;
		case "comf":
			result = ~a;
			break;
		case "decf":
		case "decfsz":
			result = a - 1;
			break;
		case "incf":
		case "incfsz":
			result = a + 1;
			break;
		case "rlf":
			result = a << 1;
			break;
		case "rrf":
			result = a >>> 1;
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

		Z = result == 0;
		C = result < a;
		DC = (result & 0x0F) < (a & 0x0F);

		return result;
	}

	public int ALU(String instructionName, int a) {
		return ALU(instructionName, a, 0);
	}

}