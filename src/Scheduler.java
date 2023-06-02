import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class Scheduler {
	public final int timeSlice;
	public int counterSlice;
	public Integer[] processId;
	public Integer[] burstTime;
	public Integer[] arrivalTime;
	public Queue<Integer> readyQueue;
	public Queue<Integer> blockedQueue;
	public static int clk;
	public int finished = 0;
	public boolean success=false;

	// the three processes
	public Process p0;
	public Process p1;
	public Process p2;
	
	public boolean p0Inserted = false;
	public boolean p1Inserted = false;
	public boolean p2Inserted = false;

	public Scheduler(int timeSlice, Process p0, Process p1, Process p2) {
		processId = new Integer[3];
		burstTime = new Integer[3];
		arrivalTime = new Integer[3];
		this.timeSlice = timeSlice;
		this.counterSlice = timeSlice;
		this.p0 = p0;
		this.p1 = p1;
		this.p2 = p2;
		this.arrivalTime[0]=p0.arrivalTime;
		this.arrivalTime[1]=p1.arrivalTime;
		this.arrivalTime[2]=p2.arrivalTime;
		this.readyQueue = new LinkedList<Integer>();
		this.blockedQueue = new LinkedList<Integer>();
	}

	public void displayQueue(String s) {
		if (s.equals("ready")) {
			for (Object o : readyQueue) {
				System.out.print(o+", ");
			}
		} else {
			for (Object o : blockedQueue) {
				System.out.print(o+", ");
			}
		}
	}

	public void roundRobin() throws IOException {

		while (finished < 3) {
			System.out.println("--------------------------New Cycle----------------------------------");
			System.out.println("clk=" + clk);
			
			//System.out.println(counterSlice);
			// if process0 arrived --> create it
			if (this.arrivalTime[0] == clk && !p0Inserted) {
				Handler.createProcess(p0);
				readyQueue.add(p0.pcb.getProcess_id());
				p0Inserted=true;
				//displayQueue("ready");
			}
			if (this.arrivalTime[1] == clk && !p1Inserted) {
				Handler.createProcess(p1);
				readyQueue.add(p1.pcb.getProcess_id());
				p1Inserted=true;
				//displayQueue("ready");
			}
			if (this.arrivalTime[2]== clk && !p2Inserted) {
				Handler.createProcess(p2);
				readyQueue.add(p2.pcb.getProcess_id());
				p2Inserted=true;
				//displayQueue("ready");
				
			}
			// check if the time slice is over or not
			if (counterSlice == 0) {
				// my time slice is over (put me at the end of the queue
				if(p0.pcb!=null && readyQueue.peek()==p0.pcb.getProcess_id()) {
					p0.pcb.setState(State.READY);
					updateMyState(p0,State.READY);
				}
				if(p1.pcb!=null && readyQueue.peek()==p1.pcb.getProcess_id()) {
					p1.pcb.setState(State.READY);
					updateMyState(p1,State.READY);
				}
				if(p2.pcb!=null && readyQueue.peek()==p2.pcb.getProcess_id()) {
					p2.pcb.setState(State.READY);
					updateMyState(p2,State.READY);
				}
				readyQueue.add(readyQueue.remove());
				counterSlice = timeSlice;
			}
			System.out.print("Ready queue: ");
			displayQueue("ready");
			System.out.println();
			System.out.print("Blocked queue: ");
			displayQueue("blocked");
			System.out.println();
			//if (clk==11) break;
			if (p0.pcb!=null && readyQueue.peek()==p0.pcb.getProcess_id()) {
				System.out.println("The process is: p0");
				p0.pcb.setState(State.RUNNING);
				updateMyState(p0,State.RUNNING);
				// check if it is in memory or not
				if (!(p0.pcb.getProcess_id() == (int) Handler.memory[1]
						|| p0.pcb.getProcess_id() == (int) Handler.memory[21])) {
					// not found in memory and i put it in memory
					System.out.println("not in memory");
					Handler.swapMemoryAndDisk();
					if(!blockedQueue.contains(p0.pcb.getProcess_id())) {
						if (p0.pcb.getProcess_id() == (int) Handler.memory[1]) {
							Handler.memory[0]= State.READY;
						}
						else if (p0.pcb.getProcess_id() == (int) Handler.memory[21]) {
							Handler.memory[20]= State.READY;
						}
					}
				}
				// it is in the first place in memory
				if (p0.pcb.getProcess_id() == (int) Handler.memory[1]) {
					// if the instruction is executed successfully 
					if (Handler.execute((String) Handler.memory[8 + p0.pcb.getPc()], p0)) {
						System.out.println("The instruction is: "+ Handler.memory[8 + p0.pcb.getPc()]);
						// 3 cases to increment pc
						// contains inout and iput increment flag is true
						if (((String) Handler.memory[8 + p0.pcb.getPc()]).contains("input") && Handler.userInputIncPcFlag) {
							p0.pcb.setPc(p0.pcb.getPc() + 1);
							Handler.memory[2]= p0.pcb.getPc();
						}
						// contains readfile and redfile increment flag is true
						else if	(((String) Handler.memory[8 + p0.pcb.getPc()]).contains("readFile") && Handler.readFileIncPcFlag) {
							p0.pcb.setPc(p0.pcb.getPc() + 1);
							Handler.memory[2]= p0.pcb.getPc();
						// does not contain neither input or readfile
						} else if (!((String) Handler.memory[8 + p0.pcb.getPc()]).contains("input") && !((String) Handler.memory[8 + p0.pcb.getPc()]).contains("readFile")) {
							p0.pcb.setPc(p0.pcb.getPc() + 1);
							Handler.memory[2]= p0.pcb.getPc();
						}
						this.burstTime[0]--;
						success=true;
						if(this.burstTime[0]==0) {
							p0.pcb.setState(State.FINISHED);
							readyQueue.remove(p0.pcb.getProcess_id());
							Handler.memory[0]= "FINISHED";
							finished++;
							counterSlice=timeSlice+1;
						}
						// TODO : METHOD TO UPDATE MEMORY WITH THE NEW VALUES OF THE PROCESS 
					} else {
						System.out.println("hena ");
						blockedQueue.add(p0.pcb.getProcess_id());
						readyQueue.remove(p0.pcb.getProcess_id());
						Handler.memory[0]= "BLOCKED";
						p0.pcb.setState(State.BLOCKED);
					}
				}
				else if (p0.pcb.getProcess_id() == (int) Handler.memory[21]) {
					// if the instruction is executed successfully 
					if (Handler.execute((String) Handler.memory[28 + p0.pcb.getPc()], p0)) {
						System.out.println("The instruction is: "+ Handler.memory[28 + p0.pcb.getPc()]);
						// 3 cases to increment pc
						// contains inout and iput increment flag is true
						if (((String) Handler.memory[28 + p0.pcb.getPc()]).contains("input") && Handler.userInputIncPcFlag) {
							p0.pcb.setPc(p0.pcb.getPc() + 1);
							Handler.memory[22]= p0.pcb.getPc();
						}
						// contains readfile and redfile increment flag is true
						else if	(((String) Handler.memory[28 + p0.pcb.getPc()]).contains("readFile") && Handler.readFileIncPcFlag) {
							p0.pcb.setPc(p0.pcb.getPc() + 1);
							Handler.memory[22]= p0.pcb.getPc();
						// does not contain neither input or readfile
						} else if (!((String) Handler.memory[28 + p0.pcb.getPc()]).contains("input") && !((String) Handler.memory[28 + p0.pcb.getPc()]).contains("readFile")) {
							p0.pcb.setPc(p0.pcb.getPc() + 1);
							Handler.memory[22]= p0.pcb.getPc();
						}
						this.burstTime[0]--;
						success=true;
						if(this.burstTime[0]==0) {
							p0.pcb.setState(State.FINISHED);
							readyQueue.remove(p0.pcb.getProcess_id());
							Handler.memory[20]= "FINISHED";
							//Handler.memory[22]= p0.pcb.getPc();
							finished++;
							counterSlice=timeSlice+1;
						}
						// TODO : METHOD TO UPDATE MEMORY WITH THE NEW VALUES OF THE PROCESS 
					} else {
						blockedQueue.add(p0.pcb.getProcess_id());
						readyQueue.remove(p0.pcb.getProcess_id());
						Handler.memory[20]= "BLOCKED";
						p0.pcb.setState(State.BLOCKED);
					}
				}
				

			}
			// it is process1 in hand
			else if (p1.pcb!=null && readyQueue.peek()==p1.pcb.getProcess_id()) {
				p1.pcb.setState(State.RUNNING);
				updateMyState(p1,State.RUNNING);
				System.out.println("The process is: p1");
				// check if it is in memory or not
				if (!(p1.pcb.getProcess_id() == (int) Handler.memory[1]
						|| p1.pcb.getProcess_id() == (int) Handler.memory[21])) {
					// not found in memory and i put it in memory
					Handler.swapMemoryAndDisk();
					if(!blockedQueue.contains(p1.pcb.getProcess_id())) {
						if (p1.pcb.getProcess_id() == (int) Handler.memory[1]) {
							Handler.memory[0]= State.READY;
						}
						else if (p1.pcb.getProcess_id() == (int) Handler.memory[21]) {
							Handler.memory[20]= State.READY;
						}
					}
				}
				// it is in the first place in memory
				if (p1.pcb.getProcess_id() == (int) Handler.memory[1]) {
					// if the instruction is executed successfully 
					if (Handler.execute((String) Handler.memory[8 + p1.pcb.getPc()], p1)) {
						System.out.println("The instruction is: "+ Handler.memory[8 + p1.pcb.getPc()]);
						// 3 cases to increment pc
						// contains inout and iput increment flag is true
						if (((String) Handler.memory[8 + p1.pcb.getPc()]).contains("input") && Handler.userInputIncPcFlag) {
							p1.pcb.setPc(p1.pcb.getPc() + 1);
							Handler.memory[2]= p1.pcb.getPc();
						}
						// contains readfile and redfile increment flag is true
						else if	(((String) Handler.memory[8 + p1.pcb.getPc()]).contains("readFile") && Handler.readFileIncPcFlag) {
							p1.pcb.setPc(p1.pcb.getPc() + 1);
							Handler.memory[2]= p1.pcb.getPc();
						// does not contain neither input or readfile
						} else if (!((String) Handler.memory[8 + p1.pcb.getPc()]).contains("input") && !((String) Handler.memory[8 + p1.pcb.getPc()]).contains("readFile")) {
							p1.pcb.setPc(p1.pcb.getPc() + 1);
							Handler.memory[2]= p1.pcb.getPc();
						}
						this.burstTime[1]--;
						System.out.println("mybursttime="+this.burstTime[1]);
						success=true;
						if(this.burstTime[1]==0) {
							p1.pcb.setState(State.FINISHED);
							readyQueue.remove(p1.pcb.getProcess_id());
							Handler.memory[0]= "FINISHED";
							//Handler.memory[2]= p1.pcb.getPc();
							finished++;
							counterSlice=timeSlice+1;
						}
						// TODO : METHOD TO UPDATE MEMORY WITH THE NEW VALUES OF THE PROCESS 
					} else {
						blockedQueue.add(p1.pcb.getProcess_id());
						readyQueue.remove(p1.pcb.getProcess_id());
						Handler.memory[0]= "BLOCKED";
						p1.pcb.setState(State.BLOCKED);
					}
				}
				else if (p1.pcb.getProcess_id() == (int) Handler.memory[21]) {
					// if the instruction is executed successfully 
					if (Handler.execute((String) Handler.memory[28 + p1.pcb.getPc()], p1)) {
						System.out.println("The instruction is: "+ Handler.memory[28 + p1.pcb.getPc()]);
						// 3 cases to increment pc
						// contains inout and iput increment flag is true
						if (((String) Handler.memory[28 + p1.pcb.getPc()]).contains("input") && Handler.userInputIncPcFlag) {
							p1.pcb.setPc(p1.pcb.getPc() + 1);
							Handler.memory[22]= p1.pcb.getPc();
						}
						// contains readfile and redfile increment flag is true
						else if	(((String) Handler.memory[28 + p1.pcb.getPc()]).contains("readFile") && Handler.readFileIncPcFlag) {
							p1.pcb.setPc(p1.pcb.getPc() + 1);
							Handler.memory[22]= p1.pcb.getPc();
						// does not contain neither input or readfile
						} else if (!((String) Handler.memory[28 + p1.pcb.getPc()]).contains("input") && !((String) Handler.memory[28 + p1.pcb.getPc()]).contains("readFile")) {
							p1.pcb.setPc(p1.pcb.getPc() + 1);
							Handler.memory[22]= p1.pcb.getPc();
						}
						this.burstTime[1]--;
						success=true;
						if(this.burstTime[1]==0) {
							p1.pcb.setState(State.FINISHED);
							readyQueue.remove(p1.pcb.getProcess_id());
							Handler.memory[20]= "FINISHED";
							//Handler.memory[22]= p1.pcb.getPc();
							finished++;
							counterSlice=timeSlice+1;
						}
						// TODO : METHOD TO UPDATE MEMORY WITH THE NEW VALUES OF THE PROCESS 
					} else {
						System.out.println("I was trying to: "+ Handler.memory[28 + p1.pcb.getPc()]);
						blockedQueue.add(p1.pcb.getProcess_id());
						readyQueue.remove(p1.pcb.getProcess_id());
						Handler.memory[20]= "BLOCKED";
						p1.pcb.setState(State.BLOCKED);
					}
				}
				

			}
			
			// if process2 is in hand
			else if (p2.pcb!=null && readyQueue.peek()==p2.pcb.getProcess_id()) {
				p2.pcb.setState(State.RUNNING);
				updateMyState(p2,State.RUNNING);
				System.out.println("The process is: p2");
				// check if it is in memory or not
				if (!(p2.pcb.getProcess_id() == (int) Handler.memory[1]
						|| p2.pcb.getProcess_id() == (int) Handler.memory[21])) {
					// not found in memory and i put it in memory
					Handler.swapMemoryAndDisk();
					if(!blockedQueue.contains(p2.pcb.getProcess_id())) {
						if (p2.pcb.getProcess_id() == (int) Handler.memory[1]) {
							Handler.memory[0]= State.READY;
						}
						else if (p2.pcb.getProcess_id() == (int) Handler.memory[21]) {
							Handler.memory[20]= State.READY;
						}
					}
				}
				// it is in the first place in memory
				if (p2.pcb.getProcess_id() == (int) Handler.memory[1]) {
					// if the instruction is executed successfully 
					if (Handler.execute((String) Handler.memory[8 + p2.pcb.getPc()], p2)) {
						System.out.println("The instruction is: "+ Handler.memory[8 + p2.pcb.getPc()]);
						// 3 cases to increment pc
						// contains inout and iput increment flag is true
						if (((String) Handler.memory[8 + p2.pcb.getPc()]).contains("input") && Handler.userInputIncPcFlag) {
							p2.pcb.setPc(p2.pcb.getPc() + 1);
							Handler.memory[2]= p2.pcb.getPc();
						}
						// contains readfile and redfile increment flag is true
						else if	(((String) Handler.memory[8 + p2.pcb.getPc()]).contains("readFile") && Handler.readFileIncPcFlag) {
							p2.pcb.setPc(p2.pcb.getPc() + 1);
							Handler.memory[2]= p2.pcb.getPc();
						// does not contain neither input or readfile
						} else if (!((String) Handler.memory[8 + p2.pcb.getPc()]).contains("input") && !((String) Handler.memory[8 + p2.pcb.getPc()]).contains("readFile")) {
							p2.pcb.setPc(p2.pcb.getPc() + 1);
							Handler.memory[2]= p2.pcb.getPc();
						}
						this.burstTime[2]--;
						success=true;
						if(this.burstTime[2]==0) {
							p2.pcb.setState(State.FINISHED);
							readyQueue.remove(p2.pcb.getProcess_id());
							Handler.memory[0]= "FINISHED";
							//Handler.memory[2]= p2.pcb.getPc();
							finished++;
							counterSlice=timeSlice+1;
						}
						// TODO : METHOD TO UPDATE MEMORY WITH THE NEW VALUES OF THE PROCESS 
					} else {
						blockedQueue.add(p2.pcb.getProcess_id());
						readyQueue.remove(p2.pcb.getProcess_id());
						Handler.memory[0]= "BLOCKED";
						p2.pcb.setState(State.BLOCKED);
					}
				}
				else if (p2.pcb.getProcess_id() == (int) Handler.memory[21]) {
					// if the instruction is executed successfully 
					if (Handler.execute((String) Handler.memory[28 + p2.pcb.getPc()], p2)) {
						System.out.println("The instruction is: "+ Handler.memory[28 + p2.pcb.getPc()]);
						// 3 cases to increment pc
						// contains inout and iput increment flag is true
						if (((String) Handler.memory[28 + p2.pcb.getPc()]).contains("input") && Handler.userInputIncPcFlag) {
							p2.pcb.setPc(p2.pcb.getPc() + 1);
							Handler.memory[22]= p0.pcb.getPc();
						}
						// contains readfile and redfile increment flag is true
						else if	(((String) Handler.memory[28 + p2.pcb.getPc()]).contains("readFile") && Handler.readFileIncPcFlag) {
							p2.pcb.setPc(p2.pcb.getPc() + 1);
							Handler.memory[22]= p0.pcb.getPc();
						// does not contain neither input or readfile
						} else if (!((String) Handler.memory[28 + p2.pcb.getPc()]).contains("input") && !((String) Handler.memory[28 + p2.pcb.getPc()]).contains("readFile")) {
							p2.pcb.setPc(p2.pcb.getPc() + 1);
							Handler.memory[22]= p0.pcb.getPc();
						}
						this.burstTime[2]--;
						success=true;
						if(this.burstTime[2]==0) {
							p2.pcb.setState(State.FINISHED);
							readyQueue.remove(p2.pcb.getProcess_id());
							Handler.memory[20]= "FINISHED";
							//Handler.memory[22]= p2.pcb.getPc();
							finished++;
							counterSlice=timeSlice+1;
						}
						// TODO : METHOD TO UPDATE MEMORY WITH THE NEW VALUES OF THE PROCESS 
					} else {
						blockedQueue.add(p2.pcb.getProcess_id());
						readyQueue.remove(p2.pcb.getProcess_id());
						Handler.memory[20]= "BLOCKED";
						p2.pcb.setState(State.BLOCKED);
					}
				}
				

			}
			System.out.println("______________________________________");
			System.out.println("MEMORY");
			for (Object o : Handler.memory) {
				System.out.println(o);
			}
			System.out.println("______________________________________");
			if (success) {
				clk++;
				counterSlice--;
				System.out.println(success+" "+counterSlice+"");
			}
				System.out.print("Ready queue: ");
				displayQueue("ready");
				System.out.println();
				System.out.print("Blocked queue: ");
				displayQueue("blocked");
				System.out.println();
			success=false;
			
		}
	}
	
	public static void updateMyState(Process p, State s) {
		if (p.pcb.getProcess_id() == (int) Handler.memory[1]) {
			Handler.memory[0]= s;
		}
		else if (p.pcb.getProcess_id() == (int) Handler.memory[21]) {
			Handler.memory[20]= s;
		}
	}

}
