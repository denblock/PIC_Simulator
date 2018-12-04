package simulator;

public class Stack {
	private int[] Stack;
	private int Stack_Pointer;

	public Stack() {
		Stack = new int[8];
		Stack_Pointer = 0;
	}

	public void Push(int val) {
		Stack[Stack_Pointer] = val;
		Stack_Pointer = (Stack_Pointer + 1) % 8;
	}

	public int Pop() {
		if (Stack_Pointer > 0) {
			Stack_Pointer--;
		}
		
		return Stack[Stack_Pointer];
	}
}
